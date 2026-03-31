package com.zelenbo.app.service

object DnsPacketCodec {

    data class ParsedDnsQuery(
        val clientPort: Int,
        val dnsPayload: ByteArray,
        val queryName: String?
    )

    fun ipv4ToBytes(ip: String): ByteArray {
        val parts = ip.split(".")
        require(parts.size == 4)
        return byteArrayOf(
            parts[0].toInt().toByte(),
            parts[1].toInt().toByte(),
            parts[2].toInt().toByte(),
            parts[3].toInt().toByte()
        )
    }

    fun parseIpv4UdpDnsQuery(
        packet: ByteArray,
        vpnDnsBytes: ByteArray,
        extractQName: (ByteArray) -> String?
    ): ParsedDnsQuery? {
        if (packet.size < 28) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihl = packet[0].toInt() and 0x0F
        if (ihl != 5) return null // MVP: only IPv4 without options
        val ipHeaderLen = ihl * 4
        if (packet.size < ipHeaderLen + 8) return null

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

        val queryName = extractQName(dnsPayload)?.lowercase()
        return ParsedDnsQuery(
            clientPort = srcPort,
            dnsPayload = dnsPayload,
            queryName = queryName
        )
    }

    fun buildIpv4UdpDnsResponse(
        request: ByteArray,
        udpPayload: ByteArray,
        vpnDnsBytes: ByteArray
    ): ByteArray? {
        if (request.size < 28) return null

        val versionAndIhl = request[0]
        val ihl = versionAndIhl.toInt() and 0x0F
        if (ihl != 5) return null // MVP: only IPv4 without options
        val ipHeaderLen = ihl * 4
        if (ipHeaderLen < 20) return null

        val srcIp = request.copyOfRange(12, 16) // client IP

        val udpStart = ipHeaderLen
        val clientPort = u16(request, udpStart) // request src port

        val dnsLen = udpPayload.size
        val udpLen = 8 + dnsLen
        val totalLen = ipHeaderLen + udpLen
        val response = ByteArray(totalLen)

        // IPv4 header (no options)
        response[0] = (4 shl 4 or ihl).toByte()
        response[1] = 0
        putU16(response, 2, totalLen)
        putU16(response, 4, u16(request, 4)) // ID
        response[6] = request[6]
        response[7] = request[7]
        response[8] = 64.toByte()
        response[9] = 17.toByte() // UDP
        response[10] = 0
        response[11] = 0

        // Src IP = vpnDnsIp
        System.arraycopy(vpnDnsBytes, 0, response, 12, 4)
        // Dst IP = client IP
        System.arraycopy(srcIp, 0, response, 16, 4)

        val ipChecksum = ipv4HeaderChecksum(response, 0, ipHeaderLen)
        putU16(response, 10, ipChecksum)

        // UDP header
        val udpOutStart = ipHeaderLen
        putU16(response, udpOutStart, 53) // Src port
        putU16(response, udpOutStart + 2, clientPort) // Dst port
        putU16(response, udpOutStart + 4, udpLen)
        putU16(response, udpOutStart + 6, 0) // checksum placeholder
        System.arraycopy(udpPayload, 0, response, udpOutStart + 8, dnsLen)

        val udpChecksum = udpChecksum(response, vpnDnsBytes, srcIp, udpOutStart)
        putU16(response, udpOutStart + 6, udpChecksum)

        return response
    }

    private fun udpChecksum(packet: ByteArray, srcIp: ByteArray, dstIp: ByteArray, udpHeaderStart: Int): Int {
        val payloadSize = packet.size - (udpHeaderStart + 8)
        val udpLen = 8 + payloadSize

        val pseudo = ByteArray(12 + udpLen)
        System.arraycopy(srcIp, 0, pseudo, 0, 4)
        System.arraycopy(dstIp, 0, pseudo, 4, 4)
        pseudo[8] = 0
        pseudo[9] = 17.toByte()
        pseudo[10] = ((udpLen ushr 8) and 0xFF).toByte()
        pseudo[11] = (udpLen and 0xFF).toByte()

        // UDP header: copy ports/len; set checksum=0
        val srcPort = u16(packet, udpHeaderStart)
        val dstPort = u16(packet, udpHeaderStart + 2)
        pseudo[12] = ((srcPort ushr 8) and 0xFF).toByte()
        pseudo[13] = (srcPort and 0xFF).toByte()
        pseudo[14] = ((dstPort ushr 8) and 0xFF).toByte()
        pseudo[15] = (dstPort and 0xFF).toByte()
        pseudo[16] = ((udpLen ushr 8) and 0xFF).toByte()
        pseudo[17] = (udpLen and 0xFF).toByte()
        pseudo[18] = 0
        pseudo[19] = 0

        // Payload
        System.arraycopy(packet, udpHeaderStart + 8, pseudo, 20, payloadSize)

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

    private fun u16(b: ByteArray, offset: Int): Int {
        return ((b[offset].toInt() and 0xFF) shl 8) or (b[offset + 1].toInt() and 0xFF)
    }

    private fun putU16(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = ((value ushr 8) and 0xFF).toByte()
        buf[offset + 1] = (value and 0xFF).toByte()
    }
}

