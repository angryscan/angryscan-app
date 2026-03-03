package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun PathHint(
    hint: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = hint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(top = 6.dp, start = 4.dp)
    )
}

@Composable
fun MainScreenCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .widthIn(max = 700.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

@Composable
fun ScanButtonModifier(
    isReady: Boolean,
    modifier: Modifier
): Modifier = modifier

/** Лёгкая подсветка при наведении (как у вкладки навигации). */
fun Modifier.scanButtonHoverFeedback(enabled: Boolean): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cs = MaterialTheme.colorScheme
    this
        .hoverable(interactionSource = interactionSource)
        .then(
            if (enabled && isHovered)
                Modifier.background(cs.primary.copy(alpha = 0.15f), scanButtonChipShape)
            else Modifier
        )
}

// Кнопка «Start scan» — те же цвета, что выделение текущего экрана в верхнем меню (primary / onPrimary).
private val scanButtonChipShape = RoundedCornerShape(20.dp)

@Composable
fun startScanButtonColors(): ButtonColors {
    val cs = MaterialTheme.colorScheme
    return ButtonDefaults.buttonColors(
        containerColor = cs.primary,
        contentColor = cs.onPrimary,
        disabledContainerColor = cs.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

/** Обводка в тон primary, как у выбранной вкладки навигации. */
@Composable
fun Modifier.scanButtonChipBorder(): Modifier {
    val cs = MaterialTheme.colorScheme
    return this
        .clip(scanButtonChipShape)
        .border(
            width = 1.dp,
            color = cs.primary.copy(alpha = 0.7f),
            shape = scanButtonChipShape
        )
}

/** Контент кнопки «Start scan»: только подпись, без иконки (современный CTA по Material / best practices). */
@Composable
fun StartScanButtonContent() {
    Text(
        text = stringResource(Res.string.MainScreen_ScanStartButton),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
    )
}
