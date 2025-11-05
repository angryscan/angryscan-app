package org.angryscan.app.ui.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.*
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar

@Composable
fun EulaDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    dialogState: DialogState = DialogState()
) {

    val scrollState = rememberScrollState()

    DialogWindow(
        onCloseRequest = onDecline,
        state = dialogState,
        transparent = true,
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPolice,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(Res.string.eula_title),
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
                        .padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = stringResource(Res.string.eula_text),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .padding(start = 8.dp, end = 20.dp)
                            )
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp)
                                .height(400.dp)
                                .width(10.dp),
                            style = LocalScrollbarStyle.current.copy(
                                hoverColor = MaterialTheme.colorScheme.primary,
                                unhoverColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .width(180.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.eula_decline),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp,
                                lineHeight = 16.sp
                            )
                        }
                        OutlinedButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .width(180.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.eula_accept),
                                fontSize = 16.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

            }
        }
    }
}