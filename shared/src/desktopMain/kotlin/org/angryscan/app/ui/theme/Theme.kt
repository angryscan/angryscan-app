package org.angryscan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.koin.compose.koinInject
import org.angryscan.app.common.AppSettings


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val appSettings = koinInject<AppSettings>()
    val theme by remember { appSettings.theme }
    val isDarkTheme = (theme == AppSettings.ThemeType.Dark || (theme == AppSettings.ThemeType.System && isSystemInDarkTheme()))
    val colors = if (isDarkTheme) {
        DarkColors
    } else {
        LightColors
    }

    val appRippleConfiguration =
        RippleConfiguration(
            color = if(isDarkTheme) Color.LightGray else Color.DarkGray,
            rippleAlpha = RippleAlpha(focusedAlpha = 0.1f, hoveredAlpha = 0.1f, pressedAlpha = 0.25f, draggedAlpha = 0.25f),
        )

    CompositionLocalProvider(LocalRippleConfiguration provides appRippleConfiguration) {
        MaterialTheme(
            colorScheme = colors,
            content = content,
            shapes = AppShapes
        )
    }
}
