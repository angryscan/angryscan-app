package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.resources.*
import org.angryscan.app.ui.windows.screens.main.settings.items.SelectAllOrDiscardAllText
import org.angryscan.common.matchers.UserSignature
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import javax.swing.JOptionPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxUserSignature(
    scanSettings: ScanSettings,
    showTitle: Boolean = true,
    unifiedBlock: Boolean = false,
    modifier: Modifier = Modifier
) {
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

    val content: @Composable ColumnScope.() -> Unit = {
        for (signature in userSignatures) {
            val selected = selectedSignatures.contains(signature)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = signature.name,
                    modifier = Modifier.width(SettingsScanTable.groupLabelWidth),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedSignatures.remove(signature)
                            else selectedSignatures.add(signature)
                            scanSettings.save()
                        },
                        modifier = Modifier.height(24.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        ),
                        label = { Text(text = signature.name, fontSize = 10.sp) }
                    )
                    IconButton(
                        onClick = { editedUserSignature = signature; userSignatureEditorVisibility = true },
                        modifier = Modifier.size(28.dp)
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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                    }
                }
            }
        }
    }
    if (showTitle) {
        val trailing: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userSignatures.isNotEmpty()) {
                    SelectAllOrDiscardAllText(
                        allSelected = selectedSignatures.containsAll(userSignatures),
                        onClick = {
                            if (selectedSignatures.containsAll(userSignatures))
                                selectedSignatures.clear()
                            else
                                selectedSignatures.addAll(userSignatures.filter { it !in selectedSignatures })
                            scanSettings.save()
                        }
                    )
                }
                IconButton(onClick = { editedUserSignature = null; userSignatureEditorVisibility = true }) {
                    Icon(painterResource(Res.drawable.ScanSettings_Add), contentDescription = null)
                }
            }
        }
        if (unifiedBlock) {
            SettingsUnifiedSubsection(
                title = stringResource(Res.string.ScanSettings_UserSignatures),
                modifier = modifier,
                titleTrailing = trailing,
                content = content
            )
        } else {
            SettingsSectionCard(
                title = stringResource(Res.string.ScanSettings_UserSignatures),
                modifier = modifier,
                titleTrailing = trailing,
                content = content
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { editedUserSignature = null; userSignatureEditorVisibility = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(painterResource(Res.drawable.ScanSettings_Add), contentDescription = null)
                }
            }
            content()
        }
    }
}
