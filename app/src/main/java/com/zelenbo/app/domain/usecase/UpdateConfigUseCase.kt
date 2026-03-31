package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.repository.SettingsRepository
import com.zelenbo.app.domain.model.ZelenBoConfig
import javax.inject.Inject

class UpdateConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(transform: (ZelenBoConfig) -> ZelenBoConfig) {
        settingsRepository.updateConfig(transform)
    }
}

