package com.zelenbo.app.presentation.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.presentation.navigation.AppRoute
import com.zelenbo.app.presentation.theme.GreenPrimary
import com.zelenbo.app.presentation.theme.GreenGlow
import com.zelenbo.app.presentation.theme.TextPrimary
import com.zelenbo.app.presentation.theme.TextSecondary
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.zelenbo.app.R

@Composable
fun DashboardScreen(
    onNavigate: (AppRoute) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val connectedColor = if (uiState.vpnState.isConnected) GreenPrimary else TextSecondary
    val statusText = uiState.vpnState.statusText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_title),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.status_hint),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = statusText,
                    color = connectedColor,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Telegram",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = uiState.config.enabledServices.contains(ServiceId.Telegram),
                        onCheckedChange = { enabled -> viewModel.setTelegramEnabled(enabled) }
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(GreenGlow.copy(alpha = pulseAlpha))
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
                    .clickable(enabled = !uiState.isBusy) { viewModel.onToggleMain() }
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .border(
                        BorderStroke(2.dp, GreenPrimary),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (uiState.vpnState.isConnected) {
                            stringResource(id = R.string.vpn_stop)
                        } else {
                            stringResource(id = R.string.vpn_start)
                        },
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (uiState.isBusy) "..." else "ZB",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { onNavigate(AppRoute.Services) }) {
                Text(text = "Сервисы", color = GreenPrimary)
            }
            TextButton(onClick = { onNavigate(AppRoute.Settings) }) {
                Text(text = "Настройки", color = GreenPrimary)
            }
            TextButton(onClick = { onNavigate(AppRoute.Logs) }) {
                Text(text = "Логи", color = GreenPrimary)
            }
            TextButton(onClick = { onNavigate(AppRoute.About) }) {
                Text(text = "О приложении", color = GreenPrimary)
            }
        }
    }
}

