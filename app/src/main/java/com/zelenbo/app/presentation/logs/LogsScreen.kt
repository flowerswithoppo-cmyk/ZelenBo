package com.zelenbo.app.presentation.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelenbo.app.R
import com.zelenbo.app.domain.model.LogEntry
import com.zelenbo.app.domain.model.LogLevel
import com.zelenbo.app.presentation.theme.GreenPrimary
import com.zelenbo.app.presentation.theme.TextPrimary
import com.zelenbo.app.presentation.theme.TextSecondary
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.zelenbo.app.presentation.navigation.AppRoute

@Composable
fun LogsScreen(
    onNavigate: (AppRoute) -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val filter by viewModel.filterLevel.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(index = logs.lastIndex)
        }
    }

    val sdf = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.logs_title),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
        TextButton(onClick = { onNavigate(AppRoute.Dashboard) }) {
            Text(text = "Назад", color = GreenPrimary)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { viewModel.setFilter(null) }) {
                    Text(
                        text = "All",
                        color = if (filter == null) GreenPrimary else TextSecondary
                    )
                }
                TextButton(onClick = { viewModel.setFilter(LogLevel.Info) }) {
                    Text(
                        text = stringResource(id = R.string.logs_filter_info),
                        color = if (filter == LogLevel.Info) GreenPrimary else TextSecondary
                    )
                }
                TextButton(onClick = { viewModel.setFilter(LogLevel.Warning) }) {
                    Text(
                        text = stringResource(id = R.string.logs_filter_warning),
                        color = if (filter == LogLevel.Warning) GreenPrimary else TextSecondary
                    )
                }
                TextButton(onClick = { viewModel.setFilter(LogLevel.Error) }) {
                    Text(
                        text = stringResource(id = R.string.logs_filter_error),
                        color = if (filter == LogLevel.Error) GreenPrimary else TextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { entry ->
                    TerminalLogRow(entry = entry, sdf = sdf)
                }
            }
        }
    }
}

@Composable
private fun TerminalLogRow(entry: LogEntry, sdf: SimpleDateFormat) {
    val color = when (entry.level) {
        LogLevel.Info -> GreenPrimary
        LogLevel.Warning -> TextSecondary
        LogLevel.Error -> androidx.compose.ui.graphics.Color(0xFFFF5252)
    }

    Text(
        text = "${entry.tag} [${entry.level}] ${sdf.format(Date(entry.timestampMillis))}: ${entry.message}",
        color = color,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

