package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
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
import org.angryscan.app.resources.MainScreen_ScanHint_S3
import org.angryscan.app.resources.MainScreen_SelectPathPlaceholder
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun S3Screen(
    navController: androidx.navigation.NavController,
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {}
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(screenStateSettings.s3ScreenState.path) }
    var endpoint by remember { mutableStateOf(screenStateSettings.s3ScreenState.endpoint) }
    var accessKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.accessKey) }
    var secretKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.secretKey) }
    var bucket by remember { mutableStateOf(screenStateSettings.s3ScreenState.bucket) }

    val coroutineScope = rememberCoroutineScope()

    var selectPathError by remember { mutableStateOf(false) }

    var scanNotCorrectPath by remember { mutableStateOf(false) }
    var incorrectConnection by remember { mutableStateOf(false) }
    var incorrectPathError by remember { mutableStateOf(false) }

    val (validationErrorDialog, validateAndShowError, dismissValidationError) = rememberScanValidation(scanSettings)

    LaunchedEffect(scanNotCorrectPath, incorrectConnection) {
        if (scanNotCorrectPath || incorrectConnection) {
            if (incorrectPathError)
                selectPathError = true
            delay(200)
            selectPathError = false
            delay(400)
            if (incorrectPathError)
                selectPathError = true
            delay(200)
            selectPathError = false
            delay(400)
            if (incorrectPathError)
                selectPathError = true
            delay(200)
            selectPathError = false
            scanNotCorrectPath = false
            incorrectConnection = false
        }
    }

    val focusRequested by ScanPathHelper.focusRequested.collectAsState()

    LaunchedEffect(helperPath) {
        if (helperPath.isNotEmpty()) {
            path = helperPath
            coroutineScope.launch {
                delay(100)
                screenStateSettings.s3ScreenState.path = path
                screenStateSettings.save()
            }
            if (focusRequested)
                ScanPathHelper.resetFocus()
        }
    }

    var selectPathDialog by remember { mutableStateOf(false) }

    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun saveScreenState() {
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(500) // Debounce 500ms
            screenStateSettings.s3ScreenState.path = path
            screenStateSettings.s3ScreenState.endpoint = endpoint
            screenStateSettings.s3ScreenState.accessKey = accessKey
            screenStateSettings.s3ScreenState.secretKey = secretKey
            screenStateSettings.s3ScreenState.bucket = bucket
            screenStateSettings.s3ScreenState.extensions.clear()
            screenStateSettings.s3ScreenState.extensions.addAll(scanSettings.extensions)
            screenStateSettings.s3ScreenState.matchers.clear()
            screenStateSettings.s3ScreenState.matchers.addAll(scanSettings.matchers)
            screenStateSettings.s3ScreenState.userSignatures.clear()
            screenStateSettings.s3ScreenState.userSignatures.addAll(scanSettings.userSignatures)
            screenStateSettings.s3ScreenState.fastScan.value = scanSettings.fastScan.value
            screenStateSettings.save()
        }
    }
    
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnS3Screen = backStackEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true
    
    var hasLoadedS3Settings by remember { mutableStateOf(false) }
    
    // Sync S3 connection params from settings when entering S3 screen (they are edited in Scan options)
    LaunchedEffect(isOnS3Screen) {
        if (isOnS3Screen) {
            endpoint = screenStateSettings.s3ScreenState.endpoint
            accessKey = screenStateSettings.s3ScreenState.accessKey
            secretKey = screenStateSettings.s3ScreenState.secretKey
            bucket = screenStateSettings.s3ScreenState.bucket
        }
    }

    // Load detection settings when entering this screen
    LaunchedEffect(isOnS3Screen) {
        if (isOnS3Screen && !hasLoadedS3Settings) {
            val hasSavedDetectionSettings = screenStateSettings.s3ScreenState.extensions.isNotEmpty() || 
                                           screenStateSettings.s3ScreenState.matchers.isNotEmpty() || 
                                           screenStateSettings.s3ScreenState.userSignatures.isNotEmpty()
            val hasOtherSavedState = screenStateSettings.s3ScreenState.path.isNotEmpty() ||
                                    screenStateSettings.s3ScreenState.endpoint.isNotEmpty() ||
                                    screenStateSettings.s3ScreenState.accessKey.isNotEmpty() ||
                                    screenStateSettings.s3ScreenState.bucket.isNotEmpty()
            val hasSavedState = hasSavedDetectionSettings || hasOtherSavedState
            
            if (hasSavedState) {
                scanSettings.extensions.clear()
                scanSettings.extensions.addAll(screenStateSettings.s3ScreenState.extensions)
                scanSettings.matchers.clear()
                scanSettings.matchers.addAll(screenStateSettings.s3ScreenState.matchers)
                scanSettings.userSignatures.clear()
                scanSettings.userSignatures.addAll(screenStateSettings.s3ScreenState.userSignatures)
                scanSettings.fastScan.value = screenStateSettings.s3ScreenState.fastScan.value
                scanSettings.save()
            }
            hasLoadedS3Settings = true
        } else if (!isOnS3Screen) {
            hasLoadedS3Settings = false
        }
    }
    
    // Save detection settings using snapshotFlow to detect changes in mutableStateListOf
    LaunchedEffect(isOnS3Screen) {
        if (!isOnS3Screen) return@LaunchedEffect
        
        var saveJob: kotlinx.coroutines.Job? = null
        
        snapshotFlow { 
            Triple(
                scanSettings.extensions.size to scanSettings.extensions.joinToString(",") { it.name },
                scanSettings.matchers.size to scanSettings.matchers.joinToString(",") { it::class.simpleName ?: "" },
                scanSettings.userSignatures.size to scanSettings.userSignatures.joinToString(",") { it.name }
            )
        }.collect { (_, _, _) ->
            if (isOnS3Screen) {
                saveJob?.cancel()
                saveJob = coroutineScope.launch {
                    delay(300)
                    screenStateSettings.s3ScreenState.extensions.clear()
                    screenStateSettings.s3ScreenState.extensions.addAll(scanSettings.extensions)
                    screenStateSettings.s3ScreenState.matchers.clear()
                    screenStateSettings.s3ScreenState.matchers.addAll(scanSettings.matchers)
                    screenStateSettings.s3ScreenState.userSignatures.clear()
                    screenStateSettings.s3ScreenState.userSignatures.addAll(scanSettings.userSignatures)
                    screenStateSettings.s3ScreenState.fastScan.value = scanSettings.fastScan.value
                    screenStateSettings.save()
                }
            }
        }
    }
    
    LaunchedEffect(isOnS3Screen, scanSettings.fastScan.value) {
        if (isOnS3Screen) {
            screenStateSettings.s3ScreenState.fastScan.value = scanSettings.fastScan.value
            screenStateSettings.save()
        }
    }

    if (selectPathDialog) {
        S3FileChooser(
            onAccept = {
                path = it.path
                selectPathDialog = false
                saveScreenState()
            },
            onDecline = { selectPathDialog = false },
            connector = ConnectorS3(
                endpointStr = endpoint,
                accessKey = accessKey,
                secretKey = secretKey,
                bucketStr = bucket
            )
        )
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
                            MaterialTheme.shapes.medium
                        )
                        else Modifier
                    ),
                shape = MaterialTheme.shapes.medium,
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
                        onValueChange = { path = it; saveScreenState() },
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        placeholder = { Text(stringResource(Res.string.MainScreen_SelectPathPlaceholder), style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        isError = selectPathError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    )
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    IconButton(
                        onClick = {
                            if (endpoint.isNotEmpty() && accessKey.isNotEmpty() && secretKey.isNotEmpty() && bucket.isNotEmpty()) {
                                selectPathDialog = true
                            } else {
                                incorrectConnection = true
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            val s3ScanEnabled = path.isNotEmpty() && endpoint.isNotEmpty() && accessKey.isNotEmpty() && secretKey.isNotEmpty() && bucket.isNotEmpty()
            if (s3ScanEnabled) {
                Button(
                    enabled = true,
                    onClick = {
                        if (!validateAndShowError()) return@Button
                        saveScreenState()
                        coroutineScope.launch {
                            val task = scanService.createTask(
                                path = path,
                                extensions = scanSettings.extensions,
                                matchers = scanSettings.matchers + scanSettings.userSignatures,
                                fastScan = scanSettings.fastScan.value,
                                connector = ConnectorS3(endpointStr = endpoint, accessKey = accessKey, secretKey = secretKey, bucketStr = bucket)
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
                    description = stringResource(Res.string.MainScreen_ScanHint_S3),
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