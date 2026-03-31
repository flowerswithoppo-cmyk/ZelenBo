package com.zelenbo.app.service

import com.zelenbo.app.domain.model.BestEffortConfig
import com.zelenbo.app.domain.model.DnsConfig
import com.zelenbo.app.domain.model.DnsPreset
import com.zelenbo.app.domain.model.DnsTransportMode
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLSocket

class DnsResolver @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sslSocketFactory: SSLSocketFactory = (SSLSocketFactory.getDefault() ?: javax.net.ssl.SSLSocketFactory.getDefault())
) {

    private val dnsMessageMediaType = "application/dns-message".toMediaType()

    fun extractFirstQuestionName(query: ByteArray): String? {
        // DNS header is 12 bytes: id, flags, qdcount, ancount, nscount, arcount
        if (query.size < 12) return null
        val qdCount = u16(query, 4)
        if (qdCount <= 0) return null
        val offset = 12
        val decoded = decodeDomainName(query, offset) ?: return null
        return decoded.first
    }

    /**
     * @return Pair(domain, nextOffset)
     */
    private fun decodeDomainName(query: ByteArray, startOffset: Int): Pair<String, Int>? {
        var offset = startOffset
        val labels = ArrayList<String>(8)
        var nextOffset = -1
        var jumped = false
        var safety = 0

        while (safety++ < 255) {
            if (offset >= query.size) return null
            val len = query[offset].toInt() and 0xFF
            if (len == 0) {
                if (!jumped) nextOffset = offset + 1
                return labels.joinToString(".") to nextOffset
            }
            val labelType = len and 0xC0
            if (labelType == 0xC0) {
                // compression pointer
                if (offset + 1 >= query.size) return null
                val pointer = ((len and 0x3F) shl 8) or (query[offset + 1].toInt() and 0xFF)
                if (!jumped) {
                    nextOffset = offset + 2
                }
                offset = pointer
                jumped = true
            } else {
                val realLen = len
                val labelEnd = offset + 1 + realLen
                if (labelEnd > query.size) return null
                val labelBytes = query.copyOfRange(offset + 1, labelEnd)
                val label = labelBytes.toString(Charsets.UTF_8)
                labels.add(label)
                offset = labelEnd
            }
        }
        return null
    }

    private fun u16(b: ByteArray, offset: Int): Int {
        if (offset + 2 > b.size) return 0
        return ((b[offset].toInt() and 0xFF) shl 8) or (b[offset + 1].toInt() and 0xFF)
    }

    suspend fun resolveOptimized(
        query: ByteArray,
        qName: String,
        dns: DnsConfig,
        bestEffort: BestEffortConfig,
        optimizedDomains: Set<String>
    ): ByteArray {
        // Optimized decision by qName
        val domain = qName.lowercase()
        val isOptimized = optimizedDomains.any { domain == it.lowercase() || domain.endsWith("." + it.lowercase()) }
        return if (isOptimized) {
            resolveUpstream(query, dns, bestEffort)
        } else {
            resolveUdpFallback(query, dns.udpFallbackServerIp, dns.udpFallbackServerPort, bestEffort)
        }
    }

    suspend fun resolveUpstream(
        query: ByteArray,
        dns: DnsConfig,
        bestEffort: BestEffortConfig
    ): ByteArray {
        return when (dns.transportMode) {
            DnsTransportMode.DoH -> resolveDoH(query, dns, bestEffort)
            DnsTransportMode.DoT -> resolveDoT(query, dns, bestEffort)
            DnsTransportMode.SystemUdp -> resolveUdpFallback(query, dns.udpFallbackServerIp, dns.udpFallbackServerPort, bestEffort)
        }
    }

    private suspend fun resolveUdpFallback(
        query: ByteArray,
        serverIp: String,
        serverPort: Int,
        bestEffort: BestEffortConfig
    ): ByteArray {
        // For safe MVP we keep it simple: send UDP query and await response.
        return withContext(Dispatchers.IO) {
            DatagramSocket().use { socket ->
                socket.soTimeout = 2500
                val address = InetAddress.getByName(serverIp)
                val packet = DatagramPacket(query, query.size, address, serverPort)
                socket.send(packet)
                val buf = ByteArray(4096)
                val respPacket = DatagramPacket(buf, buf.size)
                socket.receive(respPacket)
                respPacket.data.copyOf(respPacket.length)
            }
        }
    }

    private fun resolveDoHEndpoint(dns: DnsConfig): String {
        if (dns.transportMode != DnsTransportMode.DoH) return ""
        if (dns.customEndpoint != null && dns.customEndpoint.isNotBlank()) return dns.customEndpoint
        return when (dns.preset) {
            DnsPreset.Cloudflare -> "https://cloudflare-dns.com/dns-query"
            DnsPreset.Google -> "https://dns.google/resolve"
            DnsPreset.AdGuard -> "https://dns.adguard.com/dns-query"
            DnsPreset.NextDNS -> "https://dns.nextdns.io/"
            DnsPreset.Custom -> dns.customEndpoint ?: "https://cloudflare-dns.com/dns-query"
        }
    }

    private fun resolveDoTHost(dns: DnsConfig): String {
        if (dns.customEndpoint != null && dns.customEndpoint.isNotBlank()) {
            // For DoT custom endpoint we accept "host" or "host:port".
            val value = dns.customEndpoint.trim().removePrefix("https://").removePrefix("tcp://")
            return value.substringBefore(":")
        }
        return when (dns.preset) {
            DnsPreset.Cloudflare -> "cloudflare-dns.com"
            DnsPreset.Google -> "dns.google"
            DnsPreset.AdGuard -> "dns.adguard.com"
            DnsPreset.NextDNS -> "dns.nextdns.io"
            DnsPreset.Custom -> "cloudflare-dns.com"
        }
    }

    private suspend fun resolveDoH(query: ByteArray, dns: DnsConfig, bestEffort: BestEffortConfig): ByteArray {
        val endpoint = resolveDoHEndpoint(dns)
        if (endpoint.isBlank()) return resolveUdpFallback(query, dns.udpFallbackServerIp, dns.udpFallbackServerPort, bestEffort)

        val body: RequestBody = if (bestEffort.tlsFragmentationEnabled) {
            TlsFragmenter.fragmentedRequestBody(
                query,
                bestEffort.tlsFragmentSizeBytes,
                bestEffort.tlsFragmentDelayMs
            )
        } else {
            RequestBody.create(dnsMessageMediaType, query)
        }

        suspend fun sendOnce(): ByteArray {
            return withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .header("accept", "application/dns-message")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("DoH failed: ${response.code}")
                    }
                    val bytes = response.body?.bytes() ?: throw IllegalStateException("DoH empty response")
                    bytes
                }
            }
        }

        return DesyncEngine.withDesync(
            bestEffort = bestEffort,
            block = { sendOnce() },
            fakeProbe = { sendOnce() }
        )
    }

    private suspend fun resolveDoT(query: ByteArray, dns: DnsConfig, bestEffort: BestEffortConfig): ByteArray {
        val host = resolveDoTHost(dns)
        val port = 853

        suspend fun sendOnce(): ByteArray {
            return withContext(Dispatchers.IO) {
                val socket = sslSocketFactory.createSocket(host, port) as SSLSocket
                try {
                    socket.soTimeout = 3000
                    socket.startHandshake()
                    val out = socket.outputStream
                    val input = socket.inputStream

                    val len = query.size
                    val prefix = byteArrayOf(
                        ((len ushr 8) and 0xFF).toByte(),
                        (len and 0xFF).toByte()
                    )

                    if (bestEffort.tlsFragmentationEnabled) {
                        // Write length prefix once, then fragment the DNS bytes.
                        out.write(prefix)
                        out.flush()
                        TlsFragmenter.writeInFragments(out, query, bestEffort.tlsFragmentSizeBytes, bestEffort.tlsFragmentDelayMs)
                    } else {
                        out.write(prefix)
                        out.write(query)
                        out.flush()
                    }

                    val hdr = ByteArray(2)
                    input.readFully(hdr)
                    val respLen = ((hdr[0].toInt() and 0xFF) shl 8) or (hdr[1].toInt() and 0xFF)
                    val resp = ByteArray(respLen)
                    input.readFully(resp)
                    resp
                } finally {
                    try {
                        socket.close()
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        return DesyncEngine.withDesync(
            bestEffort = bestEffort,
            block = { sendOnce() },
            fakeProbe = { sendOnce() }
        )
    }

    private fun java.io.InputStream.readFully(target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = read(target, offset, target.size - offset)
            if (read < 0) throw IllegalStateException("Unexpected EOF")
            offset += read
        }
    }
}

