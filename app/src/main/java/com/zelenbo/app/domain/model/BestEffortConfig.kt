package com.zelenbo.app.domain.model

data class BestEffortConfig(
    val tlsFragmentationEnabled: Boolean,
    val tlsFragmentSizeBytes: Int, // 1..64
    val tlsFragmentDelayMs: Int, // 0..500

    val tcpDesyncEnabled: Boolean,
    val tcpDesyncMode: String, // split/disorder/fake/oob (best-effort at app level)

    val echEnabled: Boolean
)

