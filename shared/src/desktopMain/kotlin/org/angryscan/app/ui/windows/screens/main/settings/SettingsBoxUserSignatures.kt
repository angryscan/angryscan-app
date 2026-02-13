package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.resources.*
import org.angryscan.common.matchers.UserSignature
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import javax.swing.JOptionPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxUserSignature(scanSettings: ScanSettings) {
    val userSignatureSettings = koinInject<UserSignatureSettings>()
    var userSignatureEditorVisibility by remember { mutableStateOf(false) }
    val userSignatures = remember { userSignatureSettings.userSignatures }
    val selectedSignatures = remember { scanSettings.userSignatures }
    val coroutineScope = rememberCoroutineScope()
    var editedUserSignature by remember { mutableStateOf<UserSignature?>(null) }

    if (userSignatureEditorVisibility) {
        UserSignatureEditor(
            onCloseRequest = { userSignatureEditorVisibility = false },
            onSaveRequest = { signature ->
                if (scanSettings.userSignatures.any { it.name == signature.name } && editedUserSignature == null) {
                    coroutineScope.launch {
                        JOptionPane.showConfirmDialog(
                            null,
                            getString(Res.string.Matcher_UserSignature_ErrorMessage),
                            getString(Res.string.Matcher_UserSignature_Title),
                            JOptionPane.YES_OPTION,
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                } else {
                    if (editedUserSignature != null) {
                        val selected = editedUserSignature in scanSettings.userSignatures
                        scanSettings.userSignatures.remove(editedUserSignature)
                        scanSettings.userSignatures.removeIf { it.name == signature.name }
                        val idx = userSignatureSettings.userSignatures.indexOf(editedUserSignature)
                        if (idx >= 0) userSignatureSettings.userSignatures[idx] = signature
                        if (selected) scanSettings.userSignatures.add(signature)
                    } else {
                        userSignatureSettings.userSignatures.add(signature)
                        scanSettings.userSignatures.add(signature)
                    }
                    scanSettings.userSignatures.removeIf { it !in userSignatureSettings.userSignatures }
                    userSignatureSettings.save()
                    userSignatureEditorVisibility = false
                    scanSettings.save()
                    editedUserSignature = null
                }
            },
            userSignature = editedUserSignature
        )
    }

    SettingsSectionCard(
        title = stringResource(Res.string.ScanSettings_UserSignatures),
        titleTrailing = {
            IconButton(onClick = { editedUserSignature = null; userSignatureEditorVisibility = true }) {
                Icon(painterResource(Res.drawable.ScanSettings_Add), contentDescription = null)
            }
        }
    ) {
        if (userSignatures.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        if (selectedSignatures.containsAll(userSignatures))
                            selectedSignatures.clear()
                        else
                            selectedSignatures.addAll(userSignatures.filter { it !in selectedSignatures })
                        scanSettings.save()
                    }
                ) {
                    Text(
                        text = if (selectedSignatures.containsAll(userSignatures)) stringResource(Res.string.ScanSettings_DeselectAll) else stringResource(Res.string.ScanSettings_SelectAll),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        for (signature in userSignatures) {
            val selected = selectedSignatures.contains(signature)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) selectedSignatures.remove(signature)
                        else selectedSignatures.add(signature)
                        scanSettings.save()
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = signature.name, fontSize = 13.sp) }
                )
                IconButton(
                    onClick = { editedUserSignature = signature; userSignatureEditorVisibility = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                }
                IconButton(
                    onClick = {
                        scanSettings.userSignatures.remove(signature)
                        userSignatureSettings.userSignatures.remove(signature)
                        scanSettings.save()
                        userSignatureSettings.save()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                }
            }
        }
    }
}
