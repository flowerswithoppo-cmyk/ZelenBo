package com.zelenbo.app.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelenbo.app.R
import com.zelenbo.app.domain.model.DnsPreset
import com.zelenbo.app.domain.model.DnsTransportMode
import com.zelenbo.app.domain.model.ProxyMode
import com.zelenbo.app.presentation.navigation.AppRoute
import com.zelenbo.app.presentation.theme.GreenPrimary
import com.zelenbo.app.presentation.theme.TextPrimary
import com.zelenbo.app.presentation.theme.TextSecondary

@Composable
fun SettingsScreen(
    onNavigate: (AppRoute) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val customDomainInput by viewModel.customDomainInput.collectAsState()
    val importEncoded by viewModel.importEncoded.collectAsState()
    val exportEncoded by viewModel.exportEncoded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.settings_title),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = { onNavigate(AppRoute.Dashboard) }) {
                Text(text = "Назад", color = GreenPrimary)
            }
        }

        SettingsCard(title = stringResource(id = R.string.settings_dns_title)) {
            EnumSelector(
                label = stringResource(id = R.string.settings_dns_upstream),
                selected = config.dns.preset.name,
                options = DnsPreset.entries.map { it.name },
                onSelected = { name -> viewModel.setDnsPreset(DnsPreset.valueOf(name)) }
            )
            Spacer(Modifier.height(8.dp))
            EnumSelector(
                label = "DNS transport",
                selected = config.dns.transportMode.name,
                options = DnsTransportMode.entries.map { it.name },
                onSelected = { name -> viewModel.setDnsTransportMode(DnsTransportMode.valueOf(name)) }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = config.dns.customEndpoint.orEmpty(),
                onValueChange = { viewModel.setDnsCustomEndpoint(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.settings_dns_custom_endpoint)) }
            )
        }

        SettingsCard(title = stringResource(id = R.string.settings_best_effort_title)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.settings_tls_fragmentation), color = TextPrimary)
                Switch(
                    checked = config.bestEffort.tlsFragmentationEnabled,
                    onCheckedChange = { viewModel.setTlsFragmentationEnabled(it) }
                )
            }
            Text("Размер фрагмента: ${config.bestEffort.tlsFragmentSizeBytes}", color = TextSecondary)
            Slider(
                value = config.bestEffort.tlsFragmentSizeBytes.toFloat(),
                onValueChange = { viewModel.setTlsFragmentSizeBytes(it.toInt()) },
                valueRange = 1f..64f
            )
            Text("Задержка: ${config.bestEffort.tlsFragmentDelayMs} ms", color = TextSecondary)
            Slider(
                value = config.bestEffort.tlsFragmentDelayMs.toFloat(),
                onValueChange = { viewModel.setTlsFragmentDelayMs(it.toInt()) },
                valueRange = 0f..500f
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.settings_tcp_desync), color = TextPrimary)
                Switch(
                    checked = config.bestEffort.tcpDesyncEnabled,
                    onCheckedChange = { viewModel.setTcpDesyncEnabled(it) }
                )
            }
            EnumSelector(
                label = "TCP desync mode",
                selected = config.bestEffort.tcpDesyncMode,
                options = listOf("split", "disorder", "fake", "oob"),
                onSelected = { viewModel.setTcpDesyncMode(it) }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.settings_ech), color = TextPrimary)
                Switch(
                    checked = config.bestEffort.echEnabled,
                    onCheckedChange = { viewModel.setEchEnabled(it) }
                )
            }
        }

        SettingsCard(title = stringResource(id = R.string.settings_transport_title)) {
            EnumSelector(
                label = stringResource(id = R.string.settings_transport_proxy_mode),
                selected = config.proxyMode.name,
                options = ProxyMode.entries.map { it.name },
                onSelected = { name -> viewModel.setProxyMode(ProxyMode.valueOf(name)) }
            )
        }

        SettingsCard(title = stringResource(id = R.string.settings_dns_allowlist_title)) {
            OutlinedTextField(
                value = customDomainInput,
                onValueChange = { viewModel.setCustomDomainInput(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.settings_dns_allowlist_add_hint)) }
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.addCustomDomain(customDomainInput) }, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить домен")
            }
        }

        SettingsCard(title = "Экспорт / Импорт") {
            Button(onClick = { viewModel.exportNow() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.action_export))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = exportEncoded,
                onValueChange = { viewModel.setExportEncoded(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Export string") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = importEncoded,
                onValueChange = { viewModel.setImportEncoded(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Import string") }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val result = viewModel.importNow()
                    Toast.makeText(
                        context,
                        if (result.isSuccess) context.getString(R.string.toast_import_success) else context.getString(R.string.toast_import_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text(stringResource(id = R.string.action_import))
                }
                TextButton(onClick = { viewModel.resetDefaults() }) {
                    Text(stringResource(id = R.string.action_reset), color = GreenPrimary)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun EnumSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { expanded = true }) {
            Text(text = selected, color = GreenPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (opt in options) {
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

