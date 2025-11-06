package org.angryscan.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF1E88E5),
    secondary = Color(0xFF0F4A84),
    tertiary = Color(0xFF269336),
    onSecondary = Color(0xFFF0F0F0),
    surface = Color(0xFFaabfe6),
    surfaceVariant = Color(0xFFaed0fc),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF1E88E5),
    background = Color(0xFF000713),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF000D23),
    surfaceVariant = Color(0xFF031936),
    secondaryContainer = Color(0xFF0F4A84),
    secondary = Color(0xFF0F4A84),
    onPrimary = Color(0xFFF0F0F0),
    onSecondary = Color(0xFFF0F0F0),
    error = Color(0xFFA01010),
    tertiary = Color(0xFF269336)
)