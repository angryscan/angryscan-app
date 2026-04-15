package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun WindowControlButtons(
    windowPlacement: androidx.compose.ui.window.WindowPlacement?,
    expanded: Boolean,
    onMinimizeClick: () -> Unit,
    onExpandClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WindowControlButton(
            onClick = onMinimizeClick,
            icon = Icons.Outlined.Minimize,
            contentDescription = "Minimize",
        )

        WindowControlButton(
            onClick = onExpandClick,
            icon = if (expanded) Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull,
            contentDescription = if (expanded) "Restore" else "Maximize",
        )

        WindowControlButton(
            onClick = onCloseClick,
            icon = Icons.Outlined.Close,
            contentDescription = "Close",
            isCloseAction = true
        )
    }
}

