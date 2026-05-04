package org.angryscan.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF0F4A84),
    tertiary = Color(0xFF269336),
    onSecondary = Color(0xFFF0F0F0),
    background = Color(0xFFEEF3F8),
    onBackground = Color(0xFF1A1D21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1D21),
    surfaceVariant = Color(0xFFD8E2ED),
    onSurfaceVariant = Color(0xFF37414C),
    outline = Color(0xFF8E9AA8),
    outlineVariant = Color(0xFFB4C0CD),
    // Saturated error for visible destructive affordances (window close, etc.).
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF410E0B),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF42A5F5),
    onPrimary = Color(0xFFE3F2FD),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFBBDEFB),
    background = Color(0xFF0A0E14),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF111820),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1A2330),
    onSurfaceVariant = Color(0xFFB0B8C1),
    secondaryContainer = Color(0xFF0F4A84),
    secondary = Color(0xFF5C9FD9),
    onSecondary = Color(0xFFF0F0F0),
    error = Color(0xFFCF6679),
    tertiary = Color(0xFF269336)
)