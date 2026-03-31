package com.zelenbo.app.domain.usecase

import com.zelenbo.app.domain.model.LogEntry
import com.zelenbo.app.domain.model.LogLevel
import com.zelenbo.app.domain.repository.LogsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLogsUseCase @Inject constructor(
    private val logsRepository: LogsRepository
) {
    operator fun invoke(level: LogLevel?): Flow<LogEntry> = logsRepository.logsFlow(level)
}

