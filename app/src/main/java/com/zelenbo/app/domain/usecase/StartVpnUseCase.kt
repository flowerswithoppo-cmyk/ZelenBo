package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.model.ZelenBoConfig
import com.zelenbo.app.domain.repository.SettingsRepository
import com.zelenbo.app.domain.repository.VpnGateway
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StartVpnUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val vpnGateway: VpnGateway
) {
    suspend operator fun invoke() {
        val config: ZelenBoConfig = settingsRepository.configFlow.first()
        vpnGateway.start(config)
    }
}

