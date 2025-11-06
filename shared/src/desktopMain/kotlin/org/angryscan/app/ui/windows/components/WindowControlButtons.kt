package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        WindowControlButton(
            onClick = onMinimizeClick,
            icon = Icons.Outlined.Minimize,
            contentDescription = "Minimize",
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )

        WindowControlButton(
            onClick = onExpandClick,
            icon = if (expanded) Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull,
            contentDescription = if (expanded) "Restore" else "Maximize",
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )

        WindowControlButton(
            onClick = onCloseClick,
            icon = Icons.Outlined.Close,
            contentDescription = "Close",
            backgroundColor = MaterialTheme.colorScheme.errorContainer
        )
    }
}

