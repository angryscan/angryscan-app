package org.angryscan.app.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource
import java.awt.datatransfer.StringSelection

@Composable
fun ContactDialog(
    onCloseRequest: () -> Unit,
    dialogState: DialogState = rememberDialogState(width = 600.dp, height = 450.dp)
) {
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    SimpleDialogWindow(
        onCloseRequest = onCloseRequest,
        dialogState = dialogState,
        snackbarHostState = snackbarHostState,
        title = stringResource(Res.string.ContactDialog_Title)
    ) {
        val uriHandler = LocalUriHandler.current
        val clipboard = LocalClipboard.current

        val websiteUrl = stringResource(Res.string.ContactDialog_WebsiteUrl)
        val email = stringResource(Res.string.ContactDialog_Email)
        val websiteLabel = stringResource(Res.string.ContactDialog_WebsiteLabel)
        val emailLabel = stringResource(Res.string.ContactDialog_EmailLabel)
        val copiedMessage = stringResource(Res.string.ScanResultScreen_ClipboardCopiedMessage)

        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(30.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = websiteLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = emailLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = websiteUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { uriHandler.openUri(websiteUrl) }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { uriHandler.openUri("mailto:$email") }
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CopyIconButton {
                            coroutineScope.launch {
                                clipboard.setClipEntry(ClipEntry(StringSelection(websiteUrl)))
                                snackbarHostState.showSnackbar(copiedMessage)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CopyIconButton {
                            coroutineScope.launch {
                                clipboard.setClipEntry(ClipEntry(StringSelection(email)))
                                snackbarHostState.showSnackbar(copiedMessage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyIconButton(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Outlined.CopyAll,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
