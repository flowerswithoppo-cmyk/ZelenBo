package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<ZelenBoConfig> = settingsRepository.configFlow
}

