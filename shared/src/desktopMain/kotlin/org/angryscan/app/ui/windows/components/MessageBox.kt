package org.angryscan.app.ui.windows.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.di.PreviewModule
import org.angryscan.app.resources.MessageBox_Cancel
import org.angryscan.app.resources.MessageBox_OK
import org.angryscan.app.resources.Res

/**
 * A customizable message box dialog with accept and cancel buttons.
 *
 * @param title The title of the message box
 * @param message The message to display
 * @param onConfirm Called when the user clicks the accept button
 * @param onDismiss Called when the user clicks the cancel button
 */
@Composable
fun MessageBox(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState(
        width = 500.dp,
        height = 160.dp
    )

    DialogWindow(
        onCloseRequest = onDismiss,
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
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TitleBar(
                    windowPlacement = WindowPlacement.Floating
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
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
                        .fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(Res.string.MessageBox_Cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        elevation = null,
                    ) {
                        Text(stringResource(Res.string.MessageBox_OK))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MessageBoxPreview() {
    PreviewModule {
        MessageBox(
            title = "Confirm Action",
            message = "Are you sure you want to perform this action?",
            onConfirm = { },
            onDismiss = { },
        )
    }
}