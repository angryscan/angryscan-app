package org.angryscan.app.di

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import org.angryscan.app.ui.theme.AppShapes
import org.angryscan.app.ui.theme.DarkColors
import org.angryscan.app.ui.theme.LightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModule(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {

    val colors = if (isDarkTheme) {
        DarkColors
    } else {
        LightColors
    }

    val appRippleConfiguration =
        RippleConfiguration(
            color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
            rippleAlpha = RippleAlpha(
                focusedAlpha = 0.1f,
                hoveredAlpha = 0.1f,
                pressedAlpha = 0.25f,
                draggedAlpha = 0.25f
            ),
        )

    CompositionLocalProvider(LocalRippleConfiguration provides appRippleConfiguration) {
        MaterialTheme(
            colorScheme = colors,
            content = content,
            shapes = AppShapes
        )
    }
}