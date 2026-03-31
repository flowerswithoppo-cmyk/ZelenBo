package com.zelenbo.app.data.repository.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.BEST_EFFORT_TLS_FRAGMENT_DELAY_MS
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.BEST_EFFORT_TLS_FRAGMENT_SIZE_BYTES
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.BEST_EFFORT_TLS_FRAGMENTATION_ENABLED
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.BEST_EFFORT_TCP_DESYNC_ENABLED
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.BEST_EFFORT_TCP_DESYNC_MODE
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.BEST_EFFORT_ECH_ENABLED
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.DNS_CUSTOM_ENDPOINT
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.DNS_PRESET
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.DNS_TRANSPORT_MODE
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.DNS_UDP_FALLBACK_SERVER_IP
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.DNS_UDP_FALLBACK_SERVER_PORT
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.ENABLED_SERVICES
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.OPTIMIZED_DOMAINS
import com.zelenbo.app.data.repository.local.ZelenBoPreferencesKeys.PROXY_MODE
import com.zelenbo.app.domain.model.BestEffortConfig
import com.zelenbo.app.domain.model.DnsConfig
import com.zelenbo.app.domain.model.DnsPreset
import com.zelenbo.app.domain.model.DnsTransportMode
import com.zelenbo.app.domain.model.ProxyMode
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private object ZelenBoDefaults {
    val optimizedDomains = setOf("t.me", "telegram.me", "telegram.org")

    val bestEffort = BestEffortConfig(
        tlsFragmentationEnabled = false,
        tlsFragmentSizeBytes = 3,
        tlsFragmentDelayMs = 0,
        tcpDesyncEnabled = false,
        tcpDesyncMode = "split",
        echEnabled = false
    )

    val dns = DnsConfig(
        transportMode = DnsTransportMode.DoH,
        preset = DnsPreset.Cloudflare,
        customEndpoint = null,
        udpFallbackServerIp = "1.1.1.1",
        udpFallbackServerPort = 53
    )

    val proxyMode = ProxyMode.None
}

object ZelenBoPreferencesKeys {
    val ENABLED_SERVICES = stringSetPreferencesKey("enabled_services")
    val OPTIMIZED_DOMAINS = stringSetPreferencesKey("optimized_domains")

    val DNS_TRANSPORT_MODE = stringPreferencesKey("dns_transport_mode")
    val DNS_PRESET = stringPreferencesKey("dns_preset")
    val DNS_CUSTOM_ENDPOINT = stringPreferencesKey("dns_custom_endpoint")

    val DNS_UDP_FALLBACK_SERVER_IP = stringPreferencesKey("dns_udp_fallback_ip")
    val DNS_UDP_FALLBACK_SERVER_PORT = intPreferencesKey("dns_udp_fallback_port")

    val PROXY_MODE = stringPreferencesKey("proxy_mode")

    val BEST_EFFORT_TLS_FRAGMENTATION_ENABLED = booleanPreferencesKey("best_tls_fragmentation_enabled")
    val BEST_EFFORT_TLS_FRAGMENT_SIZE_BYTES = intPreferencesKey("best_tls_fragment_size_bytes")
    val BEST_EFFORT_TLS_FRAGMENT_DELAY_MS = intPreferencesKey("best_tls_fragment_delay_ms")

    val BEST_EFFORT_TCP_DESYNC_ENABLED = booleanPreferencesKey("best_tcp_desync_enabled")
    val BEST_EFFORT_TCP_DESYNC_MODE = stringPreferencesKey("best_tcp_desync_mode")

