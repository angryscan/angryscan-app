package org.angryscan.app.ui.windows.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSelector(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    icon: Painter? = null
) {
    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 34.dp)
            .clip(
                MaterialTheme.shapes.large
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            )
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            )
            .clickable(
                enabled = !selected,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
            if(icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null
                )
            }
        }
    }
}
