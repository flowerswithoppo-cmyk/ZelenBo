package com.zelenbo.app.service

import com.zelenbo.app.domain.model.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class VpnStateStore @Inject constructor() {
    private val _state = MutableStateFlow(
        VpnState(
            isConnecting = false,
            isConnected = false,
            statusText = "Отключено",
            lastError = null
        )
    )

    val state: StateFlow<VpnState> = _state

    fun setConnecting() {
        _state.value = _state.value.copy(
            isConnecting = true,
            isConnected = false,
            statusText = "Подключение…",
            lastError = null
        )
    }

    fun setConnected() {
        _state.value = _state.value.copy(
            isConnecting = false,
            isConnected = true,
            statusText = "Подключено",
            lastError = null
        )
    }

    fun setDisconnected(error: String? = null) {
        _state.value = _state.value.copy(
            isConnecting = false,
            isConnected = false,
            statusText = "Отключено",
            lastError = error
        )
    }
}

