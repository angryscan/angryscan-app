package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.OS

@Composable
fun DesktopWindowShapes() = if (OS.currentOS() == OS.WINDOWS)
    MaterialTheme.shapes.medium.copy(CornerSize(0.dp))
else
    MaterialTheme.shapes.medium