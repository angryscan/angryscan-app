package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ScanFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color = LocalContentColor.current
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(
            text = text,
            modifier = Modifier
                .background(Color.Transparent)
        ) },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Done,
                    contentDescription = null,
                    modifier = Modifier
                        .size(FilterChipDefaults.IconSize)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(FilterChipDefaults.IconSize),
                    tint = tint
                )
            }
        }
    )
}