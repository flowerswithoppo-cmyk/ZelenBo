package com.zelenbo.app.service

import java.io.OutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink

object TlsFragmenter {

    private val DnsMessageMediaType = "application/dns-message".toMediaTypeOrNull()

    fun fragmentedRequestBody(
        bodyBytes: ByteArray,
        fragmentSizeBytes: Int,
        fragmentDelayMs: Int
    ): RequestBody {
        val safeSize = fragmentSizeBytes.coerceIn(1, 64)
        val safeDelay = fragmentDelayMs.coerceIn(0, 500)

        return object : RequestBody() {
            override fun contentType() = DnsMessageMediaType
            override fun contentLength(): Long = bodyBytes.size.toLong()

            override fun writeTo(sink: BufferedSink) {
                var offset = 0
                while (offset < bodyBytes.size) {
                    val end = (offset + safeSize).coerceAtMost(bodyBytes.size)
                    sink.write(bodyBytes, offset, end - offset)
                    sink.flush()
                    offset = end
                    if (offset < bodyBytes.size && safeDelay > 0) {
                        try {
                            Thread.sleep(safeDelay.toLong())
                        } catch (_: InterruptedException) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    fun writeInFragments(outputStream: OutputStream, bytes: ByteArray, fragmentSizeBytes: Int, fragmentDelayMs: Int) {
        val safeSize = fragmentSizeBytes.coerceIn(1, 64)
        val safeDelay = fragmentDelayMs.coerceIn(0, 500)
        var offset = 0
        while (offset < bytes.size) {
            val end = (offset + safeSize).coerceAtMost(bytes.size)
            outputStream.write(bytes, offset, end - offset)
            outputStream.flush()
            offset = end
            if (offset < bytes.size && safeDelay > 0) {
                try {
                    Thread.sleep(safeDelay.toLong())
                } catch (_: InterruptedException) {
                    // ignore
                }
            }
        }
    }
}

