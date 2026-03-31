package com.zelenbo.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun ZelenBoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // We keep system dark flag for future extension, but default is always ZelenBo dark.
    val useDark = if (darkTheme) true else !isSystemInDarkTheme()

    val colorScheme = darkColorScheme(
        primary = GreenPrimary,
        secondary = GreenLight,
        background = DarkBackground,
        surface = DarkSurface,
        error = ErrorRed
    )

    val shapes = Shapes(
        small = ZBCardShape,
        medium = ZBCardShape,
        large = ZBCardShape
    )

    MaterialTheme(
        colorScheme = if (useDark) colorScheme else colorScheme,
        typography = ZelenBoTypography,
        shapes = shapes,
        content = content
    )
}

