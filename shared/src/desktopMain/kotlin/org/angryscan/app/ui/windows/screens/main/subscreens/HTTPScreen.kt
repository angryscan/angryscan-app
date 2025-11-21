package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.MainScreen_SelectPathPlaceholder
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.ui.windows.components.RadioButtonNavigation
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.settings.SettingsButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun HTTPScreen(
    navController: androidx.navigation.NavController,
    settingsExpanded: Boolean,
    expandSettings: () -> Unit,
    hideSettings: () -> Unit,
    expandScanState: (Int) -> Unit
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(screenStateSettings.httpScreenState.path) }

    val settingsButtonTransition = updateTransition(settingsExpanded)

    val settingsBoxTransition = updateTransition(settingsExpanded)

    var scanNotCorrectPath by remember { mutableStateOf(false) }

    var selectPathError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    fun saveScreenState() {
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(500) // Debounce 500ms
            screenStateSettings.httpScreenState.path = path
            screenStateSettings.httpScreenState.extensions.clear()
            screenStateSettings.httpScreenState.extensions.addAll(scanSettings.extensions)
            screenStateSettings.httpScreenState.matchers.clear()
            screenStateSettings.httpScreenState.matchers.addAll(scanSettings.matchers)
            screenStateSettings.httpScreenState.userSignatures.clear()
            screenStateSettings.httpScreenState.userSignatures.addAll(scanSettings.userSignatures)
            screenStateSettings.httpScreenState.fastScan.value = scanSettings.fastScan.value
            screenStateSettings.save()
        }
    }
    
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnHTTPScreen = backStackEntry?.destination?.hasRoute(MainScreenConnector.HTTP::class) == true
    
    var hasLoadedHTTPSettings by remember { mutableStateOf(false) }
    
    // Load detection settings when entering this screen
    LaunchedEffect(isOnHTTPScreen) {
        if (isOnHTTPScreen && !hasLoadedHTTPSettings) {
            val hasSavedDetectionSettings = screenStateSettings.httpScreenState.extensions.isNotEmpty() || 
                                           screenStateSettings.httpScreenState.matchers.isNotEmpty() || 
                                           screenStateSettings.httpScreenState.userSignatures.isNotEmpty()
            val hasOtherSavedState = screenStateSettings.httpScreenState.path.isNotEmpty()
            val hasSavedState = hasSavedDetectionSettings || hasOtherSavedState
            
            if (hasSavedState) {
                scanSettings.extensions.clear()
                scanSettings.extensions.addAll(screenStateSettings.httpScreenState.extensions)
                scanSettings.matchers.clear()
                scanSettings.matchers.addAll(screenStateSettings.httpScreenState.matchers)
                scanSettings.userSignatures.clear()
                scanSettings.userSignatures.addAll(screenStateSettings.httpScreenState.userSignatures)
                scanSettings.fastScan.value = screenStateSettings.httpScreenState.fastScan.value
                scanSettings.save()
            }
            hasLoadedHTTPSettings = true
        } else if (!isOnHTTPScreen) {
            hasLoadedHTTPSettings = false
        }
    }
    
    // Save detection settings using snapshotFlow to detect changes in mutableStateListOf
    LaunchedEffect(isOnHTTPScreen) {
        if (!isOnHTTPScreen) return@LaunchedEffect
        
        var saveJob: kotlinx.coroutines.Job? = null
        
        snapshotFlow { 
            Triple(
                scanSettings.extensions.size to scanSettings.extensions.joinToString(",") { it.name },
                scanSettings.matchers.size to scanSettings.matchers.joinToString(",") { it::class.simpleName ?: "" },
                scanSettings.userSignatures.size to scanSettings.userSignatures.joinToString(",") { it.name }
            )
        }.collect { (_, _, _) ->
            if (isOnHTTPScreen) {
                saveJob?.cancel()
                saveJob = coroutineScope.launch {
                    delay(300)
                    screenStateSettings.httpScreenState.extensions.clear()
                    screenStateSettings.httpScreenState.extensions.addAll(scanSettings.extensions)
                    screenStateSettings.httpScreenState.matchers.clear()
                    screenStateSettings.httpScreenState.matchers.addAll(scanSettings.matchers)
                    screenStateSettings.httpScreenState.userSignatures.clear()
                    screenStateSettings.httpScreenState.userSignatures.addAll(scanSettings.userSignatures)
                    screenStateSettings.httpScreenState.fastScan.value = scanSettings.fastScan.value
                    screenStateSettings.save()
                }
            }
        }
    }
    
    LaunchedEffect(isOnHTTPScreen, scanSettings.fastScan.value) {
        if (isOnHTTPScreen) {
            screenStateSettings.httpScreenState.fastScan.value = scanSettings.fastScan.value
            screenStateSettings.save()
        }
    }

    LaunchedEffect(scanNotCorrectPath) {
        if (scanNotCorrectPath) {
            selectPathError = true
            delay(200)
            selectPathError = false
            delay(400)
            selectPathError = true
            delay(200)
            selectPathError = false
            delay(400)
            selectPathError = true
            delay(200)
            selectPathError = false
            scanNotCorrectPath = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (settingsExpanded) 0.dp else 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier
                .height(80.dp)
                .width(700.dp),
            value = path,
            onValueChange = {
                path = it
                    .split("\\s".toRegex())
                    .filter { url -> url.trim().isNotEmpty() }
                    .joinToString(";")
                saveScreenState()
            },
            placeholder = { Text(text = stringResource(Res.string.MainScreen_SelectPathPlaceholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = selectPathError,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(64.dp)
                        .size(48.dp)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize(),
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .width(700.dp)
                .padding(vertical = 0.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            RadioButtonNavigation(
                navController = navController
            )
        }

        Row {
                Button(
                    onClick = {

                        if (
                            path.split(";").all {
                                it.startsWith("http://") ||
                                        it.startsWith("https://")
                            }
                        ) {
                            // Save state before scanning
                            saveScreenState()
                            coroutineScope.launch {
                                val task = scanService.createTask(
                                    path = path,
                                    extensions = scanSettings.extensions,
                                    matchers = scanSettings.matchers + scanSettings.userSignatures,
                                    fastScan = scanSettings.fastScan.value,
                                    connector = ConnectorHTTP()
                                )
                                scanService.startTask(task)
                                task.id.value?.let { taskId ->
                                    expandScanState(taskId)
                                }

                            }
                        } else {
                            scanNotCorrectPath = true
                        }
                    },
                    modifier = Modifier
                        .width(268.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.MainScreen_ScanStartButton),
                        fontSize = 24.sp
                    )
                }
                SettingsButton(
                    transition = settingsButtonTransition,
                    onClick = {
                        if (!settingsExpanded) {
                            expandSettings()
                        } else {
                            hideSettings()
                        }
                    }
                )
            }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            SettingsBox(
                transition = settingsBoxTransition
            )
        }
    }
}