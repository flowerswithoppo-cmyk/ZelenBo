package com.zelenbo.app.domain.model

data class DnsConfig(
    val transportMode: DnsTransportMode,
    val preset: DnsPreset,
    val customEndpoint: String?,
    // Used when transportMode=SystemUdp or as a fallback for non-optimized domains.
    val udpFallbackServerIp: String,
    val udpFallbackServerPort: Int = 53
)

