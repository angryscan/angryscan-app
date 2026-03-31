package org.angryscan.app.ui.windows.screens.main

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.angryscan.app.resources.MainScreen_SettingsHint
import org.angryscan.app.resources.MainScreen_SettingsTitle
import org.angryscan.app.resources.Res
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.components.SourceSelectorTabs
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.subscreens.FileShareScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.HTTPScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.S3Screen
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainScreen(
    showScan: (taskId: Int) -> Unit
) {
    val navController = rememberNavController()
    var bottomBarContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var settingsOpened by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val isS3Source = backStackEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize()) {
        // Primary action block is centered in the full window.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(0, -40.dp.roundToPx()) }
                    .widthIn(max = 1080.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    bottomBarContent()
                    DescriptionTooltip(
                        description = stringResource(Res.string.MainScreen_SettingsHint),
                        delay = 350
                    ) {
                        TextButton(
                            onClick = { settingsOpened = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ManageAccounts,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.MainScreen_SettingsTitle),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 1080.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                SourceSelectorTabs(
                    navController = navController,
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                )
            }
        }

        if (settingsOpened) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.scrim.copy(alpha = 0.35f))
                    .padding(horizontal = 56.dp, vertical = 44.dp),
                color = colorScheme.surface.copy(alpha = 0.98f),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.MainScreen_SettingsTitle),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { settingsOpened = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            )
                        ) {
                            Text("Done")
                        }
                    }
                    SettingsBox(
                        transition = updateTransition(targetState = true, label = "settings-popup"),
                        isS3Source = isS3Source
                    )
                }
            }
        }
    }

    // Keep subscreen-specific state and bottom bar logic active.
    Box(modifier = Modifier.size(0.dp)) {
        NavHost(
            navController = navController,
            startDestination = MainScreenConnector.FileShare
        ) {
            composable<MainScreenConnector.FileShare> {
                FileShareScreen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content }
                )
            }
            composable<MainScreenConnector.S3> {
                S3Screen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content }
                )
            }
            composable<MainScreenConnector.HTTP> {
                HTTPScreen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content }
                )
            }
        }
    }
}
