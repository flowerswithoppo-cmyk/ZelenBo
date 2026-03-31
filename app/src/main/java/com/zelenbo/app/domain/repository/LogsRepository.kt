package com.zelenbo.app.domain.repository

import com.zelenbo.app.domain.model.LogEntry
import com.zelenbo.app.domain.model.LogLevel
import kotlinx.coroutines.flow.Flow

interface LogsRepository {
    fun logsFlow(level: LogLevel? = null): Flow<LogEntry>
    suspend fun append(log: LogEntry)
}

