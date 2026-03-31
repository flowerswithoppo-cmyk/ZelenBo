package com.zelenbo.app.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelenbo.app.domain.model.LogEntry
import com.zelenbo.app.domain.model.LogLevel
import com.zelenbo.app.domain.usecase.ObserveLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val observeLogsUseCase: ObserveLogsUseCase
) : ViewModel() {

    private val _filterLevel = MutableStateFlow<LogLevel?>(null)
    val filterLevel: StateFlow<LogLevel?> = _filterLevel.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val maxLogs = 800

    init {
        viewModelScope.launch {
            _filterLevel
                .distinctUntilChanged()
                .flatMapLatest { level ->
                    observeLogsUseCase(level)
                }
                .onEach { entry ->
                    _logs.update { current ->
                        val next = current + entry
                        if (next.size > maxLogs) next.takeLast(maxLogs) else next
                    }
                }
                .collect()
        }
    }

    fun setFilter(level: LogLevel?) {
        _filterLevel.value = level
    }
}

