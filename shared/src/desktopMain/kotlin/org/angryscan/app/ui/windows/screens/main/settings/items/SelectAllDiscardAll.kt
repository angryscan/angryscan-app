package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
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
    modifier: Modifier = Modifier,
    /** Узкая зона нажатия: сразу после текста заголовка (без «кнопочных» полей). */
    dense: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colorScheme = MaterialTheme.colorScheme

    val label = if (allSelected) {
        stringResource(Res.string.ScanSettings_DiscardAll)
    } else {
        stringResource(Res.string.ScanSettings_SelectAll)
    }

    Text(
        text = label,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (dense) 0.dp else 2.dp,
                vertical = 2.dp
            ),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        textDecoration = if (isHovered) TextDecoration.Underline else TextDecoration.None,
        color = when {
            allSelected -> colorScheme.error.copy(alpha = if (isHovered) 0.95f else 0.8f)
            isHovered -> colorScheme.primary.copy(alpha = 0.95f)
            else -> colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        }
    )
}
