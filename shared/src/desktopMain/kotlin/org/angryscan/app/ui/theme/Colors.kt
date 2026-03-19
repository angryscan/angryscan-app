package org.angryscan.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Сбалансированная палитра: нормальный контраст и читаемость,
 * спокойный синий акцент без «режущего» или «размытого» вида.
 */
val LightColors = lightColorScheme(
    primary = Color(0xFF2563A8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E8F5),
    onPrimaryContainer = Color(0xFF0D3A66),
    secondary = Color(0xFF4E6A82),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E2EC),
    onSecondaryContainer = Color(0xFF2C4054),
    tertiary = Color(0xFF2D7A5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC5E9D8),
    onTertiaryContainer = Color(0xFF0D3D2C),
    surface = Color(0xFFF2F4F6),
    onSurface = Color(0xFF1C1E21),
    surfaceVariant = Color(0xFFE4E8EC),
    onSurfaceVariant = Color(0xFF43474C),
    outline = Color(0xFF74787D),
    outlineVariant = Color(0xFFB4B8BC),
    background = Color(0xFFF2F4F6),
    onBackground = Color(0xFF1C1E21),
    error = Color(0xFFB33D3D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF5DADA),
    onErrorContainer = Color(0xFF5C1C1C),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF7AB0E8),
    onPrimary = Color(0xFF0D3A66),
    primaryContainer = Color(0xFF1E4D7A),
    onPrimaryContainer = Color(0xFFD6E8F5),
    secondary = Color(0xFF9CB8D0),
    onSecondary = Color(0xFF2C4054),
    secondaryContainer = Color(0xFF3A5168),
    onSecondaryContainer = Color(0xFFD8E2EC),
    tertiary = Color(0xFF7BC4A4),
    onTertiary = Color(0xFF0D3D2C),
    tertiaryContainer = Color(0xFF1E5C44),
    onTertiaryContainer = Color(0xFFC5E9D8),
    surface = Color(0xFF1E2125),
    onSurface = Color(0xFFE4E6E8),
    surfaceVariant = Color(0xFF2C3035),
    onSurfaceVariant = Color(0xFFB4B8BC),
    outline = Color(0xFF8A8E92),
    outlineVariant = Color(0xFF43474C),
    background = Color(0xFF181B1F),
    onBackground = Color(0xFFE4E6E8),
    error = Color(0xFFE88A8A),
    onError = Color(0xFF5C1C1C),
    errorContainer = Color(0xFF7A2828),
    onErrorContainer = Color(0xFFF5DADA),
)