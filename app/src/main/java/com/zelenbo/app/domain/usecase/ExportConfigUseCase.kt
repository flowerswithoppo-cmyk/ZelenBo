package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.model.ZelenBoConfig
import javax.inject.Inject

class ExportConfigUseCase @Inject constructor() {
    operator fun invoke(config: ZelenBoConfig): String = ConfigCodec.encode(config)
}

