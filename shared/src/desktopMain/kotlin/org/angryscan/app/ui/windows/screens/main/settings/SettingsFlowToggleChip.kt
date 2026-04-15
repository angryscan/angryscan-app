package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Плотные чипы для FlowRow — те же цвета/обводка, что QuickFilterChip в TopNavigation
 * (мягкий primary, outlineVariant без «грязного» контура).
 */
@Composable
internal fun SettingsFlowToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    chipCorner: Dp,
    chipPadH: Dp,
    chipLabelFs: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(chipCorner)

    val fill = when {
        selected && hovered -> cs.primary.copy(alpha = 0.20f)
        selected -> cs.primary.copy(alpha = 0.16f)
        hovered -> cs.primary.copy(alpha = 0.10f)
        else -> cs.surfaceVariant.copy(alpha = 0.24f)
    }
    val stroke = when {
        selected -> cs.primary.copy(alpha = 0.28f)
        hovered -> cs.primary.copy(alpha = 0.28f)
        else -> cs.outlineVariant.copy(alpha = 0.26f)
    }
    val labelColor = when {
        selected || hovered -> cs.primary
        else -> cs.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .hoverable(interactionSource = interaction),
        shape = shape,
        color = fill,
        border = BorderStroke(1.dp, stroke),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = chipPadH, vertical = 0.dp),
            style = MaterialTheme.typography.labelMedium,
            fontSize = chipLabelFs,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = labelColor,
        )
    }
}
