package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
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

/** В тёмной теме добавляет заметную подсветку кнопки при наведении. */
fun Modifier.scanButtonHoverFeedback(enabled: Boolean): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val s = MaterialTheme.colorScheme.surface
    val isDark = (s.red + s.green + s.blue) / 3f < 0.25f
    val shape = RoundedCornerShape(20.dp)
    this
        .hoverable(interactionSource = interactionSource)
        .then(
            if (enabled && isDark && isHovered)
                Modifier.background(Color.White.copy(alpha = 0.18f), shape)
            else Modifier
        )
}

// Цвета кнопки «Start scan»: светлая тема — мягкий светло-голубой; тёмная — глубокий синий без яркого пятна.
private val ScanButtonLightBg = Color(0xFFBBDEFB)
private val ScanButtonLightFg = Color(0xFF0D47A1)
private val ScanButtonDarkBg = Color(0xFF1D4ED8)
private val ScanButtonDarkFg = Color(0xFFE0E7FF)

@Composable
fun startScanButtonColors() = ButtonDefaults.buttonColors(
    containerColor = if (isDarkTheme()) ScanButtonDarkBg else ScanButtonLightBg,
    contentColor = if (isDarkTheme()) ScanButtonDarkFg else ScanButtonLightFg,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
)

@Composable
private fun isDarkTheme(): Boolean {
    val s = MaterialTheme.colorScheme.surface
    return (s.red + s.green + s.blue) / 3f < 0.25f
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
