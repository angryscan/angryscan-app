package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.common.engine.IMatcher
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.MatcherTooltip

@Composable
fun AttributeCard(attribute: IMatcher) {
    MatcherTooltip(
        matcher = attribute
    ) {
        Box(
            modifier = Modifier
                .clip(
                    MaterialTheme.shapes.small
                )
                .background(color = MaterialTheme.colorScheme.secondary)
                .padding(4.dp)
        ) {
            Text(
                text = attribute.composableName(),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Composable
fun AttributeCard(attribute: IMatcher, onClick: () -> Unit, enabled: Boolean) {
    MatcherTooltip(
        matcher = attribute
    ) {
        Box(
            modifier = Modifier
                .clip(
                    MaterialTheme.shapes.small
                )
                .background(
                    color = if (enabled)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.outlineVariant
                )
                .clickable(
                    onClick = onClick,
                    enabled = enabled
                )
                .padding(4.dp)
        ) {
            Text(
                text = attribute.composableName(),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}