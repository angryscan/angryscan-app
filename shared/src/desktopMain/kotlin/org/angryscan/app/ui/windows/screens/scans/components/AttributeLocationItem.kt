package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.extensions.isMaskable

@Composable
@Preview
fun AttributeLocationItem(
    location: Location,
    maskingSupported: Boolean,
    onMask: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource),
        shape = RoundedCornerShape(6.dp),
        color = if (isHovered)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        else
            Color.Transparent,
        shadowElevation = if (isHovered) 1.dp else 0.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            val canMask = maskingSupported && location.isMaskable()
            if (canMask) {
                val maskInteraction = remember { MutableInteractionSource() }
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = "Mask",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = maskInteraction,
                            indication = ripple(
                                bounded = false,
                                radius = 12.dp
                            )
                        ) { onMask() }
                )
            }
        }
    }
}