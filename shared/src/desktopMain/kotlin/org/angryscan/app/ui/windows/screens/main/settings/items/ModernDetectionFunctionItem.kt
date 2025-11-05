package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.common.engine.IMatcher
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.MatcherTooltip

@Composable
fun ModernDetectionFunctionItem(
    matcher: IMatcher,
    scanSettings: org.angryscan.app.common.ScanSettings
) {
    val isChecked = scanSettings.matchers.any {
        it::class == matcher::class
    }

    val onCheckedChange = { checked: Boolean ->
        if (checked && !scanSettings.matchers.any { it::class == matcher::class })
            scanSettings.matchers.add(matcher)
        else if (!checked)
            scanSettings.matchers.removeIf { it::class == matcher::class }
        scanSettings.save()
    }

    val itemInteractionSource = remember { MutableInteractionSource() }
    val isItemHovered by itemInteractionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .hoverable(interactionSource = itemInteractionSource),
        shape = RoundedCornerShape(6.dp),
        color = if (isItemHovered)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shadowElevation = if (isItemHovered) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(14.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            )

            MatcherTooltip(
                matcher = matcher
            ) {
                Text(
                    text = matcher.composableName(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isItemHovered)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
