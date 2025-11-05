package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SettingsButton(
    transition: Transition<Boolean>,
    onClick: () -> Unit
) {
    val color by transition.animateColor(label = "sbColor") { state ->
        if (state) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.secondary
        }
    }

    val shape by transition.animateDp(label = "sbShape") { state ->
        if (state) {
            0.dp
        } else {
            16.dp
        }
    }

    val height by transition.animateDp(label = "sbHeight") { state ->
        if (state) {
            70.dp
        } else {
            56.dp
        }
    }

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(height)
            .clip(
                MaterialTheme.shapes.medium.copy(
                    topStart = CornerSize(0.dp),
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(shape)
                )
            )
            .background(color),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color)
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (transition.targetState)
                    Icons.Outlined.Close
                else
                    Icons.Outlined.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .size(24.dp)
            )
        }
    }
}