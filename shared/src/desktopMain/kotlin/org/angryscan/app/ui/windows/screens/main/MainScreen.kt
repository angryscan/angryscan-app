package org.angryscan.app.ui.windows.screens.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.components.SourceSelector
import org.angryscan.app.ui.windows.screens.main.components.SourceSelectorTabs
import org.angryscan.app.ui.windows.screens.main.subscreens.FileShareScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.HTTPScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.S3Screen
import org.koin.compose.koinInject

/**
 * Вариант селектора источника данных.
 * Место переключения: измените значение ниже, затем Stop → Run.
 *
 * - [SourceSelectorVariant.FloatingIcons] — плавающая панель с иконками (tooltip при hover)
 * - [SourceSelectorVariant.Tabs] — табы с иконками и подписями (File Share, AWS S3, HTTP)
 */
private val SOURCE_SELECTOR_VARIANT = SourceSelectorVariant.Tabs  // по умолчанию табы сверху

private enum class SourceSelectorVariant {
    FloatingIcons,
    Tabs
}

@Composable
fun MainScreen(
    showScan:(taskId:Int) -> Unit
) {
    val scanSettings = koinInject<ScanSettings>()
    val settingsExpandedState = scanSettings.mainScreenSettingsExpanded
    val settingsExpanded by settingsExpandedState

    var scanStateExpanded by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Tabs) {
                Spacer(modifier = Modifier.height(16.dp))
                SourceSelectorTabs(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp)
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            NavHost(
                modifier = Modifier.weight(1f),
                navController = navController,
                startDestination = MainScreenConnector.FileShare,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start, tween(700)
                    ) + fadeIn(tween(700))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End, tween(700)
                    ) + fadeOut(tween(700))
                }
            ) {
                composable<MainScreenConnector.FileShare> {
                    FileShareScreen(
                        navController = navController,
                        settingsExpanded = settingsExpanded,
                        expandSettings = {
                            if (scanStateExpanded)
                                scanStateExpanded = false
                            settingsExpandedState.value = true
                        },
                        hideSettings = {
                            settingsExpandedState.value = false
                        },
                        expandScanState = { taskId ->
                            scanStateExpanded = false
                            showScan(taskId)
                        }
                    )
                }
                composable<MainScreenConnector.S3> {
                    S3Screen(
                        navController = navController,
                        settingsExpanded = settingsExpanded,
                        expandSettings = {
                            if (scanStateExpanded)
                                scanStateExpanded = false
                            settingsExpandedState.value = true
                        },
                        hideSettings = {
                            settingsExpandedState.value = false
                        },
                        expandScanState = { taskId ->
                            scanStateExpanded = false
                            showScan(taskId)
                        }
                    )
                }
                composable<MainScreenConnector.HTTP> {
                    HTTPScreen(
                        navController = navController,
                        settingsExpanded = settingsExpanded,
                        expandSettings = {
                            if (scanStateExpanded)
                                scanStateExpanded = false
                            settingsExpandedState.value = true
                        },
                        hideSettings = {
                            settingsExpandedState.value = false
                        },
                        expandScanState = { taskId ->
                            scanStateExpanded = false
                            showScan(taskId)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.FloatingIcons) {
            SourceSelector(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )
        }
    }
}

