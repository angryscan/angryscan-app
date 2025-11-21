package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanStatItem(
    title: String,
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp,
        )
    }
}