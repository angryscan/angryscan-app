package org.angryscan.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ch.qos.logback.classic.Level
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.OS
import org.angryscan.app.logging.LogLevel
import org.angryscan.app.navigation.AppScreen
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.appName
import org.angryscan.app.resources.eula_version
import org.angryscan.app.resources.icon
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.mainWindow
import org.angryscan.app.ui.dialogs.EulaDialog
import org.angryscan.app.ui.theme.AppTheme
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.NavigationSelector
import org.angryscan.app.ui.windows.screens.main.MainScreen
import org.angryscan.app.ui.windows.screens.scans.ScanResultScreen
import org.angryscan.app.ui.windows.screens.scans.ScansScreen
import org.angryscan.app.ui.windows.screens.settings.SettingsScreen
import java.awt.Dimension
import java.util.*

@Composable
fun MainWindow(
    onCloseRequest: () -> Unit,
    onHideRequest: () -> Unit,
    isVisible: Boolean
) {
    val windowState = rememberWindowState(width = 1280.dp, height = 720.dp)
    val focusRemember by ScanPathHelper.focusRequested.collectAsState()

    val appSettings = koinInject<AppSettings>()

    val hideOnMinimize by remember { appSettings.hideOnMinimize }
    val isMac = OS.currentOS() == OS.MAC

    val navController = rememberNavController()

    val debugMode by remember { appSettings.debugMode }

    val backStackEntry by navController.currentBackStackEntryAsState()

    val appLocale by remember { appSettings.language }
    LaunchedEffect(appLocale) {
        Locale.setDefault(Locale.forLanguageTag(appLocale.locale))
    }

    LaunchedEffect(focusRemember) {
        if (focusRemember) {
            if (backStackEntry?.destination?.hasRoute(AppScreen.Main::class) ?: false)
                navController.navigate(AppScreen.Main)
        }
    }

    LaunchedEffect(debugMode) {
        if (debugMode)
            LogLevel.setLoggingLevel(Level.DEBUG)
        else
            LogLevel.setLoggingLevel(Level.INFO)
    }

    Window(
        onCloseRequest = onCloseRequest,
        title = stringResource(Res.string.appName),
        state = windowState,
        undecorated = true,
        transparent = true,
        icon = painterResource(Res.drawable.icon),
        visible = isVisible,
        alwaysOnTop = focusRemember
    ) {
        mainWindow = this.window

        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(1280, 720)
        }

        var eulaAgreedVersion by remember { appSettings.eulaAgreedVersion }
        val eulaVersion = stringResource(Res.string.eula_version).toInt()
        var showEulaDialog by remember { mutableStateOf(eulaAgreedVersion < eulaVersion) }
        val dialogEulaState = rememberDialogState(
            width = 600.dp,
            height = 400.dp
        )

        AppTheme {
            if(showEulaDialog) {
                EulaDialog(
                    onAccept = {
                        eulaAgreedVersion = eulaVersion
                        appSettings.save()
                        showEulaDialog = false
                    },
                    onDecline = {
                        showEulaDialog = false
                        onCloseRequest()
                    },
                    dialogState = dialogEulaState
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize(),
                shape = DesktopWindowShapes(),
                shadowElevation = 3.dp,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    NavigationSelector(
                        navController = navController,
                        windowPlacement = windowState.placement,
                        expanded = windowState.placement == WindowPlacement.Maximized,
                        onMinimizeClick = {
                            if (hideOnMinimize && !isMac) {
                                onHideRequest()
                            } else {
                                windowState.isMinimized = true
                            }
                        },
                        onExpandClick = {
                            if (windowState.placement == WindowPlacement.Maximized)
                                windowState.placement = WindowPlacement.Floating
                            else
                                windowState.placement = WindowPlacement.Maximized
                        },
                        onCloseClick = onCloseRequest
                    )
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.Main,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.3f).toInt() },
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + scaleOut(
                                targetScale = 1.02f,
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> (-fullWidth * 0.3f).toInt() },
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + scaleOut(
                                targetScale = 1.02f,
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    ) {
                            composable<AppScreen.Main> {
                                MainScreen(
                                    showScan = { taskId ->
                                        navController.navigate(AppScreen.ScanResult(taskId))
                                    }
                                )
                            }
                            composable<AppScreen.Scans> {
                                ScansScreen(
                                    onTaskClick = { taskId ->
                                        navController.navigate(AppScreen.ScanResult(taskId))
                                    }
                                )
                            }
                            composable<AppScreen.ScanResult> { backStackEntry ->
                                val scanResult: AppScreen.ScanResult = backStackEntry.toRoute()
                                ScanResultScreen(
                                    scanResult.scanId,
                                    onCloseClick = { navController.popBackStack() },
                                )
                            }
                            composable<AppScreen.Settings> {
                                SettingsScreen()
                            }
                        }
                }
            }
        }
    }
}