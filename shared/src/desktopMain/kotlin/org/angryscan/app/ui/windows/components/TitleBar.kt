package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope

@Composable
fun WindowScope.TitleBar(
    windowPlacement: WindowPlacement,
    block: @Composable () -> Unit
) {
    if (windowPlacement == WindowPlacement.Floating)
        WindowDraggableArea {
            block()
        }
    else
        block()
}