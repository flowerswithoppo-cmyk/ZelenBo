package com.zelenbo.app.domain.repository

import com.zelenbo.app.domain.model.VpnState
import com.zelenbo.app.domain.model.ZelenBoConfig
import kotlinx.coroutines.flow.StateFlow

interface VpnGateway {
    val vpnState: StateFlow<VpnState>

    suspend fun start(config: ZelenBoConfig)
    suspend fun stop()
}

