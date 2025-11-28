package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.engine.fallback
import org.angryscan.app.scan.engine.getEngine
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar
import org.angryscan.common.engine.IMask
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.File

@Composable
fun AttributeLocationWindow(
    filePath: String,
    attribute: IMatcher,
    onClose: (allMasked: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val locations = remember { mutableStateListOf<Location>() }

    var searching by remember { mutableStateOf(false) }

    var errorSearching by remember { mutableStateOf(false) }

    var failedToFind by remember { mutableStateOf(false) }

    val scanSettings = koinInject<ScanSettings>()

    val selectedLocations = remember { mutableStateListOf<Location>() }

    val fileType = IFileType.getFileType(filePath)
    val maskingSupported = fileType?.let { LocationFinder.isMaskSupported(it) && attribute is IMask } ?: false
    val exportSupported = fileType?.let { LocationFinder.isExportSupported(it) } ?: false

    coroutineScope.launch {
        searching = true
        var engine = scanSettings.engine.value.getEngine(listOf(attribute))
        while (engine.matchers.isEmpty()) {
            engine = engine.fallback().getEngine(listOf(attribute))

            if (engine::class == scanSettings.engine) {
                onClose(false)
                errorSearching = true
                searching = false
                return@launch
            }
        }

        try {
            locations.addAll(
                LocationFinder.findLocations(
                    filePath,
                    engine,
                    attribute
                )
            )
            selectedLocations.addAll(
                locations
                    .filter {
                        it.entry.matcher is IMask ||
                                exportSupported
                    }
            )
            if (locations.isEmpty())
                failedToFind = true
        } catch (_: Exception) {
            errorSearching = true
        }
        searching = false
    }


    val state = rememberDialogState(
        width = 800.dp,
        height = 500.dp
    )

    val scrollState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    var working by remember { mutableStateOf(false) }

    val dialogSettings = createDialogSettings()

    val saveLauncher = rememberFileSaverLauncher(
        dialogSettings = dialogSettings
    ) { file ->
        if (file != null) {
            coroutineScope.launch {
                try {
                    val rows = LocationFinder.exportRows(filePath, selectedLocations, file.path)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            getString(
                                Res.string.LocationWindow_ExportRowsCount,
                                rows
                            )
                        )
                    }
                } finally {
                    working = false
                }
            }
        }

    }

    DialogWindow(
        onCloseRequest = { onClose(!searching && !errorSearching && !failedToFind && locations.isEmpty()) },
        state = state,
        undecorated = true,
        resizable = false
    ) {
        Surface(
            shape = DesktopWindowShapes(),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TitleBar(
                    windowPlacement = WindowPlacement.Floating
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(Res.string.LocationWindow_Title, attribute.composableName()),
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { onClose(!searching && !errorSearching && !failedToFind && locations.isEmpty()) }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = filePath,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable {
                                try {
                                    Desktop.getDesktop().open(File(filePath))
                                } catch (_: Exception) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            getString(
                                                Res.string.Error_FileOpen
                                            )
                                        )
                                    }
                                }
                            }
                            .padding(4.dp)
                    )
                }
                if (errorSearching) {
                    Text(
                        text = stringResource(Res.string.LocationWindow_Error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    if (searching) {
                        CircularProgressIndicator()
                    } else {
                        if (failedToFind) {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = stringResource(Res.string.LocationWindow_NotFound),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Scaffold(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface),
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row {
                                            AnimatedVisibility(
                                                locations.any {
                                                    it.entry.matcher is IMask
                                                } || exportSupported
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.small)
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                        .clickable {
                                                            if (
                                                                selectedLocations.containsAll(locations)
                                                            ) {
                                                                selectedLocations.clear()
                                                            } else {
                                                                selectedLocations.addAll(
                                                                    locations
                                                                        .filter { !selectedLocations.contains(it) }
                                                                )
                                                            }
                                                        }
                                                        .padding(6.dp),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (selectedLocations.containsAll(locations))
                                                                Icons.Outlined.CheckBox
                                                            else
                                                                Icons.Outlined.CheckBoxOutlineBlank,
                                                            contentDescription = null,
                                                        )
                                                        Text(
                                                            text = stringResource(
                                                                Res.string.LocationWindow_SelectAllButton
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AnimatedVisibility(
                                                selectedLocations.isNotEmpty() &&
                                                        exportSupported &&
                                                        !working
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.small)
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                        .clickable {
                                                            if (!working) {
                                                                working = true
                                                                saveLauncher.launch(
                                                                    suggestedName = "${File(filePath).name}_Rows",
                                                                    extension = "csv",
                                                                    directory = PlatformFile(AppFiles.UserDirPath)
                                                                )
                                                            }
                                                        }
                                                        .padding(6.dp),
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .padding(horizontal = 10.dp)
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.LocationWindow_ExportRows)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Outlined.Download,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                            AnimatedVisibility(
                                                selectedLocations.any {
                                                    it.entry.matcher is IMask
                                                } &&
                                                        maskingSupported &&
                                                        !working
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.small)
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                        .clickable {
                                                            if (!working) {
                                                                working = true
                                                                coroutineScope.launch {
                                                                    val maskedCount =
                                                                        LocationFinder.maskLocations(
                                                                            filePath,
                                                                            selectedLocations
                                                                        )
                                                                    if (maskedCount == selectedLocations.size) {
                                                                        locations.removeAll(selectedLocations)
                                                                        selectedLocations.clear()
                                                                        coroutineScope.launch {
                                                                            snackbarHostState.showSnackbar(
                                                                                getString(
                                                                                    Res.string.LocationWindow_MaskedCount,
                                                                                    maskedCount
                                                                                )
                                                                            )
                                                                        }
                                                                    } else {
                                                                        coroutineScope.launch {
                                                                            snackbarHostState.showSnackbar(
                                                                                getString(
                                                                                    Res.string.LocationWindow_MaskError
                                                                                )
                                                                            )
                                                                        }
                                                                    }
                                                                    working = false
                                                                }
                                                            }
                                                        }
                                                        .padding(6.dp),
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .padding(horizontal = 10.dp)
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.LocationWindow_MaskButton)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Outlined.VisibilityOff,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Box {
                                        Column(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            LazyColumn(
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(
                                                        start = 8.dp,
                                                        end = if (scrollState.canScrollBackward || scrollState.canScrollForward) 20.dp else 8.dp
                                                    ),
                                                state = scrollState
                                            ) {
                                                items(locations) { location ->
                                                    AttributeLocationItem(
                                                        location = location,
                                                        selectable = maskingSupported || exportSupported,
                                                        checked = selectedLocations.contains(location),
                                                        onCheckedChanged = { state ->
                                                            if (state) {
                                                                selectedLocations.add(location)
                                                            } else {
                                                                selectedLocations.remove(location)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        VerticalScrollbar(
                                            adapter = rememberScrollbarAdapter(scrollState),
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 6.dp)
                                                .width(10.dp),
                                            style = LocalScrollbarStyle.current.copy(
                                                hoverColor = MaterialTheme.colorScheme.primary,
                                                unhoverColor = MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}