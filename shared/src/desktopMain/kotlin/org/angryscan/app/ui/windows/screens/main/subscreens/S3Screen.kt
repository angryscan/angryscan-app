package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.angryscan.app.common.SavedS3Connection
import org.angryscan.app.common.SavedS3ConnectionsRepository
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.hasSelectedMatchersForScan
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun S3Screen(
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
    val savedS3ConnectionsRepository = koinInject<SavedS3ConnectionsRepository>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(screenStateSettings.s3ScreenState.path) }
    var endpoint by remember { mutableStateOf(screenStateSettings.s3ScreenState.endpoint) }
    var accessKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.accessKey) }
    var secretKey by remember { mutableStateOf("") }
    var bucket by remember { mutableStateOf(screenStateSettings.s3ScreenState.bucket) }

    val coroutineScope = rememberCoroutineScope()

    var selectPathError by remember { mutableStateOf(false) }
    var incorrectConnection by remember { mutableStateOf(false) }

    val (validationErrorDialog, validateAndShowError, dismissValidationError) = rememberScanValidation(scanSettings)
    val s3ConnectionSuccessMessage = stringResource(Res.string.ScanSettings_PostgresConnectionSuccess)
    val s3ConnectionErrorMessage = stringResource(Res.string.Validation_S3ConnectionMessage)

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestOk by remember { mutableStateOf<Boolean?>(null) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    var secretKeyVisible by remember { mutableStateOf(false) }
    var savedConnectionsExpanded by remember { mutableStateOf(false) }
    var selectedSavedConnectionKey by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConnection by remember { mutableStateOf<SavedS3Connection?>(null) }
    var pendingConnectionNameDialog by remember { mutableStateOf(false) }
    var pendingConnectionName by remember { mutableStateOf("") }
    var pendingConnectionNameError by remember { mutableStateOf(false) }
    var savedConnections by remember { mutableStateOf<List<SavedS3Connection>>(emptyList()) }

    LaunchedEffect(selectPathError) {
        if (selectPathError) {
            delay(2000)
            if (selectPathError) {
                selectPathError = false
            }
        }
    }
    LaunchedEffect(incorrectConnection) {
        if (incorrectConnection) {
            delay(2000)
            if (incorrectConnection) {
                incorrectConnection = false
            }
        }
    }

    val focusRequested by ScanPathHelper.focusRequested.collectAsState()

    LaunchedEffect(helperPath) {
        if (helperPath.isNotEmpty()) {
            path = helperPath
            coroutineScope.launch {
                delay(100)
                screenStateSettings.s3ScreenState.path = path
                withContext(Dispatchers.IO) { screenStateSettings.save() }
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
            screenStateSettings.s3ScreenState.secretKey = ""
            screenStateSettings.s3ScreenState.bucket = bucket
            screenStateSettings.s3ScreenState.extensions.clear()
            screenStateSettings.s3ScreenState.extensions.addAll(scanSettings.extensions)
            screenStateSettings.s3ScreenState.matchers.clear()
            screenStateSettings.s3ScreenState.matchers.addAll(scanSettings.matchers)
            screenStateSettings.s3ScreenState.userSignatures.clear()
            screenStateSettings.s3ScreenState.userSignatures.addAll(scanSettings.userSignatures)
            screenStateSettings.s3ScreenState.fastScan.value = scanSettings.fastScan.value
            withContext(Dispatchers.IO) { screenStateSettings.save() }
        }
    }

    fun savedConnectionUrl(conn: SavedS3Connection): String {
        val endpointPart = conn.endpoint.trim()
        val bucketPart = conn.bucket.trim()
        return if (bucketPart.isNotBlank()) "$endpointPart/$bucketPart" else endpointPart
    }

    fun savedConnectionPrimary(conn: SavedS3Connection): String =
        conn.name.trim().ifBlank { savedConnectionUrl(conn) }

    fun savedConnectionSecondary(conn: SavedS3Connection): String =
        "url: ${savedConnectionUrl(conn)}"

    fun savedConnectionTertiary(conn: SavedS3Connection): String =
        buildString {
            if (conn.bucket.isNotBlank()) append("bucket: ${conn.bucket}")
            if (conn.accessKey.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append("access key: ${conn.accessKey}")
            }
        }

    fun defaultConnectionName(): String {
        val endpointPart = endpoint.trim().ifBlank { "s3-connection" }
        val bucketPart = bucket.trim()
        return if (bucketPart.isNotBlank()) "$endpointPart/$bucketPart" else endpointPart
    }

    fun normalizeEndpointLookup(raw: String): String {
        return raw
            .trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }

    fun normalizeBucketLookup(raw: String): String {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isBlank()) return ""
        val noScheme = trimmed.substringAfter("://", trimmed)
        val firstSegment = noScheme.substringBefore('/').ifBlank { noScheme }
        return firstSegment.trim()
    }

    fun normalizeAccessKeyLookup(raw: String): String =
        raw.trim().lowercase()

    fun refreshSavedConnections() {
        coroutineScope.launch {
            savedConnections = scanService.withHistoryBatchesPaused {
                savedS3ConnectionsRepository.list()
            }
        }
    }

    fun applySavedConnection(conn: SavedS3Connection) {
        coroutineScope.launch {
            val storedSecret = savedS3ConnectionsRepository.getSecretKey(conn.connectionKey).orEmpty()
            endpoint = conn.endpoint
            bucket = conn.bucket
            accessKey = conn.accessKey
            secretKey = storedSecret
            selectedSavedConnectionKey = conn.connectionKey
            connectionTestOk = false
            connectionTestMessage = null
            saveScreenState()
        }
    }

    fun saveCurrentS3Connection(connectionName: String? = null) {
        val finalName = connectionName?.trim()?.takeIf { it.isNotBlank() } ?: defaultConnectionName()
        coroutineScope.launch {
            val key = scanService.withHistoryBatchesPaused {
                savedS3ConnectionsRepository.upsert(
                    name = finalName,
                    endpoint = endpoint,
                    bucket = bucket,
                    accessKey = accessKey,
                    secretKey = secretKey
                )
            }
            selectedSavedConnectionKey = key
            refreshSavedConnections()
        }
    }

    fun removeSavedConnection(conn: SavedS3Connection) {
        coroutineScope.launch {
            scanService.withHistoryBatchesPaused {
                savedS3ConnectionsRepository.remove(conn.connectionKey)
            }
            if (selectedSavedConnectionKey == conn.connectionKey) {
                selectedSavedConnectionKey = null
            }
            refreshSavedConnections()
            if (savedConnections.isEmpty()) {
                savedConnectionsExpanded = false
            }
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
            bucket = screenStateSettings.s3ScreenState.bucket
            if (endpoint.isNotBlank() && bucket.isNotBlank() && accessKey.isNotBlank()) {
                val restoredSecret = scanService.withHistoryBatchesPaused {
                    val directKey = savedS3ConnectionsRepository.connectionKey(
                        endpoint = endpoint,
                        bucket = bucket,
                        accessKey = accessKey
                    )
                    savedS3ConnectionsRepository.getSecretKey(directKey)
                        ?: savedS3ConnectionsRepository
                            .list()
                            .firstOrNull { conn ->
                                normalizeEndpointLookup(conn.endpoint) == normalizeEndpointLookup(endpoint) &&
                                    normalizeBucketLookup(conn.bucket) == normalizeBucketLookup(bucket) &&
                                    normalizeAccessKeyLookup(conn.accessKey) == normalizeAccessKeyLookup(accessKey)
                            }
                            ?.let { matched -> savedS3ConnectionsRepository.getSecretKey(matched.connectionKey) }
                }
                secretKey = restoredSecret.orEmpty()
            } else {
                secretKey = ""
            }
            refreshSavedConnections()
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
                withContext(Dispatchers.IO) { scanSettings.save() }
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
                    withContext(Dispatchers.IO) { screenStateSettings.save() }
                }
            }
        }
    }

    LaunchedEffect(isOnS3Screen, scanSettings.fastScan.value) {
        if (isOnS3Screen) {
            screenStateSettings.s3ScreenState.fastScan.value = scanSettings.fastScan.value
            withContext(Dispatchers.IO) { screenStateSettings.save() }
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

                    Button(
                        enabled = true,
                        onClick = {
                            val normalizedPath = path.trim()
                            val hasConnectionSettings = endpoint.isNotBlank() &&
                                bucket.isNotBlank() &&
                                accessKey.isNotBlank() &&
                                secretKey.isNotBlank()
                            val missingExtensions = scanSettings.extensions.isEmpty()
                            val missingMatchers = !hasSelectedMatchersForScan(scanSettings)
                            if (missingExtensions || missingMatchers) {
                                onRequireScanSettings(missingExtensions, missingMatchers)
                                return@Button
                            }
                            if (!hasConnectionSettings) {
                                onRequireSourceInputs()
                                incorrectConnection = true
                                return@Button
                            }
                            if (normalizedPath.isEmpty()) {
                                onRequireSourceInputs()
                                selectPathError = true
                                return@Button
                            }
                            if (!validateAndShowError()) return@Button
                            path = normalizedPath
                            saveScreenState()
                            coroutineScope.launch {
                                val task = scanService.createTask(
                                    path = normalizedPath,
                                    extensions = scanSettings.extensions,
                                    matchers = scanSettings.matchers + scanSettings.userSignatures,
                                    fastScan = scanSettings.fastScan.value,
                                    connector = ConnectorS3(
                                        endpointStr = endpoint,
                                        accessKey = accessKey,
                                        secretKey = secretKey,
                                        bucketStr = bucket
                                    )
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
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .widthIn(min = blockMinWidth)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        isError: Boolean = false,
                        visualTransformation: VisualTransformation = VisualTransformation.None,
                        isPassword: Boolean = false
                    ) {
                        val shape = RoundedCornerShape(10.dp)
                        Surface(
                            modifier = modifier
                                .height(32.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isError) {
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                                    },
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
                                    visualTransformation = if (isPassword && !secretKeyVisible) PasswordVisualTransformation() else visualTransformation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = if (isPassword) 24.dp else 0.dp),
                                    decorationBox = { inner ->
                                        if (value.isEmpty()) {
                                            Text(placeholder, style = placeholderStyle, maxLines = 1)
                                        }
                                        inner()
                                    }
                                )
                                if (isPassword) {
                                    IconButton(
                                        onClick = { secretKeyVisible = !secretKeyVisible },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (secretKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = if (secretKeyVisible) "Hide secret key" else "Show secret key",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactField(
                            value = endpoint,
                            onValueChange = {
                                endpoint = it
                                selectedSavedConnectionKey = null
                                saveScreenState()
                                connectionTestOk = null
                                connectionTestMessage = null
                            },
                            placeholder = "Endpoint",
                            modifier = Modifier.weight(1.2f).widthIn(min = 180.dp),
                            isError = incorrectConnection && endpoint.isBlank()
                        )
                        CompactField(
                            value = bucket,
                            onValueChange = {
                                bucket = it
                                selectedSavedConnectionKey = null
                                saveScreenState()
                                connectionTestOk = null
                                connectionTestMessage = null
                            },
                            placeholder = "Bucket",
                            modifier = Modifier.weight(0.85f).widthIn(min = 120.dp),
                            isError = incorrectConnection && bucket.isBlank()
                        )
                        CompactField(
                            value = accessKey,
                            onValueChange = {
                                accessKey = it
                                selectedSavedConnectionKey = null
                                saveScreenState()
                                connectionTestOk = null
                                connectionTestMessage = null
                            },
                            placeholder = "Access key",
                            modifier = Modifier.weight(1f).widthIn(min = 140.dp),
                            isError = incorrectConnection && accessKey.isBlank()
                        )
                        CompactField(
                            value = secretKey,
                            onValueChange = {
                                secretKey = it
                                selectedSavedConnectionKey = null
                                saveScreenState()
                                connectionTestOk = null
                                connectionTestMessage = null
                            },
                            placeholder = "Secret key",
                            modifier = Modifier.weight(0.82f).widthIn(min = 112.dp),
                            isError = incorrectConnection && secretKey.isBlank(),
                            isPassword = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val canTest = endpoint.isNotBlank() && bucket.isNotBlank() && accessKey.isNotBlank() && secretKey.isNotBlank()
                        Button(
                            enabled = canTest && !isTestingConnection,
                            onClick = {
                                isTestingConnection = true
                                connectionTestOk = null
                                connectionTestMessage = null
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
                                        connectionTestMessage = s3ConnectionSuccessMessage
                                    }.onFailure {
                                        connectionTestOk = false
                                        connectionTestMessage = s3ConnectionErrorMessage
                                    }
                                    isTestingConnection = false
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = startScanButtonColors()
                        ) {
                            Text(
                                text = stringResource(Res.string.ScanSettings_PostgresTestConnection),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            enabled = canTest,
                            onClick = {
                                pendingConnectionName = defaultConnectionName()
                                pendingConnectionNameError = false
                                pendingConnectionNameDialog = true
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = stringResource(Res.string.ScanSettings_PostgresAddConnection),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = { savedConnectionsExpanded = true },
                            enabled = savedConnections.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.MainScreen_SavedConnections, savedConnections.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (savedConnections.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        connectionTestMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (connectionTestOk == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeleteConnection?.let { connToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteConnection = null },
            title = { Text(stringResource(Res.string.MainScreen_DeleteSavedConnectionTypeTitle, "S3")) },
            text = { Text(savedConnectionPrimary(connToDelete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeSavedConnection(connToDelete)
                        pendingDeleteConnection = null
                    }
                ) {
                    Text(stringResource(Res.string.ScanSettings_Profiles_Delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConnection = null }) {
                    Text(stringResource(Res.string.Common_Cancel))
                }
            }
        )
    }

    if (pendingConnectionNameDialog) {
        AlertDialog(
            onDismissRequest = { pendingConnectionNameDialog = false },
            title = { Text(stringResource(Res.string.MainScreen_ConnectionNameTitle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pendingConnectionName,
                        onValueChange = {
                            pendingConnectionName = it
                            if (pendingConnectionNameError && it.isNotBlank()) {
                                pendingConnectionNameError = false
                            }
                        },
                        placeholder = { Text(stringResource(Res.string.MainScreen_ConnectionNamePlaceholder)) },
                        singleLine = true,
                        isError = pendingConnectionNameError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pendingConnectionNameError) {
                        Text(
                            text = stringResource(Res.string.MainScreen_ConnectionNameRequired),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = pendingConnectionName.trim()
                        if (trimmed.isBlank()) {
                            pendingConnectionNameError = true
                        } else {
                            saveCurrentS3Connection(connectionName = trimmed)
                            pendingConnectionNameDialog = false
                        }
                    }
                ) {
                    Text(stringResource(Res.string.Common_Save))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConnectionNameDialog = false }) {
                    Text(stringResource(Res.string.Common_Cancel))
                }
            }
        )
    }

    if (savedConnectionsExpanded) {
        Dialog(onDismissRequest = { savedConnectionsExpanded = false }) {
            val dialogScroll = rememberScrollState()
            val cs = MaterialTheme.colorScheme
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 3.dp,
                color = cs.surfaceVariant,
                border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.56f)),
                modifier = Modifier.widthIn(min = 440.dp, max = 580.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.MainScreen_SavedConnections, savedConnections.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface
                        )
                        TextButton(
                            onClick = { savedConnectionsExpanded = false },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.Common_Cancel),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.34f))
                    if (savedConnections.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.MainScreen_NoSavedConnections),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(dialogScroll),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            savedConnections.forEachIndexed { index, conn ->
                                val isSelected = conn.connectionKey == selectedSavedConnectionKey
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) cs.primaryContainer.copy(alpha = 0.18f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            applySavedConnection(conn)
                                            savedConnectionsExpanded = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.75f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = savedConnectionPrimary(conn),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isSelected) cs.primary else cs.onSurface
                                        )
                                        Text(
                                            text = savedConnectionSecondary(conn),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = cs.onSurfaceVariant
                                        )
                                        Text(
                                            text = savedConnectionTertiary(conn).ifBlank { " " },
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = cs.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingDeleteConnection = conn },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Delete saved connection",
                                            tint = cs.onSurfaceVariant.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (index < savedConnections.lastIndex) {
                                    HorizontalDivider(
                                        color = cs.outlineVariant.copy(alpha = 0.24f),
                                        modifier = Modifier.padding(horizontal = 8.dp)
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

}