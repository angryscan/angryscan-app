package org.angryscan.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import org.angryscan.app.scan.common.mainWindow
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar

@Composable
fun DesktopAlertDialog(
    onCloseRequest: () -> Unit,
    title: String,
    message: String,
    dialogSettings: DialogWindowSettings = DialogWindowSettings()
) {
    // Calculate center position relative to main window
    val centerPosition = remember {
        try {
            val mainWindowBounds = mainWindow.bounds
            val dialogWidth = dialogSettings.width.value.toInt()
            val dialogHeight = dialogSettings.height.value.toInt()
            
            val centerX = mainWindowBounds.x + (mainWindowBounds.width - dialogWidth) / 2
            val centerY = mainWindowBounds.y + (mainWindowBounds.height - dialogHeight) / 2
            
            WindowPosition.Absolute(x = centerX.dp, y = centerY.dp)
        } catch (e: UninitializedPropertyAccessException) {
            WindowPosition.PlatformDefault // Use default position if mainWindow not initialized
        }
    }

    val state = rememberDialogState(
        width = dialogSettings.width,
        height = dialogSettings.height,
        position = centerPosition
    )

    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = state,
        transparent = true,
        undecorated = true,
        resizable = false
    ) {
        Surface(
            shape = DesktopWindowShapes(),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = title,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = message,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    )
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onCloseRequest,
                        ) {
                            Text(text = "OK")
                        }
                    }
                }
            }
        }
    }
}

data class DialogWindowSettings(
    val width: Dp = 400.dp,
    val height: Dp = 200.dp,
)