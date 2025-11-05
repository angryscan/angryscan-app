package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.navigation.AppScreen
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService

@Composable
fun NavigationTabs(
    navController: NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val scanService = koinInject<ScanService>()
    val allTasks by scanService.tasks.tasks.collectAsState()

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val hasActiveScans = remember(currentTime, allTasks) {
        allTasks.any { task ->
            val state = task.state.value
            state == TaskState.SCANNING || state == TaskState.SEARCHING
        }
    }
    
    val navigationItems = listOf(
        NavigationItem(
            route = AppScreen.Main,
            icon = painterResource(Res.drawable.SideMenu_IconMainPage),
            label = stringResource(Res.string.SideMenu_MainPage),
            isSelected = currentDestination?.hasRoute(AppScreen.Main::class) ?: false,
            hasActiveIndicator = false
        ),
        NavigationItem(
            route = AppScreen.Scans,
            icon = painterResource(Res.drawable.SideMenu_IconScans),
            label = stringResource(Res.string.SideMenu_ScanListPage),
            isSelected = currentDestination?.hasRoute(AppScreen.Scans::class) ?: false ||
                    currentDestination?.hasRoute(AppScreen.ScanResult::class) ?: false,
            hasActiveIndicator = hasActiveScans
        ),
        NavigationItem(
            route = AppScreen.Settings,
            icon = painterResource(Res.drawable.SideMenu_IconSettings),
            label = stringResource(Res.string.SideMenu_SettingsPage),
            isSelected = currentDestination?.hasRoute(AppScreen.Settings::class) ?: false,
            hasActiveIndicator = false
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
    ) {
        navigationItems.forEach { item ->
            NavigationTab(
                item = item,
                onClick = { 
                    try {
                        println("Navigating to: ${item.route}")
                        if (item.route == AppScreen.Scans) {
                            val isOnScansScreen = currentDestination?.hasRoute(AppScreen.Scans::class) ?: false
                            if (!isOnScansScreen) {
                                navController.navigate(item.route)
                                println("Navigation to Scans successful")
                            } else {
                                println("Already on Scans screen - no navigation needed")
                            }
                        } else if (!item.isSelected) {
                            navController.navigate(item.route)
                            println("Navigation successful")
                        } else {
                            println("Already on this screen")
                        }
                    } catch (e: Exception) {
                        println("Navigation error: ${e.message}")
                    }
                }
            )
        }
    }
}

