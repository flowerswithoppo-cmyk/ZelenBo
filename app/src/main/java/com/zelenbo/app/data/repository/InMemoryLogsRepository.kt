package com.zelenbo.app.data.repository

import com.zelenbo.app.domain.model.LogEntry
import com.zelenbo.app.domain.model.LogLevel
import com.zelenbo.app.domain.repository.LogsRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

class InMemoryLogsRepository @Inject constructor() : LogsRepository {

    private val bus = MutableSharedFlow<LogEntry>(
        replay = 0,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun logsFlow(level: LogLevel?): Flow<LogEntry> {
        val flow = bus.asSharedFlow()
        return if (level == null) flow else flow.filter { it.level == level }
    }

    override suspend fun append(log: LogEntry) {
        bus.tryEmit(log)
    }
}

