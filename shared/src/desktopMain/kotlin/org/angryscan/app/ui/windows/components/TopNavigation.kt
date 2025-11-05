package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun TopNavigation(
    navController: NavController,
    windowPlacement: androidx.compose.ui.window.WindowPlacement? = null,
    expanded: Boolean = false,
    onMinimizeClick: (() -> Unit)? = null,
    onExpandClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogo(navController = navController)
                
                NavigationTabs(
                    navController = navController,
                    currentDestination = destination
                )
            }

            if (onMinimizeClick != null && onExpandClick != null && onCloseClick != null) {
                WindowControlButtons(
                    windowPlacement = windowPlacement,
                    expanded = expanded,
                    onMinimizeClick = onMinimizeClick,
                    onExpandClick = onExpandClick,
                    onCloseClick = onCloseClick
                )
            }
        }
    }
}

