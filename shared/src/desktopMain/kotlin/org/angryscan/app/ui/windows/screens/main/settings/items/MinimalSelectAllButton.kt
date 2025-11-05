package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_SelectAll

@Composable
fun MinimalSelectAllButton(
    scanSettings: ScanSettings
) {
    val matchers = remember { scanSettings.matchers }
    val isAllSelected = MatchersRegister.all { m ->
        matchers.any { m::class == it::class }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isAllSelected) {
                    scanSettings.matchers.clear()
                } else {
                    scanSettings.matchers.addAll(MatchersRegister.filter { m ->
                        !scanSettings.matchers.any { it::class == m::class }
                    })
                }
                scanSettings.save()
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isHovered)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shadowElevation = if (isHovered) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isHovered)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                modifier = Modifier.size(24.dp)
            ) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            scanSettings.matchers.addAll(MatchersRegister.filter { m ->
                                !scanSettings.matchers.any { m::class == it::class }
                            })
                        } else {
                            scanSettings.matchers.clear()
                        }
                        scanSettings.save()
                    },
                    modifier = Modifier.size(18.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                )
            }

            Text(
                text = stringResource(Res.string.ScanSettings_SelectAll),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHovered)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}