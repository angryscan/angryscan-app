package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.MainScreen_SelectPathPlaceholder
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.S3Screen_Tooltip_ConnectionSettings
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
private fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = textStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun S3Screen(
    navController: androidx.navigation.NavController,
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {}
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

    var endpointError by remember { mutableStateOf(false) }
    var accessKeyError by remember { mutableStateOf(false) }
    var secretKeyError by remember { mutableStateOf(false) }
    var bucketError by remember { mutableStateOf(false) }
    
    val (validationErrorDialog, validateAndShowError, dismissValidationError) = rememberScanValidation(scanSettings)

    LaunchedEffect(scanNotCorrectPath, incorrectConnection) {
        if (scanNotCorrectPath || incorrectConnection) {
            if (incorrectPathError)
                selectPathError = true
            if (endpoint.isEmpty())
                endpointError = true
            if (accessKey.isEmpty())
                accessKeyError = true
            if (secretKey.isEmpty())
                secretKeyError = true
            if (bucket.isEmpty())
                bucketError = true
            delay(200)

            selectPathError = false
            endpointError = false
            accessKeyError = false
            secretKeyError = false
            bucketError = false
            delay(400)

            if (incorrectPathError)
                selectPathError = true
            if (endpoint.isEmpty())
                endpointError = true
            if (accessKey.isEmpty())
                accessKeyError = true
            if (secretKey.isEmpty())
                secretKeyError = true
            if (bucket.isEmpty())
                bucketError = true
            delay(200)

            selectPathError = false
            endpointError = false
            accessKeyError = false
            secretKeyError = false
            bucketError = false
            delay(400)

            if (incorrectPathError)
                selectPathError = true
            if (endpoint.isEmpty())
                endpointError = true
            if (accessKey.isEmpty())
                accessKeyError = true
            if (secretKey.isEmpty())
                secretKeyError = true
            if (bucket.isEmpty())
                bucketError = true
            delay(200)

            selectPathError = false
            endpointError = false
            accessKeyError = false
            secretKeyError = false
            bucketError = false
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
    var connectionSettingsExpanded by remember { mutableStateOf(screenStateSettings.s3ScreenState.connectionSettingsExpanded) }
    
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
            screenStateSettings.s3ScreenState.connectionSettingsExpanded = connectionSettingsExpanded
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

    setSidebarContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 16.dp, 8.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it; saveScreenState() },
                            modifier = Modifier.weight(1f).height(80.dp),
                            placeholder = { Text(stringResource(Res.string.MainScreen_SelectPathPlaceholder), style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            isError = selectPathError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        )
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        DescriptionTooltip(description = stringResource(Res.string.S3Screen_Tooltip_ConnectionSettings)) {
                            IconButton(
                                onClick = { connectionSettingsExpanded = !connectionSettingsExpanded; saveScreenState() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (connectionSettingsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (endpoint.isNotEmpty() && accessKey.isNotEmpty() && secretKey.isNotEmpty() && bucket.isNotEmpty()) {
                                    selectPathDialog = true
                                } else {
                                    if (!connectionSettingsExpanded) { connectionSettingsExpanded = true; saveScreenState() }
                                    incorrectConnection = true
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    AnimatedVisibility(
                        visible = connectionSettingsExpanded,
                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CustomOutlinedTextField(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                value = endpoint,
                                onValueChange = { endpoint = it; if (it.isNotEmpty()) endpointError = false; saveScreenState() },
                                placeholder = "Endpoint",
                                isError = endpointError,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            CustomOutlinedTextField(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                value = bucket,
                                onValueChange = { bucket = it; if (it.isNotEmpty()) bucketError = false; saveScreenState() },
                                placeholder = "Bucket",
                                isError = bucketError,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            CustomOutlinedTextField(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                value = accessKey,
                                onValueChange = { accessKey = it; if (it.isNotEmpty()) accessKeyError = false; saveScreenState() },
                                placeholder = "Access key",
                                isError = accessKeyError,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            CustomOutlinedTextField(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                value = secretKey,
                                onValueChange = { secretKey = it; if (it.isNotEmpty()) secretKeyError = false; saveScreenState() },
                                placeholder = "Secret key",
                                isError = secretKeyError,
                                visualTransformation = PasswordVisualTransformation(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    if (endpoint.isEmpty() || accessKey.isEmpty() || secretKey.isEmpty() || bucket.isEmpty()) {
                        if (!connectionSettingsExpanded) { connectionSettingsExpanded = true; saveScreenState() }
                        incorrectConnection = true
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
                            connector = ConnectorS3(endpointStr = endpoint, accessKey = accessKey, secretKey = secretKey, bucketStr = bucket)
                        )
                        scanService.startTask(task)
                        task.id.value?.let { expandScanState(it) }
                    }
                },
                modifier = ScanButtonModifier(
                    isReady = path.isNotEmpty() && endpoint.isNotEmpty() && accessKey.isNotEmpty() && secretKey.isNotEmpty() && bucket.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(text = stringResource(Res.string.MainScreen_ScanStartButton), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Validation error dialog
    ScanValidationErrorDialog(
        validationError = validationErrorDialog,
        onDismiss = dismissValidationError
    )
}