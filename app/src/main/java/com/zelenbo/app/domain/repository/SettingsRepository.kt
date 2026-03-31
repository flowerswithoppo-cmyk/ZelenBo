package com.zelenbo.app.domain.repository

import com.zelenbo.app.domain.model.ZelenBoConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val configFlow: Flow<ZelenBoConfig>

    suspend fun updateConfig(transform: (ZelenBoConfig) -> ZelenBoConfig)
}

