package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.scan.common.connectors.S3File
import org.angryscan.app.scan.common.connectors.S3ViewModel
import org.angryscan.app.scan.common.connectors.S3ViewState
import org.angryscan.app.ui.extensions.toHumanReadable
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun S3FileChooser(
    onAccept: (s3File: S3File) -> Unit,
    onDecline: () -> Unit,
    connector: ConnectorS3
) {
    val dialogState: DialogState = rememberDialogState(
        width = 800.dp,
        height = 500.dp
    )

    val s3view: S3ViewModel = koinViewModel { parametersOf(connector, "") }
    val state by s3view.state.collectAsState()

    var path by remember { mutableStateOf("") }

    var selectedFile: S3File? by remember { mutableStateOf<S3File?>(null) }

    LaunchedEffect(state) {
        if (state is S3ViewState.Success) {
            path = (state as S3ViewState.Success).prefix
        } else {
            path = ""
        }
    }

    DialogWindow(
        onCloseRequest = onDecline,
        state = dialogState,
        transparent = true,
        undecorated = true,
        resizable = true
    ) {
        Surface(
            shape = DesktopWindowShapes(),
            color = MaterialTheme.colorScheme.surface,
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
                            .height(56.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )

                        Text(
                            text = connector.bucketStr,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (state is S3ViewState.Success) {
                            if (path.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        s3view.setDir(
                                            path.trim(s3view.connector.pathDelimiter)
                                                .let {
                                                    if (it.contains(s3view.connector.pathDelimiter)) {
                                                        it.substringBeforeLast(s3view.connector.pathDelimiter) +
                                                                s3view.connector.pathDelimiter
                                                    } else {
                                                        ""
                                                    }
                                                }
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowBackIosNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Text(
                                text = (state as S3ViewState.Success)
                                    .prefix
                                    .substringBeforeLast(s3view.connector.pathDelimiter) +
                                        s3view.connector.pathDelimiter,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                                lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state is S3ViewState.Success) {
                            items((state as S3ViewState.Success).files) { file ->
                                FileRow(
                                    file = file,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                selectedFile = file
                                            },
                                            onDoubleClick = {
                                                if (file.isDirectory) {
                                                    s3view.setDir(file.path)
                                                } else {
                                                    onAccept(file)
                                                }
                                            }
                                        ),
                                    selected = selectedFile == file,
                                )
                            }
                        } else {
                            item {
                                DirLoading()
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier
                                .width(180.dp),
                            onClick = {
                                if (selectedFile != null) {
                                    onAccept(selectedFile!!)
                                }
                            }
                        ) {
                            Text(text = "Выбрать")
                        }
                        OutlinedButton(
                            modifier = Modifier
                                .width(180.dp),
                            onClick = onDecline
                        ) {
                            Text(text = "Отмена")
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun FileRow(
    file: S3File,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = file.name,
            modifier = Modifier
                .weight(1f)
        )
        if (!file.isDirectory) {
            Text(
                text = file.size.toHumanReadable(),
                modifier = Modifier
                    .width(60.dp)
            )
        }
    }
}

@Composable
fun DirLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        CircularProgressIndicator()
    }
}