package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder
import org.angryscan.app.scan.engine.fallback
import org.angryscan.app.scan.engine.getEngine
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar
import java.awt.Desktop
import java.io.File

@Composable
fun AttributeLocationWindow(
    filePath: String,
    attribute: IMatcher,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }

    var searching by remember { mutableStateOf(false) }

    var errorSearching by remember { mutableStateOf(false) }

    var failedToFind by remember { mutableStateOf(false) }

    val scanSettings = koinInject<ScanSettings>()


    coroutineScope.launch {
        searching = true
        var engine = scanSettings.engine.value.getEngine(listOf(attribute))
        while (engine.matchers.isEmpty()) {
            engine = engine.fallback().getEngine(listOf(attribute))

            if (engine::class == scanSettings.engine) {
                onClose()
                errorSearching = true
                searching = false
                return@launch
            }
        }

        try {
            locations = LocationFinder.findLocations(
                filePath,
                engine,
                attribute
            )
            if (locations.isEmpty())
                failedToFind = true
        } catch (_: Exception) {
            errorSearching = true
        }
        searching = false
    }


    val state = rememberDialogState(
        width = 700.dp,
        height = 500.dp
    )

    val scrollState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    DialogWindow(
        onCloseRequest = onClose,
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
                                onClick = onClose
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
                                    .fillMaxSize(),
                                snackbarHost = { SnackbarHost(snackbarHostState) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .background(color = MaterialTheme.colorScheme.surface)
                                        .padding(vertical = 8.dp)
                                ) {

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .padding(
                                                start = 8.dp,
                                                end = if (scrollState.canScrollBackward || scrollState.canScrollForward) 20.dp else 8.dp
                                            ),
                                        state = scrollState
                                    ) {
                                        items(locations) { location ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .weight(0.8f)
                                                        .fillMaxWidth(),
                                                ) {
                                                    Text(
                                                        text = location.entry.before,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Text(
                                                        text = location.entry.value,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = location.entry.after,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                Text(
                                                    modifier = Modifier
                                                        .weight(0.2f)
                                                        .fillMaxWidth(),
                                                    text = location.location,
                                                    style = MaterialTheme.typography.bodySmall
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