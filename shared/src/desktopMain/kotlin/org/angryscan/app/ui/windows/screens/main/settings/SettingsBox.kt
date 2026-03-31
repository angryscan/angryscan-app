package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val ContainerShape = RoundedCornerShape(24.dp)
private val ContentPadding = 12.dp

@Composable
fun SettingsBox(
    transition: Transition<Boolean>,
    isS3Source: Boolean = false
) {
    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()
    val fastScan by scanSettings.fastScan
    val editorScrollState = rememberScrollState()
    val profilesScrollState = rememberScrollState()
    val profiles = screenStateSettings.scanProfiles
    var selectedProfileName by remember { mutableStateOf(screenStateSettings.activeScanProfileName) }
    var saveAsDialogVisible by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    val colorScheme = MaterialTheme.colorScheme
    val selectedProfile = profiles.firstOrNull { it.name == selectedProfileName }
        ?: profiles.firstOrNull()

    if (selectedProfile == null) return

    fun buildProfile(name: String): ScreenStateSettings.ScanProfile = ScreenStateSettings.ScanProfile(
        name = name,
        fastScan = scanSettings.fastScan.value,
        extensions = scanSettings.extensions.toMutableList(),
        matchers = scanSettings.matchers.toMutableList(),
        userSignatures = scanSettings.userSignatures.toMutableList()
    )

    fun applyProfile(profile: ScreenStateSettings.ScanProfile) {
        scanSettings.fastScan.value = profile.fastScan
        scanSettings.extensions.clear()
        scanSettings.extensions.addAll(profile.extensions)
        scanSettings.matchers.clear()
        scanSettings.matchers.addAll(profile.matchers)
        scanSettings.userSignatures.clear()
        scanSettings.userSignatures.addAll(profile.userSignatures)
        scanSettings.save()
    }

    fun updateSelectedProfileFromCurrent() {
        val index = profiles.indexOfFirst { it.name == selectedProfileName }
        if (index >= 0) {
            profiles[index] = buildProfile(selectedProfileName)
            screenStateSettings.activeScanProfileName = selectedProfileName
            screenStateSettings.save()
        }
    }

    LaunchedEffect(profiles.size) {
        if (profiles.none { it.name == selectedProfileName } && profiles.isNotEmpty()) {
            selectedProfileName = profiles.first().name
        }
    }

    if (saveAsDialogVisible) {
        AlertDialog(
            onDismissRequest = { saveAsDialogVisible = false },
            title = { Text(stringResource(Res.string.ScanSettings_Profiles_NewDialogTitle)) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.ScanSettings_Profiles_NamePlaceholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalName = newProfileName.trim()
                        if (finalName.isNotEmpty()) {
                            val existingIndex = profiles.indexOfFirst { it.name == finalName }
                            if (existingIndex >= 0) {
                                profiles[existingIndex] = buildProfile(finalName)
                            } else {
                                profiles.add(buildProfile(finalName))
                            }
                            selectedProfileName = finalName
                            screenStateSettings.activeScanProfileName = finalName
                            screenStateSettings.save()
                        }
                        saveAsDialogVisible = false
                    }
                ) { Text(stringResource(Res.string.Common_Save)) }
            },
            dismissButton = {
                TextButton(onClick = { saveAsDialogVisible = false }) {
                    Text(stringResource(Res.string.Common_Cancel))
                }
            }
        )
    }

    AnimatedVisibility(
        visible = transition.currentState,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(ContainerShape)
                .background(colorScheme.surface.copy(alpha = 0.6f))
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = ContainerShape
                )
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                color = colorScheme.surfaceVariant.copy(alpha = 0.28f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.ScanSettings_Profiles_Title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = selectedProfileName.ifBlank { stringResource(Res.string.ScanSettings_Profiles_Current) },
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            newProfileName = selectedProfileName
                            saveAsDialogVisible = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.ScanSettings_Profiles_SaveAs))
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = stringResource(Res.string.ScanSettings_Profiles_ScrollHint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 132.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(profilesScrollState)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            profiles.forEach { profile ->
                                val isSelected = profile.name == selectedProfileName
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedProfileName = profile.name
                                            applyProfile(profile)
                                            screenStateSettings.activeScanProfileName = profile.name
                                            screenStateSettings.save()
                                        },
                                    color = if (isSelected)
                                        colorScheme.primary.copy(alpha = 0.12f)
                                    else
                                        Color.Transparent
                                ) {
                                    Text(
                                        text = profile.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(profilesScrollState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(8.dp),
                            style = LocalScrollbarStyle.current.copy(
                                hoverColor = colorScheme.primary,
                                unhoverColor = colorScheme.outline.copy(alpha = 0.35f)
                            )
                        )
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    OutlinedButton(
                        onClick = { applyProfile(selectedProfile) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.ScanSettings_Profiles_Apply))
                    }
                    TextButton(
                        onClick = { updateSelectedProfileFromCurrent() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.ScanSettings_Profiles_Update))
                    }
                    TextButton(
                        enabled = profiles.size > 1,
                        onClick = {
                            val toDelete = selectedProfileName
                            profiles.removeAll { it.name == toDelete }
                            selectedProfileName = profiles.firstOrNull()?.name ?: "Default"
                            screenStateSettings.activeScanProfileName = selectedProfileName
                            screenStateSettings.save()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.ScanSettings_Profiles_Delete))
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ContentPadding, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = selectedProfileName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.ScanSettings_Profiles_Current),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp),
                        color = colorScheme.outlineVariant.copy(alpha = 0.45f),
                        thickness = 1.dp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScrollState)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsSectionCard(
                            title = stringResource(Res.string.ScanSettings_FastScan),
                            titleTrailing = {
                                Switch(
                                    checked = fastScan,
                                    onCheckedChange = {
                                        scanSettings.fastScan.value = it
                                        scanSettings.save()
                                    }
                                )
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.ScanSettings_Tooltip_FastScan),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isS3Source) {
                            var s3Endpoint by remember { mutableStateOf(screenStateSettings.s3ScreenState.endpoint) }
                            var s3Bucket by remember { mutableStateOf(screenStateSettings.s3ScreenState.bucket) }
                            var s3AccessKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.accessKey) }
                            var s3SecretKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.secretKey) }
                            Text(
                                text = stringResource(Res.string.S3Screen_Tooltip_ConnectionSettings),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = s3Endpoint,
                                    onValueChange = {
                                        s3Endpoint = it
                                        screenStateSettings.s3ScreenState.endpoint = it
                                        screenStateSettings.save()
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    placeholder = { Text("Endpoint", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = s3Bucket,
                                    onValueChange = {
                                        s3Bucket = it
                                        screenStateSettings.s3ScreenState.bucket = it
                                        screenStateSettings.save()
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    placeholder = { Text("Bucket", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = s3AccessKey,
                                    onValueChange = {
                                        s3AccessKey = it
                                        screenStateSettings.s3ScreenState.accessKey = it
                                        screenStateSettings.save()
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    placeholder = { Text("Access key", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = s3SecretKey,
                                    onValueChange = {
                                        s3SecretKey = it
                                        screenStateSettings.s3ScreenState.secretKey = it
                                        screenStateSettings.save()
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    placeholder = { Text("Secret key", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        }

                        SettingsBoxExtensionsSelection(scanSettings)
                        SettingsBoxDetectFunctionsGrouped(scanSettings)
                        SettingsBoxUserSignature(scanSettings)
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(editorScrollState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(8.dp),
                    style = LocalScrollbarStyle.current.copy(
                        hoverColor = MaterialTheme.colorScheme.primary,
                        unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}
