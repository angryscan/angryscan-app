package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.MainScreen_SelectPathPlaceholder
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.windows.components.RadioButtonNavigation
import org.angryscan.app.ui.windows.screens.main.components.S3FileChooser
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.settings.SettingsButton

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
            trailingIcon = {
                Row {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(
                                MaterialTheme.shapes.large
                            )
                            .background(MaterialTheme.colorScheme.onBackground)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                if (endpoint.isNotEmpty() &&
                                    accessKey.isNotEmpty() &&
                                    secretKey.isNotEmpty() &&
                                    bucket.isNotEmpty()
                                ) {
                                    selectPathDialog = true
                                } else {
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .height(60.dp)
                    .width(340.dp),
                value = endpoint,
                onValueChange = { endpoint = it },
                placeholder = { Text(text = "Endpoint") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                isError = endpointError
            )
            OutlinedTextField(
                modifier = Modifier
                    .height(60.dp)
                    .width(340.dp),
                value = bucket,
                onValueChange = { bucket = it },
                placeholder = { Text(text = "Bucket") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                isError = bucketError
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .height(60.dp)
                    .width(340.dp),
                value = accessKey,
                onValueChange = { accessKey = it },
                placeholder = { Text(text = "Access key") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                isError = accessKeyError
            )
            OutlinedTextField(
                modifier = Modifier
                    .height(60.dp)
                    .width(340.dp),
                value = secretKey,
                onValueChange = { secretKey = it },
                placeholder = { Text(text = "Secret key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = MaterialTheme.shapes.medium,
                isError = secretKeyError
            )
        }

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
                        if (endpoint.isNotEmpty() &&
                            accessKey.isNotEmpty() &&
                            secretKey.isNotEmpty() &&
                            bucket.isNotEmpty()
                        ) {
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