package org.angryscan.app.ui.windows.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val cardShape = RoundedCornerShape(16.dp)

@Composable
fun SettingsRow(
    title: String,
    block: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = cardShape
            ),
        shape = cardShape,
        color = colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                block()
            }
        }
    }
}