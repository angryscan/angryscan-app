package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.MainScreen_Placeholder_HTTP
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.ui.hasSelectedMatchersForScan
import org.angryscan.app.ui.windows.screens.main.components.*
import org.angryscan.app.ui.windows.screens.main.rememberMainSourceRowTokens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun HTTPScreen(
    navController: androidx.navigation.NavController,
    expandScanState: (Int) -> Unit,
    onRequireScanSettings: (missingExtensions: Boolean, missingMatchers: Boolean) -> Unit = { _, _ -> },
    onRequireSourceInputs: () -> Unit = {},
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
    setUnderSourceContent: (@Composable () -> Unit) -> Unit = {}
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(screenStateSettings.httpScreenState.path) }
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
            withContext(Dispatchers.IO) { screenStateSettings.save() }
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
            if (hasSavedDetectionSettings) {
                scanSettings.extensions.clear()
                scanSettings.extensions.addAll(screenStateSettings.httpScreenState.extensions)
                scanSettings.matchers.clear()
                scanSettings.matchers.addAll(screenStateSettings.httpScreenState.matchers)
                scanSettings.userSignatures.clear()
                scanSettings.userSignatures.addAll(screenStateSettings.httpScreenState.userSignatures)
                scanSettings.fastScan.value = screenStateSettings.httpScreenState.fastScan.value
                withContext(Dispatchers.IO) { scanSettings.save() }
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
                    withContext(Dispatchers.IO) { screenStateSettings.save() }
                }
            }
        }
    }

    LaunchedEffect(isOnHTTPScreen, scanSettings.fastScan.value) {
        if (isOnHTTPScreen) {
            screenStateSettings.httpScreenState.fastScan.value = scanSettings.fastScan.value
            withContext(Dispatchers.IO) { screenStateSettings.save() }
        }
    }

    LaunchedEffect(selectPathError) {
        if (selectPathError) {
            delay(2000)
            if (selectPathError) {
                selectPathError = false
            }
        }
    }

    setSidebarContent { }
    setUnderSourceContent { }
    setBottomBarContent {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sourceTokens = rememberMainSourceRowTokens(maxWidth, maxHeight)
            val controlHeight = sourceTokens.controlHeight
            val controlShape = RoundedCornerShape(sourceTokens.controlCorner)
            val scanButtonWidth = when {
                maxWidth >= 1500.dp -> sourceTokens.scanButtonWidthWide
                maxWidth < 1200.dp -> sourceTokens.scanButtonWidthCompact
                else -> sourceTokens.scanButtonWidthRegular
            }
            val controlGap = if (maxWidth < 1200.dp) sourceTokens.controlGapCompact else sourceTokens.controlGapRegular
            val pathMinWidth = if (maxWidth < 1200.dp) sourceTokens.pathMinWidthCompact else sourceTokens.pathMinWidthRegular
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(controlGap, Alignment.Start)
            ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = pathMinWidth)
                    .height(controlHeight)
                    .then(
                        if (selectPathError) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            controlShape
                        )
                        else Modifier
                    ),
                shape = controlShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = sourceTokens.inlinePaddingHorizontal,
                            vertical = sourceTokens.inlinePaddingVertical
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sourceTokens.inlineControlGap)
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
                        modifier = Modifier.weight(1f).heightIn(min = sourceTokens.fieldMinHeight),
                        placeholder = {
                            Text(
                                text = stringResource(Res.string.MainScreen_Placeholder_HTTP),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = RoundedCornerShape(sourceTokens.compactFieldCorner + 4.dp),
                        isError = selectPathError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent
                        )
                    )
                }
            }
            Button(
                enabled = true,
                onClick = {
                    val urls = path.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                    if (urls.isEmpty()) {
                        onRequireSourceInputs()
                        selectPathError = true
                        return@Button
                    }
                    val missingExtensions = scanSettings.extensions.isEmpty()
                    val missingMatchers = !hasSelectedMatchersForScan(scanSettings)
                    if (missingExtensions || missingMatchers) {
                        onRequireScanSettings(missingExtensions, missingMatchers)
                        return@Button
                    }
                    if (!urls.all { it.startsWith("http://") || it.startsWith("https://") }) {
                        onRequireSourceInputs()
                        selectPathError = true
                        return@Button
                    }
                    if (!validateAndShowError()) return@Button
                    val normalizedPath = urls.joinToString(";")
                    path = normalizedPath
                    saveScreenState()
                    coroutineScope.launch {
                        val task = scanService.createTask(
                            path = normalizedPath,
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
                    modifier = Modifier.width(scanButtonWidth).height(controlHeight)
                ).scanButtonHoverFeedback(enabled = true).scanButtonChipBorder(),
                shape = controlShape,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                ),
                colors = startScanButtonColors()
            ) {
                StartScanButtonContent()
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