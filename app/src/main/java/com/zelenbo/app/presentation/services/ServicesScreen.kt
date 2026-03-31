package com.zelenbo.app.presentation.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelenbo.app.R
import com.zelenbo.app.presentation.navigation.AppRoute
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.presentation.theme.GreenPrimary
import com.zelenbo.app.presentation.theme.TextPrimary
import com.zelenbo.app.presentation.theme.TextSecondary
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource

@Composable
fun ServicesScreen(
    onNavigate: (AppRoute) -> Unit,
    viewModel: ServicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customDomainInput by viewModel.customDomainInput.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.services_title),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = { onNavigate(AppRoute.Dashboard) }) {
                Text(text = "Назад", color = GreenPrimary)
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ServiceToggleRow(
                    label = "Telegram",
                    checked = uiState.enabledServices.contains(ServiceId.Telegram),
                    onCheckedChange = { viewModel.toggleService(ServiceId.Telegram, it) }
                )
                ServiceToggleRow(
                    label = "YouTube",
                    checked = uiState.enabledServices.contains(ServiceId.YouTube),
                    onCheckedChange = { viewModel.toggleService(ServiceId.YouTube, it) }
                )
                ServiceToggleRow(
                    label = "Discord",
                    checked = uiState.enabledServices.contains(ServiceId.Discord),
                    onCheckedChange = { viewModel.toggleService(ServiceId.Discord, it) }
                )
                ServiceToggleRow(
                    label = "Instagram",
                    checked = uiState.enabledServices.contains(ServiceId.Instagram),
                    onCheckedChange = { viewModel.toggleService(ServiceId.Instagram, it) }
                )
                ServiceToggleRow(
                    label = "Twitter/X",
                    checked = uiState.enabledServices.contains(ServiceId.TwitterX),
                    onCheckedChange = { viewModel.toggleService(ServiceId.TwitterX, it) }
                )
            }
        }

        Spacer(modifier = Modifier.padding(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_dns_allowlist_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.padding(8.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = customDomainInput,
                    onValueChange = { viewModel.setCustomDomainInput(it) },
                    label = { Text(stringResource(id = R.string.settings_dns_allowlist_add_hint), color = TextSecondary) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Button(
                    onClick = { viewModel.addCustomDomain(customDomainInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Добавить")
                }
            }
        }

        Spacer(modifier = Modifier.padding(12.dp))

        Text(
            text = "Домены (optimized):",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val domains = uiState.optimizedDomains.toList().sorted()
            items(domains.size) { index ->
                val domain = domains[index]
                DomainRow(
                    domain = domain,
                    onRemove = { viewModel.removeDomain(domain) }
                )
            }
        }
    }
}

@Composable
private fun ServiceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DomainRow(domain: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = domain,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRemove) {
            Text(text = "Удалить", color = GreenPrimary)
        }
    }
}

