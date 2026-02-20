package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.MainScreen_ScanHint_HTTP
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun HTTPScreen(
    navController: androidx.navigation.NavController,
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {}
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(screenStateSettings.httpScreenState.path) }

    var scanNotCorrectPath by remember { mutableStateOf(false) }

    var selectPathError by remember { mutableStateOf(false) }
    
    val (validationErrorDialog, validateAndShowError, dismissValidationError) = rememberScanValidation(scanSettings)

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

    setSidebarContent { }
    setBottomBarContent {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .then(
                        if (selectPathError) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            RoundedCornerShape(20.dp)
                        )
                        else Modifier
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = path,
                        onValueChange = {
                            path = it
                                .split("\\s".toRegex())
                                .filter { url -> url.trim().isNotEmpty() }
                                .joinToString(";")
                            saveScreenState()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        placeholder = {
                            Text(
                                text = "Enter URLs separated by space or semicolon (;)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = selectPathError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    )
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (path.isNotEmpty()) {
                Button(
                    enabled = true,
                    onClick = {
                        if (!path.split(";").all {
                                it.startsWith("http://") || it.startsWith("https://")
                            }
                        ) {
                            scanNotCorrectPath = true
                            return@Button
                        }
                        if (!validateAndShowError()) return@Button
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
                            task.id.value?.let { expandScanState(it) }
                        }
                    },
                    modifier = ScanButtonModifier(
                        isReady = true,
                        modifier = Modifier.wrapContentWidth().height(72.dp).widthIn(min = 200.dp)
                    ).scanButtonHoverFeedback(enabled = true),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp,
                        disabledElevation = 2.dp
                    ),
                    colors = startScanButtonColors()
                ) {
                    StartScanButtonContent()
                }
            } else {
                DescriptionTooltip(
                    description = stringResource(Res.string.MainScreen_ScanHint_HTTP),
                    delay = 400
                ) {
                    Button(
                        enabled = false,
                        onClick = { },
                        modifier = ScanButtonModifier(
                            isReady = false,
                            modifier = Modifier.wrapContentWidth().height(72.dp).widthIn(min = 200.dp)
                        ).scanButtonHoverFeedback(enabled = false),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp,
                            disabledElevation = 0.dp
                        ),
                        colors = startScanButtonColors()
                    ) {
                        StartScanButtonContent()
                    }
                }
            }
        }
    }

    // Validation error dialog
    ScanValidationErrorDialog(
        validationError = validationErrorDialog,
        onDismiss = dismissValidationError
    )
}