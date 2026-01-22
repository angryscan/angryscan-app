package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import androidx.compose.ui.unit.dp
import org.angryscan.app.resources.LocationWindow_MaskNotAvailable
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.jetbrains.compose.resources.stringResource

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
            .fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = if (isHovered) 1.dp else 0.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(
                    onClick = { onCheckedChanged(!checked) }
                )
                .padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (location.isMaskable) {
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
                } else {
                    DescriptionTooltip(
                        description = stringResource(Res.string.LocationWindow_MaskNotAvailable)
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = null,
                            modifier = Modifier.size(40.dp),
                            colors = CheckboxDefaults.colors().copy(
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                                uncheckedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = false
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = location.entry.before.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = location.entry.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = location.entry.after.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
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