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
import androidx.compose.ui.graphics.Color
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
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.85f)
    val backgroundColor = if (selected) colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val contentColor = if (selected) colorScheme.primary else colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(width = 172.dp, height = 32.dp)
            .clip(
                MaterialTheme.shapes.large
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .background(
                color = backgroundColor,
            )
            .clickable(
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
                fontSize = 13.sp,
                lineHeight = 13.sp,
                color = contentColor
            )
            if(icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}
