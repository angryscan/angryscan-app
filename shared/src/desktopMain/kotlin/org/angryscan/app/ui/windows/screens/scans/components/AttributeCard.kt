package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import org.angryscan.app.di.PreviewModule
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.MatcherTooltip
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.FullName

@Composable
fun AttributeCard(
    attribute: IMatcher,
    count: Int
) {
    MatcherTooltip(
        matcher = attribute,
        count
    ) {
        Box(
            modifier = Modifier
                .clip(
                    MaterialTheme.shapes.small
                )
                .background(color = MaterialTheme.colorScheme.secondary)
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Text(
                text = attribute.composableName(),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.05.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Composable
fun AttributeCard(attribute: IMatcher, count: Int, onClick: () -> Unit, enabled: Boolean) {
    MatcherTooltip(
        matcher = attribute,
        count = count
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
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Text(
                text = attribute.composableName(),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.05.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Preview
@Composable
fun AttributeCardPreview() {
    PreviewModule {
        AttributeCard(
            FullName,
            5
        )
    }
}
