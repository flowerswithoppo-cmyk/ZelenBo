package com.zelenbo.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelenbo.app.domain.model.BestEffortConfig
import com.zelenbo.app.domain.model.DnsConfig
import com.zelenbo.app.domain.model.DnsPreset
import com.zelenbo.app.domain.model.DnsTransportMode
import com.zelenbo.app.domain.model.ProxyMode
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.domain.model.VpnState
import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.repository.VpnGateway
import com.zelenbo.app.domain.usecase.ObserveConfigUseCase
import com.zelenbo.app.domain.usecase.StartVpnUseCase
import com.zelenbo.app.domain.usecase.StopVpnUseCase
import com.zelenbo.app.domain.usecase.UpdateConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vpnGateway: VpnGateway,
    observeConfigUseCase: ObserveConfigUseCase,
    private val startVpnUseCase: StartVpnUseCase,
    private val stopVpnUseCase: StopVpnUseCase,
    private val updateConfigUseCase: UpdateConfigUseCase
) : ViewModel() {

    private val defaultConfig = ZelenBoConfig(
        enabledServices = setOf(ServiceId.Telegram),
        dns = DnsConfig(
            transportMode = DnsTransportMode.DoH,
            preset = DnsPreset.Cloudflare,
            customEndpoint = null,
            udpFallbackServerIp = "1.1.1.1",
            udpFallbackServerPort = 53
        ),
        bestEffort = BestEffortConfig(
            tlsFragmentationEnabled = false,
            tlsFragmentSizeBytes = 3,
            tlsFragmentDelayMs = 0,
            tcpDesyncEnabled = false,
            tcpDesyncMode = "split",
            echEnabled = false
        ),
        proxyMode = ProxyMode.None,
        optimizedDomains = setOf("t.me", "telegram.me", "telegram.org")
    )

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val configFlow = observeConfigUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        defaultConfig
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        vpnGateway.vpnState,
        configFlow,
        _busy
    ) { vpnState, config, busy ->
        DashboardUiState(
            vpnState = vpnState,
            config = config,
            isBusy = busy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.initial(defaultConfig))

    fun onToggleMain() {
        viewModelScope.launch {
            if (_busy.value) return@launch
            _busy.value = true
            try {
                if (vpnGateway.vpnState.value.isConnected) {
                    stopVpnUseCase()
                } else {
                    startVpnUseCase()
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun setTelegramEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                val newSet = if (enabled) {
                    current.enabledServices + ServiceId.Telegram
                } else {
                    current.enabledServices - ServiceId.Telegram
                }
                current.copy(enabledServices = newSet)
            }
        }
    }
}

data class DashboardUiState(
    val vpnState: VpnState,
    val config: ZelenBoConfig,
    val isBusy: Boolean
) {
    companion object {
        fun initial(defaultConfig: ZelenBoConfig): DashboardUiState = DashboardUiState(
            vpnState = VpnState(
                isConnecting = false,
                isConnected = false,
                statusText = "Отключено",
                lastError = null
            ),
            config = defaultConfig,
            isBusy = false
        )
    }
}

