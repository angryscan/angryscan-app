package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_DiscardAll
import org.angryscan.app.resources.ScanSettings_SelectAll
import org.jetbrains.compose.resources.stringResource

/**
 * Единообразная кнопка «Выбрать все» / «Сбросить все» для блоков настроек.
 * Select all — primary; Discard all — красный (error).
 * Область вокруг текста для удобного нажатия и визуального отделения.
 */
@Composable
fun SelectAllOrDiscardAllText(
    allSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 100.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    allSelected -> colorScheme.errorContainer.copy(alpha = 0.25f)
                    isHovered -> colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else -> colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (allSelected) stringResource(Res.string.ScanSettings_DiscardAll) else stringResource(Res.string.ScanSettings_SelectAll),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            color = when {
                allSelected -> colorScheme.error
                isHovered -> colorScheme.primary
                else -> colorScheme.onSurfaceVariant
            }
        )
    }
}
