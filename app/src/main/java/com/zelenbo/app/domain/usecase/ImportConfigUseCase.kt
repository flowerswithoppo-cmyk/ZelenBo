package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.model.ZelenBoConfig
import javax.inject.Inject

class ImportConfigUseCase @Inject constructor() {
    operator fun invoke(encoded: String): Result<ZelenBoConfig> = ConfigCodec.decode(encoded)
}

