package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.angryscan.app.scan.common.files.Location

@Composable
@Preview
fun AttributeLocationItem(
    location: Location,
    selectable: Boolean = true,
    checked: Boolean = false,
    onCheckedChanged: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
        ,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = if (isHovered) 1.dp else 0.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { onCheckedChanged(!checked) }
                )
                .padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChanged,
                    modifier = Modifier.size(40.dp),
                    colors = CheckboxDefaults.colors().copy(
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = selectable
                )
                Text(
                    text = location.entry.before,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = location.entry.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = location.entry.after,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = location.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}