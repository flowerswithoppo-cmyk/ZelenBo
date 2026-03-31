package com.zelenbo.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.repository.VpnGateway
import com.zelenbo.app.domain.usecase.ConfigCodec
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class VpnGatewayImpl @Inject constructor(
    private val appContext: Context,
    private val vpnStateStore: VpnStateStore
) : VpnGateway {

    override val vpnState: StateFlow<com.zelenbo.app.domain.model.VpnState>
        get() = vpnStateStore.state

    override suspend fun start(config: ZelenBoConfig) {
        val encoded = ConfigCodec.encode(config)
        val intent = Intent(appContext, ZelenBoVpnService::class.java).apply {
            action = ZelenBoVpnService.ACTION_START
            putExtra(ZelenBoVpnService.EXTRA_CONFIG_ENCODED, encoded)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    override suspend fun stop() {
        val intent = Intent(appContext, ZelenBoVpnService::class.java).apply {
            action = ZelenBoVpnService.ACTION_STOP
        }
        appContext.startService(intent)
    }
}

