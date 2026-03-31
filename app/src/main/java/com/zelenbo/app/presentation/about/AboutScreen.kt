package com.zelenbo.app.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zelenbo.app.R
import com.zelenbo.app.presentation.navigation.AppRoute
import com.zelenbo.app.presentation.theme.GreenPrimary
import com.zelenbo.app.presentation.theme.TextPrimary
import com.zelenbo.app.presentation.theme.TextSecondary

@Composable
fun AboutScreen(onNavigate: (AppRoute) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.about_title),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "ZelenBo v0.1.0", color = GreenPrimary, style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(id = R.string.about_description), color = TextPrimary)
                Text(text = "GitHub: https://github.com/your-org/zelenbo", color = TextSecondary)
                Text(text = "Лицензия: MIT", color = TextSecondary)
            }
        }

        TextButton(onClick = { onNavigate(AppRoute.Dashboard) }) {
            Text(text = "Назад", color = GreenPrimary)
        }
    }
}