    val BEST_EFFORT_ECH_ENABLED = booleanPreferencesKey("best_ech_enabled")
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val configFlow: Flow<ZelenBoConfig> = dataStore.data.map { prefs ->
        val enabledServicesRaw = prefs[ENABLED_SERVICES].orEmpty()
        val enabledServices = enabledServicesRaw.mapNotNull { runCatching { ServiceId.valueOf(it) }.getOrNull() }.toSet()
        val optimizedDomains = prefs[OPTIMIZED_DOMAINS].orEmpty()

        val dnsTransportMode = prefs[DNS_TRANSPORT_MODE]?.let { runCatching { DnsTransportMode.valueOf(it) }.getOrNull() }
            ?: ZelenBoDefaults.dns.transportMode
        val dnsPreset = prefs[DNS_PRESET]?.let { runCatching { DnsPreset.valueOf(it) }.getOrNull() }
            ?: ZelenBoDefaults.dns.preset
        val customEndpoint = prefs[DNS_CUSTOM_ENDPOINT]

        val udpIp = prefs[DNS_UDP_FALLBACK_SERVER_IP] ?: ZelenBoDefaults.dns.udpFallbackServerIp
        val udpPort = prefs[DNS_UDP_FALLBACK_SERVER_PORT] ?: ZelenBoDefaults.dns.udpFallbackServerPort

        val dns = DnsConfig(
            transportMode = dnsTransportMode,
            preset = dnsPreset,
            customEndpoint = customEndpoint,
            udpFallbackServerIp = udpIp,
            udpFallbackServerPort = udpPort
        )

        val bestEffort = BestEffortConfig(
            tlsFragmentationEnabled = prefs[BEST_EFFORT_TLS_FRAGMENTATION_ENABLED] ?: ZelenBoDefaults.bestEffort.tlsFragmentationEnabled,
            tlsFragmentSizeBytes = prefs[BEST_EFFORT_TLS_FRAGMENT_SIZE_BYTES] ?: ZelenBoDefaults.bestEffort.tlsFragmentSizeBytes,
            tlsFragmentDelayMs = prefs[BEST_EFFORT_TLS_FRAGMENT_DELAY_MS] ?: ZelenBoDefaults.bestEffort.tlsFragmentDelayMs,
            tcpDesyncEnabled = prefs[BEST_EFFORT_TCP_DESYNC_ENABLED] ?: ZelenBoDefaults.bestEffort.tcpDesyncEnabled,
            tcpDesyncMode = prefs[BEST_EFFORT_TCP_DESYNC_MODE] ?: ZelenBoDefaults.bestEffort.tcpDesyncMode,
            echEnabled = prefs[BEST_EFFORT_ECH_ENABLED] ?: ZelenBoDefaults.bestEffort.echEnabled
        )

        val proxyMode = prefs[PROXY_MODE]?.let { runCatching { ProxyMode.valueOf(it) }.getOrNull() } ?: ZelenBoDefaults.proxyMode

        ZelenBoConfig(
            enabledServices = if (enabledServices.isEmpty()) setOf(ServiceId.Telegram) else enabledServices,
            dns = dns,
            bestEffort = bestEffort,
            proxyMode = proxyMode,
            optimizedDomains = if (optimizedDomains.isEmpty()) ZelenBoDefaults.optimizedDomains else optimizedDomains
        )
    }

    override suspend fun updateConfig(transform: (ZelenBoConfig) -> ZelenBoConfig) {
        val current = configFlow.first()
        val next = transform(current)

        dataStore.edit { prefs ->
            prefs[ENABLED_SERVICES] = next.enabledServices.map { it.name }.toSet()
            prefs[OPTIMIZED_DOMAINS] = next.optimizedDomains

            prefs[DNS_TRANSPORT_MODE] = next.dns.transportMode.name
            prefs[DNS_PRESET] = next.dns.preset.name
            if (next.dns.customEndpoint == null) {
                prefs.remove(DNS_CUSTOM_ENDPOINT)
            } else {
                prefs[DNS_CUSTOM_ENDPOINT] = next.dns.customEndpoint
            }

            prefs[DNS_UDP_FALLBACK_SERVER_IP] = next.dns.udpFallbackServerIp
            prefs[DNS_UDP_FALLBACK_SERVER_PORT] = next.dns.udpFallbackServerPort

            prefs[PROXY_MODE] = next.proxyMode.name

            prefs[BEST_EFFORT_TLS_FRAGMENTATION_ENABLED] = next.bestEffort.tlsFragmentationEnabled
            prefs[BEST_EFFORT_TLS_FRAGMENT_SIZE_BYTES] = next.bestEffort.tlsFragmentSizeBytes
            prefs[BEST_EFFORT_TLS_FRAGMENT_DELAY_MS] = next.bestEffort.tlsFragmentDelayMs

            prefs[BEST_EFFORT_TCP_DESYNC_ENABLED] = next.bestEffort.tcpDesyncEnabled
            prefs[BEST_EFFORT_TCP_DESYNC_MODE] = next.bestEffort.tcpDesyncMode

            prefs[BEST_EFFORT_ECH_ENABLED] = next.bestEffort.echEnabled
        }
    }
}

