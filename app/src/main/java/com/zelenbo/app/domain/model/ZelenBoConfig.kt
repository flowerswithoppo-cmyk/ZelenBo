package com.zelenbo.app.domain.model

data class ZelenBoConfig(
    val enabledServices: Set<ServiceId>,
    val dns: DnsConfig,
    val bestEffort: BestEffortConfig,
    val proxyMode: ProxyMode,
    // Domains for which we apply DNS optimization.
    val optimizedDomains: Set<String>
)

