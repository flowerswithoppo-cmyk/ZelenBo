package com.zelenbo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.usecase.ConfigCodec
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ZelenBoVpnService : VpnService() {

    @Inject lateinit var vpnStateStore: VpnStateStore
    @Inject lateinit var trafficManager: TrafficManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pfd: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val encoded = intent.getStringExtra(EXTRA_CONFIG_ENCODED)
                if (encoded.isNullOrBlank()) {
                    vpnStateStore.setDisconnected("Отсутствует конфигурация")
                    stopSelf()
                    return START_NOT_STICKY
                }

                vpnStateStore.setConnecting()
                startForeground(NOTIFICATION_ID, createNotification(isConnected = false))

                val decoded = ConfigCodec.decode(encoded)
                if (decoded.isFailure) {
                    vpnStateStore.setDisconnected("Ошибка конфигурации")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val config = decoded.getOrThrow()

                serviceScope.launch {
                    startVpnInternal(config)
                }
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    stopVpnInternal()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            stopVpnInternal()
        }
        super.onDestroy()
    }

    private suspend fun startVpnInternal(config: ZelenBoConfig) {
        try {
            // Stop previous instance if any.
            stopVpnInternal()

            val p = Builder()
                .setSession("ZelenBo")
                .setMtu(1500)
                .addAddress(VPN_DNS_IP, VPN_PREFIX_LEN)
                .addRoute(VPN_SUBNET, VPN_PREFIX_LEN)
                .addDnsServer(VPN_DNS_IP)
                .establish()

            pfd = p

            val runtime = TrafficManager.RuntimeConfig(
                vpnDnsIp = VPN_DNS_IP,
                dnsConfig = config.dns,
                bestEffort = config.bestEffort,
                optimizedDomains = config.optimizedDomains
            )

            trafficManager.start(
                vpnTunFd = p.fileDescriptor,
                runtimeConfig = runtime
            )

            vpnStateStore.setConnected()
            startForeground(NOTIFICATION_ID, createNotification(isConnected = true))
        } catch (t: Throwable) {
            vpnStateStore.setDisconnected(t.message ?: "Ошибка запуска VPN")
            stopSelf()
        }
    }

    private suspend fun stopVpnInternal() {
        try {
            trafficManager.stop()
        } catch (_: Throwable) {
        }
        try {
            pfd?.close()
        } catch (_: Throwable) {
        }
        pfd = null
        vpnStateStore.setDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(isConnected: Boolean): Notification {
        val channelId = "zelenbo_vpn"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ZelenBo VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val title = "ZelenBo"
        val text = if (isConnected) "Подключено" else "Подключение…"

        val launchIntent = Intent(this, com.zelenbo.app.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun Builder(): android.net.VpnService.Builder = android.net.VpnService.Builder(this)

    companion object {
        const val ACTION_START = "com.zelenbo.app.action.START"
        const val ACTION_STOP = "com.zelenbo.app.action.STOP"
        const val EXTRA_CONFIG_ENCODED = "extra_config_encoded"
        private const val NOTIFICATION_ID = 1

        // VPN DNS server address inside the VPN interface.
        private const val VPN_DNS_IP = "10.20.30.1"
        private const val VPN_PREFIX_LEN = 24
        private const val VPN_SUBNET = "10.20.30.0"
    }
}

