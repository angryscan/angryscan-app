package org.angryscan.app.ui.windows.screens.settings.components

import androidx.compose.runtime.Composable

@Composable
fun SettingsButton(
    onClick: () -> Unit,
    text: String
) {
    SettingsSelector(
        onClick = onClick,
        text = text,
        selected = false
    )
}