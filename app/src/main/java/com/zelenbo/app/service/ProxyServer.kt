package com.zelenbo.app.service

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Local SOCKS5 proxy server bound to 127.0.0.1:1080.
 *
 * MVP-safe behavior: SOCKS5 CONNECT is forwarded directly to destination.
 * (Shadowsocks/VLESS upstream dialing is intentionally not implemented as part of the safe MVP.)
 */
class ProxyServer @Inject constructor() {

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val bufferSize = 32 * 1024

    fun start(
        scope: CoroutineScope,
        bindAddress: String = "127.0.0.1",
        port: Int = 1080
    ) {
        if (serverSocket != null) return
        serverSocket = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(bindAddress, port))
        }

        acceptJob = scope.launch(Dispatchers.IO) {
            while (scope.coroutineContext.isActive) {
                try {
                    val client = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) { handleClient(client) }
                } catch (_: IOException) {
                    // server closed
                    break
                }
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
    }

    private suspend fun handleClient(client: Socket) {
        client.use { sock ->
            sock.soTimeout = 15_000
            val input = sock.getInputStream()
            val output = sock.getOutputStream()

            // Method selection
            val header = ByteArray(2)
            if (!readExact(input, header)) return
            val ver = header[0].toInt() and 0xFF
            val nMethods = header[1].toInt() and 0xFF
            if (ver != 0x05) return

            val methods = ByteArray(nMethods)
            if (!readExact(input, methods)) return
            // No-auth supported (0x00) only
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            // Request
            val req = ByteArray(4)
            if (!readExact(input, req)) return
            val reqVer = req[0].toInt() and 0xFF
            val cmd = req[1].toInt() and 0xFF
            val atyp = req[3].toInt() and 0xFF
            if (reqVer != 0x05) return

            val destHost: String
            val destPort: Int
            when (atyp) {
                0x01 -> { // IPv4
                    val addr = ByteArray(4)
                    if (!readExact(input, addr)) return
                    destHost = InetAddress.getByAddress(addr).hostAddress
                }
                0x03 -> { // DOMAIN
                    val lenBuf = ByteArray(1)
                    if (!readExact(input, lenBuf)) return
                    val len = lenBuf[0].toInt() and 0xFF
                    val nameBytes = ByteArray(len)
                    if (!readExact(input, nameBytes)) return
                    destHost = String(nameBytes, Charsets.UTF_8)
                }
                0x04 -> { // IPv6
                    val addr = ByteArray(16)
                    if (!readExact(input, addr)) return
                    destHost = InetAddress.getByAddress(addr).hostAddress
                }
                else -> {
                    sendReply(output, 0x08)
                    return
                }
            }

            val portBuf = ByteArray(2)
            if (!readExact(input, portBuf)) return
            destPort = ((portBuf[0].toInt() and 0xFF) shl 8) or (portBuf[1].toInt() and 0xFF)

            if (cmd != 0x01) {
                // CONNECT only
                sendReply(output, 0x07)
                return
            }

            val remote = try {
                withContext(Dispatchers.IO) { Socket(destHost, destPort) }
            } catch (_: Throwable) {
                sendReply(output, 0x05)
                return
            }

            remote.use { r ->
                val localAddr = r.localAddress as? InetAddress
                val localPort = r.localPort
                val repAtyp = when (localAddr?.address?.size) {
                    4 -> 0x01
                    16 -> 0x04
                    else -> 0x01
                }
                val bindAddrBytes = when (repAtyp) {
                    0x01 -> (localAddr ?: InetAddress.getByName("127.0.0.1")).address
                    else -> (localAddr ?: InetAddress.getByName("::1")).address
                }

                // Reply: success
                val reply = ByteArray(4 + bindAddrBytes.size + 2)
                reply[0] = 0x05
                reply[1] = 0x00
                reply[2] = 0x00
                reply[3] = repAtyp.toByte()
                System.arraycopy(bindAddrBytes, 0, reply, 4, bindAddrBytes.size)
                reply[4 + bindAddrBytes.size] = ((localPort ushr 8) and 0xFF).toByte()
                reply[4 + bindAddrBytes.size + 1] = (localPort and 0xFF).toByte()
                output.write(reply)
                output.flush()

                // Relay data
                coroutineScope {
                    val clientToRemote = launch(Dispatchers.IO) {
                        copyStream(input, r.getOutputStream())
                    }
                    val remoteToClient = launch(Dispatchers.IO) {
                        copyStream(r.getInputStream(), output)
                    }
                    clientToRemote.join()
                    remoteToClient.join()
                }
            }
        }
    }

    private fun sendReply(output: OutputStream, rep: Int) {
        try {
            // reply: VER=5, REP=rep, RSV=0, ATYP=IPv4=1, BND.ADDR=0.0.0.0, BND.PORT=0
            output.write(byteArrayOf(0x05, rep.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
            output.flush()
        } catch (_: Throwable) {
        }
    }

    private fun readExact(input: InputStream, buf: ByteArray): Boolean {
        var offset = 0
        while (offset < buf.size) {
            val read = input.read(buf, offset, buf.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }

    private suspend fun copyStream(input: InputStream, output: OutputStream) {
        val buf = ByteArray(bufferSize)
        while (true) {
            val read = withContext(Dispatchers.IO) { input.read(buf) }
            if (read <= 0) break
            withContext(Dispatchers.IO) { output.write(buf, 0, read) }
            withContext(Dispatchers.IO) { output.flush() }
        }
    }
}

