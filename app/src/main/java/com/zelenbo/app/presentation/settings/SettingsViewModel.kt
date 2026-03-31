package com.zelenbo.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelenbo.app.domain.model.BestEffortConfig
import com.zelenbo.app.domain.model.DnsConfig
import com.zelenbo.app.domain.model.DnsPreset
import com.zelenbo.app.domain.model.DnsTransportMode
import com.zelenbo.app.domain.model.ProxyMode
import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.domain.usecase.ExportConfigUseCase
import com.zelenbo.app.domain.usecase.ImportConfigUseCase
import com.zelenbo.app.domain.usecase.ObserveConfigUseCase
import com.zelenbo.app.domain.usecase.UpdateConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeConfigUseCase: ObserveConfigUseCase,
    private val updateConfigUseCase: UpdateConfigUseCase,
    private val exportConfigUseCase: ExportConfigUseCase,
    private val importConfigUseCase: ImportConfigUseCase
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

    val config: StateFlow<ZelenBoConfig> = observeConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultConfig)

    private val _customDomainInput = MutableStateFlow("")
    val customDomainInput: StateFlow<String> = _customDomainInput.asStateFlow()

    private val _importEncoded = MutableStateFlow("")
    val importEncoded: StateFlow<String> = _importEncoded.asStateFlow()

    private val _exportEncoded = MutableStateFlow("")
    val exportEncoded: StateFlow<String> = _exportEncoded.asStateFlow()

    fun setCustomDomainInput(value: String) {
        _customDomainInput.value = value
    }

    fun setImportEncoded(value: String) {
        _importEncoded.value = value
    }

    fun setExportEncoded(value: String) {
        _exportEncoded.value = value
    }

    fun setDnsPreset(preset: DnsPreset) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(dns = current.dns.copy(preset = preset))
            }
        }
    }

    fun setDnsTransportMode(mode: DnsTransportMode) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(dns = current.dns.copy(transportMode = mode))
            }
        }
    }

    fun setDnsCustomEndpoint(endpoint: String) {
        val cleaned = endpoint.trim().ifBlank { null }
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(dns = current.dns.copy(customEndpoint = cleaned))
            }
        }
    }

    fun setTlsFragmentationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(
                    bestEffort = current.bestEffort.copy(tlsFragmentationEnabled = enabled)
                )
            }
        }
    }

    fun setTlsFragmentSizeBytes(value: Int) {
        val safe = value.coerceIn(1, 64)
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(bestEffort = current.bestEffort.copy(tlsFragmentSizeBytes = safe))
            }
        }
    }

    fun setTlsFragmentDelayMs(value: Int) {
        val safe = value.coerceIn(0, 500)
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(bestEffort = current.bestEffort.copy(tlsFragmentDelayMs = safe))
            }
        }
    }

    fun setTcpDesyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(bestEffort = current.bestEffort.copy(tcpDesyncEnabled = enabled))
            }
        }
    }

    fun setTcpDesyncMode(mode: String) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(bestEffort = current.bestEffort.copy(tcpDesyncMode = mode))
            }
        }
    }

    fun setEchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(bestEffort = current.bestEffort.copy(echEnabled = enabled))
            }
        }
    }

    fun setProxyMode(mode: ProxyMode) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(proxyMode = mode)
            }
        }
    }

    fun addCustomDomain(raw: String) {
        val cleaned = sanitizeDomain(raw)
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(optimizedDomains = current.optimizedDomains + cleaned)
            }
        }
        _customDomainInput.value = ""
    }

    fun removeDomain(domain: String) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(optimizedDomains = current.optimizedDomains - domain)
            }
        }
    }

    fun exportNow() {
        viewModelScope.launch {
            val current = config.value
            val encoded = exportConfigUseCase(current)
            _exportEncoded.value = encoded
        }
    }

    fun importNow(): Result<Unit> {
        return try {
            val encoded = importEncoded.value.trim()
            val result = importConfigUseCase(encoded)
            if (result.isFailure) {
                Result.failure(result.exceptionOrNull() ?: IllegalArgumentException("Import failed"))
            } else {
                viewModelScope.launch {
                    updateConfigUseCase { _ ->
                        result.getOrThrow()
                    }
                }
                Result.success(Unit)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            updateConfigUseCase { _ -> defaultConfig }
        }
    }

    private fun sanitizeDomain(raw: String): String {
        var s = raw.trim().lowercase()
        if (s.startsWith("http://")) s = s.removePrefix("http://")
        if (s.startsWith("https://")) s = s.removePrefix("https://")
        val slashIndex = s.indexOf('/')
        if (slashIndex >= 0) s = s.substring(0, slashIndex)
        val qIndex = s.indexOf('?')
        if (qIndex >= 0) s = s.substring(0, qIndex)
        val colonIndex = s.indexOf(':')
        if (colonIndex >= 0) s = s.substring(0, colonIndex)
        return s.trim('.')
    }
}

