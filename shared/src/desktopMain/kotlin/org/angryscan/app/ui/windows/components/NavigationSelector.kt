package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.navigation.NavController

@Composable
fun WindowScope.NavigationSelector(
    navController: NavController,
    windowPlacement: WindowPlacement? = null,
    expanded: Boolean = false,
    onMinimizeClick: (() -> Unit)? = null,
    onExpandClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null
) {
    if (windowPlacement == WindowPlacement.Floating) {
        WindowDraggableArea {
            TopNavigation(
                navController = navController,
                windowPlacement = windowPlacement,
                expanded = expanded,
                onMinimizeClick = onMinimizeClick,
                onExpandClick = onExpandClick,
                onCloseClick = onCloseClick
            )
        }
    } else {
        TopNavigation(
            navController = navController,
            windowPlacement = windowPlacement,
            expanded = expanded,
            onMinimizeClick = onMinimizeClick,
            onExpandClick = onExpandClick,
            onCloseClick = onCloseClick
        )
    }
}

