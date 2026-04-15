package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
    setUnderSourceContent: (@Composable () -> Unit) -> Unit = {}
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

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestOk by remember { mutableStateOf<Boolean?>(null) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    var showConnectionErrorDialog by remember { mutableStateOf(false) }

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
            if (hasSavedDetectionSettings) {
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val controlHeight = 68.dp
            val controlShape = RoundedCornerShape(18.dp)
            val scanButtonWidth = when {
                maxWidth >= 1500.dp -> 240.dp
                maxWidth < 1200.dp -> 220.dp
                else -> 232.dp
            } * 0.75f
            val controlGap = if (maxWidth < 1200.dp) 8.dp else 12.dp
            val pathMinWidth = if (maxWidth < 1200.dp) 460.dp else 500.dp
            val blockMinWidth = pathMinWidth + controlGap + scanButtonWidth

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(min = blockMinWidth)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(controlGap)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
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
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                            OutlinedTextField(
                                value = path,
                                onValueChange = { path = it; saveScreenState() },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                placeholder = { Text(stringResource(Res.string.MainScreen_SelectPathPlaceholder), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
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
                                Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(22.dp), tint = SourceActionBlue)
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
                                    modifier = Modifier.width(scanButtonWidth).height(controlHeight)
                                ).scanButtonHoverFeedback(enabled = false).scanButtonChipBorder(),
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
            }
        }
    }

    // AWS connection params should appear under the source radio buttons.
    setUnderSourceContent {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val controlGap = if (maxWidth < 1200.dp) 8.dp else 12.dp
            val scanButtonWidth = when {
                maxWidth >= 1500.dp -> 240.dp
                maxWidth < 1200.dp -> 220.dp
                else -> 232.dp
            } * 0.75f
            val pathMinWidth = if (maxWidth < 1200.dp) 460.dp else 500.dp
            val blockMinWidth = pathMinWidth + controlGap + scanButtonWidth

            // Connection parameters (compact, single row)
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .widthIn(min = blockMinWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Same "feel" as the path block (bodyMedium), but keep compact size.
                    val compactSize = MaterialTheme.typography.bodySmall.fontSize
                    val compactLineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    val fieldTextStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = compactSize,
                        lineHeight = compactLineHeight
                    )
                    val placeholderStyle = fieldTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )

                    @Composable
                    fun CompactField(
                        value: String,
                        onValueChange: (String) -> Unit,
                        placeholder: String,
                        modifier: Modifier,
                        visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
                    ) {
                        val shape = RoundedCornerShape(10.dp)
                        Surface(
                            modifier = modifier
                                .height(32.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                    shape = shape
                                ),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    singleLine = true,
                                    textStyle = fieldTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    visualTransformation = visualTransformation,
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (value.isEmpty()) {
                                            Text(placeholder, style = placeholderStyle, maxLines = 1)
                                        }
                                        inner()
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactField(
                            value = endpoint,
                            onValueChange = { endpoint = it; saveScreenState(); connectionTestOk = null; connectionTestMessage = null },
                            placeholder = "Endpoint",
                            modifier = Modifier.weight(1.35f).widthIn(min = 200.dp)
                        )
                        CompactField(
                            value = bucket,
                            onValueChange = { bucket = it; saveScreenState(); connectionTestOk = null; connectionTestMessage = null },
                            placeholder = "Bucket",
                            modifier = Modifier.weight(0.85f).widthIn(min = 120.dp)
                        )
                        CompactField(
                            value = accessKey,
                            onValueChange = { accessKey = it; saveScreenState(); connectionTestOk = null; connectionTestMessage = null },
                            placeholder = "Access key",
                            modifier = Modifier.weight(1f).widthIn(min = 140.dp)
                        )
                        CompactField(
                            value = secretKey,
                            onValueChange = { secretKey = it; saveScreenState(); connectionTestOk = null; connectionTestMessage = null },
                            placeholder = "Secret key",
                            modifier = Modifier.weight(1f).widthIn(min = 125.dp),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.width(32.dp)
                    ) {
                        val canTest = endpoint.isNotBlank() && bucket.isNotBlank() && accessKey.isNotBlank() && secretKey.isNotBlank()
                        DescriptionTooltip(description = "Test connection", delay = 350) {
                            Button(
                                enabled = canTest && !isTestingConnection,
                                onClick = {
                                    isTestingConnection = true
                                    connectionTestOk = null
                                    connectionTestMessage = null
                                    showConnectionErrorDialog = false
                                    coroutineScope.launch {
                                        runCatching {
                                            ConnectorS3(
                                                endpointStr = endpoint,
                                                accessKey = accessKey,
                                                secretKey = secretKey,
                                                bucketStr = bucket
                                            ).use { it.testConnection(prefix = "") }
                                        }.onSuccess {
                                            connectionTestOk = true
                                            connectionTestMessage = "Connected"
                                        }.onFailure { e ->
                                            connectionTestOk = false
                                            connectionTestMessage = e.message ?: e::class.simpleName
                                            showConnectionErrorDialog = true
                                        }
                                        isTestingConnection = false
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = startScanButtonColors()
                            ) {
                                when {
                                    isTestingConnection -> CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    connectionTestOk == true -> Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    connectionTestOk == false -> Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
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

    if (showConnectionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showConnectionErrorDialog = false }) { Text("OK") }
            },
            title = { Text("Connection error") },
            text = { Text(connectionTestMessage.orEmpty()) }
        )
    }
}