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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.Res
import org.angryscan.app.ui.windows.screens.main.LocalMainScreenAdaptiveTokens
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
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    val shape = RoundedCornerShape(adaptiveScale.dp(24.dp, min = 20.dp, max = 32.dp))
    Column(
        modifier = modifier
            .widthIn(max = adaptiveScale.dp(700.dp, min = 700.dp, max = 980.dp))
            .fillMaxWidth()
            .padding(
                horizontal = adaptiveScale.dp(20.dp, min = 16.dp, max = 28.dp),
                vertical = adaptiveScale.dp(20.dp, min = 16.dp, max = 28.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = shape
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = shape
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
    this
        .hoverable(interactionSource = interactionSource)
        .then(
            if (enabled && isHovered)
                Modifier.background(SourceActionBlue.copy(alpha = 0.15f), scanButtonChipShape)
            else Modifier
        )
}

// Кнопка «Start scan» — те же цвета, что выделение текущего экрана в верхнем меню (primary / onPrimary).
private val scanButtonChipShape = RoundedCornerShape(20.dp)
val SourceActionBlue = Color(0xFF42A5F5)
val SourceActionOnBlue = Color(0xFFE3F2FD)

@Composable
fun startScanButtonColors(): ButtonColors {
    val cs = MaterialTheme.colorScheme
    return ButtonDefaults.buttonColors(
        containerColor = SourceActionBlue,
        contentColor = SourceActionOnBlue,
        disabledContainerColor = cs.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

@Composable
fun sourceActionFilledTonalButtonColors(): ButtonColors {
    val cs = MaterialTheme.colorScheme
    return ButtonDefaults.filledTonalButtonColors(
        containerColor = SourceActionBlue,
        contentColor = SourceActionOnBlue,
        disabledContainerColor = cs.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

/** Обводка в тон primary, как у выбранной вкладки навигации. */
@Composable
fun Modifier.scanButtonChipBorder(): Modifier {
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    val shape = RoundedCornerShape(adaptiveScale.dp(20.dp, min = 18.dp, max = 26.dp))
    return this
        .clip(shape)
        .border(
            width = adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp),
            color = SourceActionBlue.copy(alpha = 0.75f),
            shape = shape
        )
}

/** Контент кнопки «Start scan»: только подпись, без иконки (современный CTA по Material / best practices). */
@Composable
fun StartScanButtonContent() {
    Text(
        text = stringResource(Res.string.MainScreen_ScanStartButton),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
