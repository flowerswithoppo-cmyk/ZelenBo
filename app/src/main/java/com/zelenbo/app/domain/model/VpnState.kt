package com.zelenbo.app.domain.model

data class VpnState(
    val isConnecting: Boolean,
    val isConnected: Boolean,
    val statusText: String,
    val lastError: String? = null
)

