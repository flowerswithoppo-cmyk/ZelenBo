package com.zelenbo.app.presentation.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.usecase.ObserveConfigUseCase
import com.zelenbo.app.domain.usecase.UpdateConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

@HiltViewModel
class ServicesViewModel @Inject constructor(
    observeConfigUseCase: ObserveConfigUseCase,
    private val updateConfigUseCase: UpdateConfigUseCase
) : ViewModel() {

    private val configFlow = observeConfigUseCase()
    private val initialState = ServicesUiState.empty()
    val uiState: StateFlow<ServicesUiState> = configFlow
        .map { ServicesUiState.fromConfig(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)

    private val _customDomainInput = MutableStateFlow("")
    val customDomainInput: StateFlow<String> = _customDomainInput.asStateFlow()

    fun setCustomDomainInput(value: String) {
        _customDomainInput.value = value
    }

    fun toggleService(serviceId: ServiceId, enabled: Boolean) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                val nextEnabled = if (enabled) current.enabledServices + serviceId else current.enabledServices - serviceId
                val nextOptimized = nextOptimizedDomains(
                    current = current,
                    enabledServices = nextEnabled,
                    toggled = serviceId
                )
                current.copy(
                    enabledServices = nextEnabled,
                    optimizedDomains = nextOptimized
                )
            }
        }
    }

    fun addCustomDomain(raw: String) {
        val cleaned = sanitizeDomain(raw)
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(
                    optimizedDomains = current.optimizedDomains + cleaned
                )
            }
        }
        _customDomainInput.value = ""
    }

    fun removeDomain(domain: String) {
        viewModelScope.launch {
            updateConfigUseCase { current ->
                current.copy(
                    optimizedDomains = current.optimizedDomains - domain
                )
            }
        }
    }

    private fun nextOptimizedDomains(
        current: ZelenBoConfig,
        enabledServices: Set<ServiceId>,
        toggled: ServiceId
    ): Set<String> {
        val builtIn = builtInDomainsForServices(enabledServices)
        // Keep user custom domains by removing only built-in domains of the toggled service.
        val toggledBuiltIn = builtInDomainsByService[toggled].orEmpty()
        val keptUserDomains = current.optimizedDomains - toggledBuiltIn
        return keptUserDomains union builtIn
    }

    private fun builtInDomainsForServices(services: Set<ServiceId>): Set<String> {
        val result = HashSet<String>()
        for (id in services) {
            result.addAll(builtInDomainsByService[id].orEmpty())
        }
        return result
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

    private val builtInDomainsByService: Map<ServiceId, Set<String>> = mapOf(
        ServiceId.Telegram to setOf("t.me", "telegram.me", "telegram.org"),
        ServiceId.YouTube to setOf("youtube.com", "youtu.be"),
        ServiceId.Discord to setOf("discord.com", "discord.gg", "discordapp.com"),
        ServiceId.Instagram to setOf("instagram.com"),
        ServiceId.TwitterX to setOf("twitter.com", "x.com"),
        ServiceId.CustomDomains to emptySet()
    )
}

data class ServicesUiState(
    val enabledServices: Set<ServiceId>,
    val optimizedDomains: Set<String>
) {
    companion object {
        fun empty(): ServicesUiState = ServicesUiState(
            enabledServices = emptySet(),
            optimizedDomains = emptySet()
        )

        fun fromConfig(config: ZelenBoConfig): ServicesUiState = ServicesUiState(
            enabledServices = config.enabledServices,
            optimizedDomains = config.optimizedDomains
        )
    }
}

