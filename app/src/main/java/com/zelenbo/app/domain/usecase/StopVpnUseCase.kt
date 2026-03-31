package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.repository.VpnGateway
import javax.inject.Inject

class StopVpnUseCase @Inject constructor(
    private val vpnGateway: VpnGateway
) {
    suspend operator fun invoke() {
        vpnGateway.stop()
    }
}

