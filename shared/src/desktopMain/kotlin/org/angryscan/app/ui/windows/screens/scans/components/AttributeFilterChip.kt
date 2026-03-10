package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val chipShape = RoundedCornerShape(20.dp)

@Composable
fun AttributeFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val colorScheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .height(28.dp)
            .clip(chipShape),
        shape = chipShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f),
            labelColor = colorScheme.onSurfaceVariant,
            iconColor = tint,
            selectedContainerColor = colorScheme.primary.copy(alpha = 0.22f),
            selectedLabelColor = colorScheme.primary,
            selectedLeadingIconColor = colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colorScheme.outlineVariant.copy(alpha = 0.5f),
            selectedBorderColor = colorScheme.primary.copy(alpha = 0.6f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        ),
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (selected) "Selected" else "Not selected",
                modifier = Modifier.size(16.dp),
                tint = if (selected) colorScheme.primary else tint
            )
        }
    )
}
