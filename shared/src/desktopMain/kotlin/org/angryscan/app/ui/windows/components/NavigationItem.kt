package org.angryscan.app.ui.windows.components

import androidx.compose.ui.graphics.painter.Painter

data class NavigationItem(
    val route: Any,
    val icon: Painter,
    val label: String,
    val isSelected: Boolean,
    val hasActiveIndicator: Boolean = false
)

