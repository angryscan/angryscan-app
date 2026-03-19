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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.angryscan.app.resources.MainScreen_SettingsTitle
import org.angryscan.app.resources.MainScreen_SidebarTitle
import org.angryscan.app.resources.Res
import org.angryscan.app.ui.windows.screens.main.components.*
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.subscreens.FileShareScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.HTTPScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.S3Screen
import org.jetbrains.compose.resources.stringResource

/**
 * Вариант 2: одна колонка — табы источника, путь+скан на всю ширину, настройки в аккордеоне.
 * Остальные варианты — классический layout (сайдбар / табы / плавающие иконки).
 */
private val SOURCE_SELECTOR_VARIANT = SourceSelectorVariant.UnifiedColumn

private enum class SourceSelectorVariant {
    Sidebar,
    FloatingIcons,
    Tabs,
    UnifiedColumn
}

private val MainContentVerticalSpacing = 18.dp

@Composable
fun MainScreen(
    showScan: (taskId: Int) -> Unit
) {
    var scanStateExpanded by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    var sidebarExtraContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var bottomBarContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    val settingsTransition = updateTransition(targetState = true, label = "settings")
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isS3Source = backStackEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true

    var pathCardAdvancedExpanded by rememberSaveable { mutableStateOf(false) }

    val navHost: @Composable (Modifier) -> Unit = { mod ->
        NavHost(
            navController = navController,
            startDestination = MainScreenConnector.FileShare,
            modifier = mod,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(500)
                ) + fadeIn(tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(500)
                ) + fadeOut(tween(400))
            }
        ) {
            composable<MainScreenConnector.FileShare> {
                FileShareScreen(
                    navController = navController,
                    expandScanState = { taskId ->
                        scanStateExpanded = false
                        showScan(taskId)
                    },
                    setSidebarContent = { sidebarExtraContent = it },
                    setBottomBarContent = { bottomBarContent = it },
                    unifiedPathCard = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.UnifiedColumn) {
                        UnifiedPathCardExtras(
                            advancedExpanded = pathCardAdvancedExpanded,
                            onAdvancedExpandedChange = { pathCardAdvancedExpanded = it },
                            settingsContent = {
                                SettingsBox(
                                    transition = settingsTransition,
                                    isS3Source = false,
                                    embedded = true
                                )
                            }
                        )
                    } else {
                        null
                    }
                )
            }
            composable<MainScreenConnector.S3> {
                S3Screen(
                    navController = navController,
                    expandScanState = { taskId ->
                        scanStateExpanded = false
                        showScan(taskId)
                    },
                    setSidebarContent = { sidebarExtraContent = it },
                    setBottomBarContent = { bottomBarContent = it },
                    unifiedPathCard = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.UnifiedColumn) {
                        UnifiedPathCardExtras(
                            advancedExpanded = pathCardAdvancedExpanded,
                            onAdvancedExpandedChange = { pathCardAdvancedExpanded = it },
                            settingsContent = {
                                SettingsBox(
                                    transition = settingsTransition,
                                    isS3Source = true,
                                    embedded = true
                                )
                            }
                        )
                    } else {
                        null
                    }
                )
            }
            composable<MainScreenConnector.HTTP> {
                HTTPScreen(
                    navController = navController,
                    expandScanState = { taskId ->
                        scanStateExpanded = false
                        showScan(taskId)
                    },
                    setSidebarContent = { sidebarExtraContent = it },
                    setBottomBarContent = { bottomBarContent = it },
                    unifiedPathCard = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.UnifiedColumn) {
                        UnifiedPathCardExtras(
                            advancedExpanded = pathCardAdvancedExpanded,
                            onAdvancedExpandedChange = { pathCardAdvancedExpanded = it },
                            settingsContent = {
                                SettingsBox(
                                    transition = settingsTransition,
                                    isS3Source = false,
                                    embedded = true
                                )
                            }
                        )
                    } else {
                        null
                    }
                )
            }
        }
    }

    if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.UnifiedColumn) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .offset(x = (-8).dp, y = (-8).dp)
            ) {
                navHost(Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 24.dp,
                        vertical = MainContentVerticalSpacing
                    )
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    bottomBarContent()
                }
                Spacer(modifier = Modifier.height(MainContentVerticalSpacing))
                SourceSelectorTabs(
                    navController = navController,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        top = MainContentVerticalSpacing,
                        end = 24.dp,
                        bottom = MainContentVerticalSpacing
                    )
            ) {
                bottomBarContent()
            }
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
                            .padding(
                                top = 0.dp,
                                bottom = MainContentVerticalSpacing,
                                start = 24.dp,
                                end = 8.dp
                            )
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = stringResource(Res.string.MainScreen_SidebarTitle),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = MainContentVerticalSpacing)
                        )
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
                            .padding(
                                top = 0.dp,
                                bottom = MainContentVerticalSpacing,
                                start = 8.dp,
                                end = 24.dp
                            )
                    ) {
                        Text(
                            text = stringResource(Res.string.MainScreen_SettingsTitle),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = MainContentVerticalSpacing)
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            SettingsBox(
                                transition = settingsTransition,
                                isS3Source = isS3Source
                            )
                        }
                    }
                }

                Column(
                    modifier = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Sidebar) {
                        Modifier
                            .width(0.dp)
                            .fillMaxHeight()
                            .padding(top = 24.dp, bottom = 40.dp)
                    } else {
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(start = 56.dp, end = 0.dp, top = 32.dp, bottom = 40.dp)
                    },
                    horizontalAlignment = if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Sidebar) {
                        Alignment.Start
                    } else {
                        Alignment.CenterHorizontally
                    },
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (SOURCE_SELECTOR_VARIANT == SourceSelectorVariant.Tabs) {
                        SourceSelectorTabs(
                            navController = navController,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                    navHost(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
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
