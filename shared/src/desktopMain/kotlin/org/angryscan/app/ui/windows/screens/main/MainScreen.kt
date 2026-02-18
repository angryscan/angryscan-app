package org.angryscan.app.ui.windows.screens.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.angryscan.app.resources.MainScreen_SettingsTitle
import org.angryscan.app.resources.MainScreen_SidebarTitle
import org.angryscan.app.resources.Res
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.components.MainScreenSidebar
import org.angryscan.app.ui.windows.screens.main.components.SourceSelector
import org.angryscan.app.ui.windows.screens.main.components.SourceSelectorTabs
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.subscreens.FileShareScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.HTTPScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.S3Screen
import org.jetbrains.compose.resources.stringResource

/**
 * Вариант селектора источника данных.
 * - [SourceSelectorVariant.Sidebar] — боковая панель (современный layout)
 * - [SourceSelectorVariant.FloatingIcons] — плавающая панель с иконками
 * - [SourceSelectorVariant.Tabs] — табы сверху
 */
private val SOURCE_SELECTOR_VARIANT = SourceSelectorVariant.Sidebar

private enum class SourceSelectorVariant {
    Sidebar,
    FloatingIcons,
    Tabs
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    // Overline-style: мелкий uppercase, letter-spacing, приглушённый цвет (паттерн из Material / Tailwind / дашбордов)
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MainScreen(
    showScan:(taskId:Int) -> Unit
) {
    var scanStateExpanded by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    var sidebarExtraContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var bottomBarContent by remember { mutableStateOf<@Composable () -> Unit>({}) }

    val settingsTransition = updateTransition(targetState = true, label = "settings")
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isS3Source = backStackEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Верхний блок: путь + кнопка Scan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 4.dp)
            ) {
                bottomBarContent()
            }
            // Ниже: источники и настройки
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
            if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Sidebar) {
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 8.dp)
                        .fillMaxHeight()
                ) {
                    SectionHeader(text = stringResource(Res.string.MainScreen_SidebarTitle))
                    MainScreenSidebar(
                        navController = navController,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        extraContent = sidebarExtraContent
                    )
                }
            }

            if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Sidebar) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 8.dp, bottom = 24.dp, start = 8.dp, end = 24.dp)
                ) {
                    SectionHeader(text = stringResource(Res.string.MainScreen_SettingsTitle))
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        SettingsBox(transition = settingsTransition, isS3Source = isS3Source)
                    }
                }
            }

            Column(
                modifier = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Sidebar) {
                    Modifier
                        .width(0.dp)
                        .fillMaxHeight()
                        .padding(
                            start = 0.dp,
                            end = 0.dp,
                            top = 24.dp,
                            bottom = 40.dp
                        )
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(
                            start = 56.dp,
                            end = 0.dp,
                            top = 32.dp,
                            bottom = 40.dp
                        )
                },
                horizontalAlignment = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Sidebar)
                    Alignment.Start
                else
                    Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Tabs) {
                    SourceSelectorTabs(
                        navController = navController,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                }

                NavHost(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                            expandScanState = { taskId ->
                                scanStateExpanded = false
                                showScan(taskId)
                            },
                            setSidebarContent = { content -> sidebarExtraContent = content },
                            setBottomBarContent = { content -> bottomBarContent = content }
                        )
                    }
                    composable<MainScreenConnector.S3> {
                        S3Screen(
                            navController = navController,
                            expandScanState = { taskId ->
                                scanStateExpanded = false
                                showScan(taskId)
                            },
                            setSidebarContent = { content -> sidebarExtraContent = content },
                            setBottomBarContent = { content -> bottomBarContent = content }
                        )
                    }
                    composable<MainScreenConnector.HTTP> {
                        HTTPScreen(
                            navController = navController,
                            expandScanState = { taskId ->
                                scanStateExpanded = false
                                showScan(taskId)
                            },
                            setSidebarContent = { content -> sidebarExtraContent = content },
                            setBottomBarContent = { content -> bottomBarContent = content }
                        )
                    }
                }
            }
            }
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

