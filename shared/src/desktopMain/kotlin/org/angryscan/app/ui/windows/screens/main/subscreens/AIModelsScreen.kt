package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import io.modelaudit.scanFolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.ui.windows.components.RadioButtonNavigation
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.settings.SettingsButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.io.File

@Composable
fun AIModelsScreen(
    navController: androidx.navigation.NavController,
    settingsExpanded: Boolean,
    expandSettings: () -> Unit,
    hideSettings: () -> Unit,
    expandScanState: (Int) -> Unit
) {
    val screenStateSettings = koinInject<ScreenStateSettings>()
    var path by remember { mutableStateOf(screenStateSettings.aimodelsScreenState.path) }

    val settingsButtonTransition = updateTransition(settingsExpanded)
    val settingsBoxTransition = updateTransition(settingsExpanded)

    val coroutineScope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun saveScreenState() {
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(500)
            screenStateSettings.aimodelsScreenState.path = path
            screenStateSettings.save()
        }
    }

    var selectPathError by remember { mutableStateOf(false) }
    var scanNotCorrectPath by remember { mutableStateOf(false) }
    var scanInProgress by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnAIModelsScreen = backStackEntry?.destination?.hasRoute(MainScreenConnector.AIModels::class) == true

    var hasLoadedAIModelsSettings by remember { mutableStateOf(false) }

    LaunchedEffect(isOnAIModelsScreen) {
        if (isOnAIModelsScreen && !hasLoadedAIModelsSettings) {
            if (screenStateSettings.aimodelsScreenState.path.isNotEmpty()) {
                path = screenStateSettings.aimodelsScreenState.path
            }
            hasLoadedAIModelsSettings = true
        } else if (!isOnAIModelsScreen) {
            hasLoadedAIModelsSettings = false
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

    val folderPicker = rememberDirectoryPickerLauncher(
        dialogSettings = createDialogSettings(),
        title = stringResource(Res.string.MainScreen_FolderPickerTitle)
    ) { dir ->
        if (dir != null) {
            path = dir.path
            saveScreenState()
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
                saveScreenState()
            },
            placeholder = {
                Text(text = stringResource(Res.string.MainScreen_SelectPathPlaceholder))
            },
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
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(
                            MaterialTheme.shapes.large.copy(
                                topEnd = CornerSize(0.dp),
                                bottomEnd = CornerSize(0.dp)
                            )
                        )
                        .background(MaterialTheme.colorScheme.onBackground)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { folderPicker.launch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background
                    )
                }
            }
        )

        Box(
            modifier = Modifier
                .width(700.dp)
                .padding(vertical = 0.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            RadioButtonNavigation(navController = navController)
        }

        Row {
            Button(
                onClick = {
                    if (path.isBlank()) {
                        scanNotCorrectPath = true
                        return@Button
                    }
                    if (!File(path).exists()) {
                        scanNotCorrectPath = true
                        return@Button
                    }
                    saveScreenState()
                    scanInProgress = true
                    scanResult = null
                    coroutineScope.launch {
                        try {
                            val result = scanFolder(path)
                            scanResult = result
                        } finally {
                            scanInProgress = false
                        }
                    }
                },
                modifier = Modifier
                    .width(268.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium.copy(
                    topEnd = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                ),
                enabled = !scanInProgress
            ) {
                Text(
                    text = if (scanInProgress) "..." else stringResource(Res.string.MainScreen_ScanStartButton),
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
            SettingsBox(transition = settingsBoxTransition)
        }
    }

    scanResult?.let { result ->
        AlertDialog(
            onDismissRequest = { scanResult = null },
            title = { Text("AI Models scan result") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    SelectionContainer {
                        Text(
                            text = result,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { scanResult = null }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }
}
