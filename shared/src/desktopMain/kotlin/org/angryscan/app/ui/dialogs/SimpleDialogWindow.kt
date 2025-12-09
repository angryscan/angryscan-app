package org.angryscan.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import org.angryscan.app.resources.MessageBox_OK
import org.angryscan.app.resources.Res
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun SimpleDialogWindow(
    onCloseRequest: () -> Unit,
    dialogState: DialogState = rememberDialogState(width = 600.dp, height = 450.dp),
    title: String,
    content: @Composable () -> Unit
) {
    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        transparent = true,
        undecorated = true,
        resizable = false
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            shape = DesktopWindowShapes(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Column {
                TitleBar(
                    windowPlacement = WindowPlacement.Floating
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPolice,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = title,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onCloseRequest
                        ) {
                            Text(stringResource(Res.string.MessageBox_OK))
                        }
                    }
                }
            }
        }
    }
}
