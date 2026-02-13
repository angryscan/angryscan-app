package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

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
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0.85f,
        animationSpec = tween(300),
        label = "scanReady"
    )
    return modifier.alpha(alpha)
}
