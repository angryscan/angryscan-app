package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.MainScreen_SelectPathPlaceholder
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.S3Screen_Tooltip_ConnectionSettings
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.components.RadioButtonNavigation
import org.angryscan.app.ui.windows.screens.main.components.S3FileChooser
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.settings.SettingsButton
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
    settingsExpanded: Boolean,
    expandSettings: () -> Unit,
    hideSettings: () -> Unit,
    expandScanState: (Int) -> Unit
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(helperPath) }
    var endpoint by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var bucket by remember { mutableStateOf("") }

    val settingsButtonTransition = updateTransition(settingsExpanded)

    val settingsBoxTransition = updateTransition(settingsExpanded)


    val coroutineScope = rememberCoroutineScope()

    var selectPathError by remember { mutableStateOf(false) }

    var scanNotCorrectPath by remember { mutableStateOf(false) }
    var incorrectConnection by remember { mutableStateOf(false) }
    var incorrectPathError by remember { mutableStateOf(false) }

    var endpointError by remember { mutableStateOf(false) }
    var accessKeyError by remember { mutableStateOf(false) }
    var secretKeyError by remember { mutableStateOf(false) }
    var bucketError by remember { mutableStateOf(false) }

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
            if (focusRequested)
                ScanPathHelper.resetFocus()
        }
    }

    var selectPathDialog by remember { mutableStateOf(false) }
    var connectionSettingsExpanded by remember { mutableStateOf(false) }

    if (selectPathDialog) {
        S3FileChooser(
            onAccept = {
                path = it.path
                selectPathDialog = false
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
            onValueChange = { path = it },
            placeholder = { Text(text = stringResource(Res.string.MainScreen_SelectPathPlaceholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = selectPathError,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .height(40.dp)
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
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isHovered) 1.1f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "scale"
                    )
                    
                    DescriptionTooltip(
                        description = stringResource(Res.string.S3Screen_Tooltip_ConnectionSettings)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    when {
                                        connectionSettingsExpanded -> 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        isHovered -> 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else -> 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { connectionSettingsExpanded = !connectionSettingsExpanded }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = "Connection settings",
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(scale),
                                tint = when {
                                    connectionSettingsExpanded -> MaterialTheme.colorScheme.primary
                                    isHovered -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(
                                MaterialTheme.shapes.large
                            )
                            .background(MaterialTheme.colorScheme.onBackground)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                val areFieldsFilled = endpoint.isNotEmpty() &&
                                    accessKey.isNotEmpty() &&
                                    secretKey.isNotEmpty() &&
                                    bucket.isNotEmpty()
                                
                                if (areFieldsFilled) {
                                    selectPathDialog = true
                                } else {
                                    // Если поля не заполнены и блок не раскрыт, раскрыть его
                                    if (!connectionSettingsExpanded) {
                                        connectionSettingsExpanded = true
                                    }
                                    // Активировать моргающую красную обводку через LaunchedEffect
                                    incorrectConnection = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
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
        
        AnimatedVisibility(
            visible = connectionSettingsExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .width(700.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomOutlinedTextField(
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f),
                        value = endpoint,
                        onValueChange = { 
                            endpoint = it
                            if (it.isNotEmpty()) endpointError = false
                        },
                        placeholder = "Endpoint",
                        isError = endpointError,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 14.sp
                        )
                    )
                    CustomOutlinedTextField(
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f),
                        value = bucket,
                        onValueChange = { 
                            bucket = it
                            if (it.isNotEmpty()) bucketError = false
                        },
                        placeholder = "Bucket",
                        isError = bucketError,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomOutlinedTextField(
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f),
                        value = accessKey,
                        onValueChange = { 
                            accessKey = it
                            if (it.isNotEmpty()) accessKeyError = false
                        },
                        placeholder = "Access key",
                        isError = accessKeyError,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 14.sp
                        )
                    )
                    CustomOutlinedTextField(
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f),
                        value = secretKey,
                        onValueChange = { 
                            secretKey = it
                            if (it.isNotEmpty()) secretKeyError = false
                        },
                        placeholder = "Secret key",
                        isError = secretKeyError,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }

        Row {
                Button(
                    onClick = {
                        val areFieldsFilled = endpoint.isNotEmpty() &&
                            accessKey.isNotEmpty() &&
                            secretKey.isNotEmpty() &&
                            bucket.isNotEmpty()
                        
                        if (areFieldsFilled) {
                            coroutineScope.launch {
                                val task = scanService.createTask(
                                    path = path,
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
                                task.id.value?.let { taskId ->
                                    expandScanState(taskId)
                                }

                            }
                        } else {
                            // Если поля не заполнены и блок не раскрыт, раскрыть его
                            if (!connectionSettingsExpanded) {
                                connectionSettingsExpanded = true
                            }
                            // Активировать моргающую красную обводку через LaunchedEffect
                            incorrectConnection = true
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