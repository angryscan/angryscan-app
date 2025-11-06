package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.common.matchers.UserSignature
import javax.swing.JOptionPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxUserSignature(scanSettings: ScanSettings) {

    var expanded by remember { scanSettings.userSignatureSettingsExpanded }

    val userSignatureSettings = koinInject<UserSignatureSettings>()

    var userSignatureEditorVisibility by remember { mutableStateOf(false) }
    val userSignatures = remember { userSignatureSettings.userSignatures }
    val selectedSignatures = remember { scanSettings.userSignatures }

    val coroutineScope = rememberCoroutineScope()

    var editedUserSignature by remember { mutableStateOf<UserSignature?>(null) }

    if (userSignatureEditorVisibility) {
        UserSignatureEditor(
            onCloseRequest = {
                userSignatureEditorVisibility = false
            },
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

                        val index = userSignatureSettings.userSignatures.mapIndexed { index, us -> us to index }
                            .first { it.first.name == signature.name }.second
                        userSignatureSettings.userSignatures[index] = signature

                        if (selected)
                            scanSettings.userSignatures.add(signature)
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


    SettingsBoxSpan(
        text = stringResource(Res.string.ScanSettings_UserSignatures),
        expanded = expanded,
        onExpandClick = {
            expanded = !expanded
            scanSettings.save()
        },
        textTail = {
            IconButton(
                onClick = {
                    editedUserSignature = null
                    userSignatureEditorVisibility = true
                }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ScanSettings_Add),
                    contentDescription = null
                )
            }
        }
    ) {
        val rows = FileType.entries.size / 3 + if (FileType.entries.size % 3 > 0) 1 else 0

        val height = (24 * rows + (6 * (rows - 1))).dp + 52.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = userSignatures.isNotEmpty(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Checkbox(
                            checked = selectedSignatures.containsAll(userSignatures) && userSignatures.isNotEmpty(),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedSignatures.addAll(userSignatures.filter {
                                        !selectedSignatures.contains(
                                            it
                                        )
                                    })
                                } else {
                                    selectedSignatures.clear()
                                }

                                scanSettings.save()
                            }
                        )
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                            Text(
                                text = stringResource(Res.string.ScanSettings_SelectAll),
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    if (!selectedSignatures.containsAll(userSignatures))
                                        selectedSignatures.addAll(userSignatures.filter {
                                            !selectedSignatures.contains(
                                                it
                                            )
                                        })
                                    else
                                        selectedSignatures.clear()
                                }
                            )
                        }
                    }
                }
            }
            items(userSignatureSettings.userSignatures) { signature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedSignatures.contains(signature),
                            onCheckedChange = { checked ->
                                if (checked && !selectedSignatures.contains(signature))
                                    selectedSignatures.add(signature)
                                else if (!checked)
                                    selectedSignatures.remove(signature)
                                scanSettings.save()
                            }
                        )
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                            Text(
                                text = signature.name,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    if (selectedSignatures.contains(signature))
                                        selectedSignatures.remove(signature)
                                    else
                                        selectedSignatures.add(signature)
                                }
                            )
                        }
                    }
                    Row {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    editedUserSignature = signature
                                    userSignatureEditorVisibility = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    scanSettings.userSignatures.remove(signature)
                                    userSignatureSettings.userSignatures.remove(signature)
                                    scanSettings.save()
                                    userSignatureSettings.save()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null
                            )
                        }
                    }

                }
            }
        }
    }
}