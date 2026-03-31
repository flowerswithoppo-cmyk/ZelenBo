package com.zelenbo.app.service

import com.zelenbo.app.domain.model.BestEffortConfig
import com.zelenbo.app.domain.model.DnsConfig
import com.zelenbo.app.domain.model.LogEntry
import com.zelenbo.app.domain.model.LogLevel
import com.zelenbo.app.domain.repository.LogsRepository
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrafficManager @Inject constructor(
    private val dnsResolver: DnsResolver,
    private val logsRepository: LogsRepository,
    private val proxyServer: ProxyServer
) {

    data class RuntimeConfig(
        val vpnDnsIp: String,
        val dnsConfig: DnsConfig,
        val bestEffort: BestEffortConfig,
        val optimizedDomains: Set<String>
    )

    private var scope: CoroutineScope? = null
    private var dnsJob: Job? = null

    private val idGen = AtomicLong(0)

    fun start(
        vpnTunFd: FileDescriptor,
        runtimeConfig: RuntimeConfig
    ) {
        if (scope != null) return

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope

        // Start SOCKS5 local proxy on 127.0.0.1:1080.
        // In safe MVP it forwards traffic directly.
        proxyServer.start(scope = newScope, bindAddress = "127.0.0.1", port = 1080)

        dnsJob = newScope.launch {
            runDnsLoop(vpnTunFd, runtimeConfig)
        }
    }

    suspend fun stop() {
        val s = scope ?: return
        dnsJob?.cancelAndJoin()
        dnsJob = null
        s.cancel()
        scope = null
    }

    private suspend fun runDnsLoop(
        tunFd: FileDescriptor,
        runtimeConfig: RuntimeConfig
    ) {
        val input = FileInputStream(tunFd)
        val output = FileOutputStream(tunFd)
        val buffer = ByteArray(32767)

        // VPN DNS server is inside the VPN address range; we only answer UDP:53.
        val vpnDnsBytes = ipv4ToBytes(runtimeConfig.vpnDnsIp)

        log(LogLevel.Info, "DNS loop started")
        var handled = 0L
        while (scope?.isActive == true) {
            val len = try {
                input.read(buffer)
            } catch (_: Throwable) {
                break
            }
            if (len <= 0) continue

            val packet = buffer.copyOf(len)
            val parsed = parseIpv4UdpDnsQuery(packet, vpnDnsBytes) ?: continue

            val qName = parsed.queryName ?: "unknown"
            val qBytes = parsed.dnsPayload

            val resolvedResponse = try {
                dnsResolver.resolveOptimized(
                    query = qBytes,
                    qName = qName,
                    dns = runtimeConfig.dnsConfig,
                    bestEffort = runtimeConfig.bestEffort,
                    optimizedDomains = runtimeConfig.optimizedDomains
                )
            } catch (t: Throwable) {
                log(LogLevel.Error, "DNS resolve failed for $qName: ${t.message}")
                // If upstream fails, just drop packet.
                continue
            }

            val responsePacket = buildIpv4UdpDnsResponse(
                request = packet,
                udpPayload = resolvedResponse,
                vpnDnsBytes = vpnDnsBytes
            ) ?: continue

            try {
                output.write(responsePacket)
                output.flush()
                handled++
            } catch (_: Throwable) {
                break
            }
        }

        log(LogLevel.Info, "DNS loop stopped, handled=$handled")
    }

    private fun parseIpv4UdpDnsQuery(packet: ByteArray, vpnDnsBytes: ByteArray): ParsedDnsQuery? {
        if (packet.size < 28) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihl = packet[0].toInt() and 0x0F
        if (ihl != 5) return null // MVP: only IPv4 without options
        val ipHeaderLen = ihl * 4
        if (packet.size < ipHeaderLen + 8) return null

        val totalLen = u16(packet, 2)
        if (totalLen < ipHeaderLen + 8) return null

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // UDP

        val dstIp = packet.copyOfRange(16, 20)
        if (!dstIp.contentEquals(vpnDnsBytes)) return null

        val udpStart = ipHeaderLen
        val srcPort = u16(packet, udpStart)
        val dstPort = u16(packet, udpStart + 2)
        if (dstPort != 53) return null

        val udpLen = u16(packet, udpStart + 4)
        if (udpLen < 8) return null
        val dnsStart = udpStart + 8
        val dnsLen = udpLen - 8
        if (dnsStart + dnsLen > packet.size) return null
        val dnsPayload = packet.copyOfRange(dnsStart, dnsStart + dnsLen)

        val queryName = dnsResolver.extractFirstQuestionName(dnsPayload)?.lowercase()

        return ParsedDnsQuery(
            clientPort = srcPort,
            dnsPayload = dnsPayload,
            queryName = queryName
        )
    }

    private data class ParsedDnsQuery(
        val clientPort: Int,
        val dnsPayload: ByteArray,
        val queryName: String?
    )

    private fun buildIpv4UdpDnsResponse(
        request: ByteArray,
        udpPayload: ByteArray,
        vpnDnsBytes: ByteArray
    ): ByteArray? {
        // Swap src/dst: response should come from vpnDnsIp:53 to clientIp:clientPort
        if (request.size < 28) return null

        val versionAndIhl = request[0]
        val ihl = versionAndIhl.toInt() and 0x0F
        if (ihl != 5) return null // MVP: only IPv4 without options
        val ipHeaderLen = ihl * 4
        if (ipHeaderLen < 20) return null

        val srcIp = request.copyOfRange(12, 16)
        val dstIp = request.copyOfRange(16, 20)

        val udpStart = ipHeaderLen
        val clientPort = u16(request, udpStart) // request src port

        // IP header length fixed 20 (no options) in response
        val dnsLen = udpPayload.size
        val udpLen = 8 + dnsLen
        val totalLen = ipHeaderLen + udpLen
        val response = ByteArray(totalLen)

        // IPv4 header
        response[0] = (4 shl 4 or ihl).toByte()
        // TOS
        response[1] = 0
        // Total length
        putU16(response, 2, totalLen)
        // ID
        putU16(response, 4, u16(request, 4))
        // Flags/fragment offset
        response[6] = request[6]
        response[7] = request[7]
        // TTL
        response[8] = 64.toByte()
        // Protocol UDP
        response[9] = 17.toByte()
        // Checksum placeholder
        response[10] = 0
        response[11] = 0

        // Src IP = vpnDnsIp
        System.arraycopy(vpnDnsBytes, 0, response, 12, 4)
        // Dst IP = request src IP
        System.arraycopy(srcIp, 0, response, 16, 4)

        val ipChecksum = ipv4HeaderChecksum(response, 0, ipHeaderLen)
        putU16(response, 10, ipChecksum)

        // UDP header
        val udpOutStart = ipHeaderLen
        // Src port 53
        putU16(response, udpOutStart, 53)
        // Dst port = clientPort from request
        putU16(response, udpOutStart + 2, clientPort)
        // UDP length
        putU16(response, udpOutStart + 4, udpLen)
        // UDP checksum (optional in IPv4 but we compute)
        putU16(response, udpOutStart + 6, 0)
        System.arraycopy(udpPayload, 0, response, udpOutStart + 8, dnsLen)

        val udpChecksum = udpChecksum(response, vpnDnsBytes, srcIp, udpOutStart, udpPayload)
        putU16(response, udpOutStart + 6, udpChecksum)

        return response
    }

    private fun udpChecksum(
        packet: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpHeaderStart: Int,
        payload: ByteArray
    ): Int {
        // Pseudo header: src, dst, zero, protocol, udpLen
        val udpLen = 8 + payload.size
        val pseudo = ByteArray(12 + udpLen)
        // src
        System.arraycopy(srcIp, 0, pseudo, 0, 4)
        // dst
        System.arraycopy(dstIp, 0, pseudo, 4, 4)
        // zero
        pseudo[8] = 0
        // protocol UDP
        pseudo[9] = 17.toByte()
        // udp length
        pseudo[10] = ((udpLen ushr 8) and 0xFF).toByte()
        pseudo[11] = (udpLen and 0xFF).toByte()

        // UDP header
        // srcPort
        val srcPort = u16(packet, udpHeaderStart)
        pseudo[12] = ((srcPort ushr 8) and 0xFF).toByte()
        pseudo[13] = (srcPort and 0xFF).toByte()
        // dstPort
        val dstPort = u16(packet, udpHeaderStart + 2)
        pseudo[14] = ((dstPort ushr 8) and 0xFF).toByte()
        pseudo[15] = (dstPort and 0xFF).toByte()
        // udp length
        pseudo[16] = ((udpLen ushr 8) and 0xFF).toByte()
        pseudo[17] = (udpLen and 0xFF).toByte()
        // checksum = 0 for calculation
        pseudo[18] = 0
        pseudo[19] = 0

        System.arraycopy(payload, 0, pseudo, 20, payload.size)

        val sum = onesComplementSum(pseudo)
        val result = (~sum) and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }

    private fun ipv4HeaderChecksum(packet: ByteArray, start: Int, len: Int): Int {
        var sum = 0
        var i = start
        while (i < start + len) {
            val word = ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (~sum) and 0xFFFF
    }

    private fun onesComplementSum(buf: ByteArray): Int {
        var sum = 0
        var i = 0
        while (i + 1 < buf.size) {
            val word = ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < buf.size) {
            sum += (buf[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum
    }

    private fun ipv4ToBytes(ip: String): ByteArray {
        val parts = ip.split(".")
        require(parts.size == 4)
        return byteArrayOf(
            parts[0].toInt().toByte(),
            parts[1].toInt().toByte(),
            parts[2].toInt().toByte(),
            parts[3].toInt().toByte()
        )
    }

    private fun u16(b: ByteArray, offset: Int): Int {
        return ((b[offset].toInt() and 0xFF) shl 8) or (b[offset + 1].toInt() and 0xFF)
    }

    private fun putU16(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = ((value ushr 8) and 0xFF).toByte()
        buf[offset + 1] = (value and 0xFF).toByte()
    }

    private suspend fun log(level: LogLevel, message: String) {
        val entry = LogEntry(
            id = idGen.incrementAndGet(),
            timestampMillis = System.currentTimeMillis(),
            level = level,
            tag = "vpn",
            message = message
        )
        logsRepository.append(entry)
    }
}

