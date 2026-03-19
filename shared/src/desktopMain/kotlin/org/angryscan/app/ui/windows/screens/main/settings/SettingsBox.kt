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
private val EmbeddedShape = RoundedCornerShape(16.dp)
private val ContentPadding = 12.dp
private val EmbeddedContentPadding = 8.dp

enum class SettingsTab { Scan, Files, Detect, Signatures }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBox(
    transition: Transition<Boolean>,
    isS3Source: Boolean = false,
    /** Внутри карточки пути: компактнее, без лишней рамки */
    embedded: Boolean = false
) {
    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()
    val fastScan by scanSettings.fastScan
    var selectedTab by remember { mutableIntStateOf(SettingsTab.Scan.ordinal) }
    val scrollState = rememberScrollState()

    val colorScheme = MaterialTheme.colorScheme
    val containerShape = if (embedded) EmbeddedShape else ContainerShape
    val contentPad = if (embedded) EmbeddedContentPadding else ContentPadding
    val tabsToShow = remember(embedded, isS3Source) {
        if (embedded && !isS3Source) listOf(SettingsTab.Files, SettingsTab.Detect, SettingsTab.Signatures)
        else listOf(SettingsTab.Scan, SettingsTab.Files, SettingsTab.Detect, SettingsTab.Signatures)
    }
    val selectedVisibleIndex = tabsToShow.indexOf(SettingsTab.entries.getOrElse(selectedTab) { SettingsTab.Files }).coerceIn(0, tabsToShow.size - 1)
    LaunchedEffect(embedded, isS3Source, selectedTab) {
        if (embedded && !isS3Source && selectedTab == SettingsTab.Scan.ordinal) selectedTab = SettingsTab.Files.ordinal
    }
    AnimatedVisibility(
        visible = transition.currentState,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (embedded) Modifier
                        .clip(EmbeddedShape)
                        .background(colorScheme.surface.copy(alpha = 0.5f))
                    else Modifier
                        .clip(containerShape)
                        .background(colorScheme.surface.copy(alpha = 0.6f))
                        .border(
                            width = 1.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = containerShape
                        )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Шапка: табы в отдельной полоске с мягким фоном
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (embedded) colorScheme.primary.copy(alpha = 0.06f)
                            else colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                ) {
                    TabRow(
                        selectedTabIndex = selectedVisibleIndex,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        contentColor = colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            Box(Modifier.fillMaxWidth()) {
                                if (selectedVisibleIndex < tabPositions.size) {
                                    val pos = tabPositions[selectedVisibleIndex]
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .offset(x = pos.left)
                                            .width(pos.width)
                                            .height(if (embedded) 2.5.dp else 3.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(colorScheme.primary)
                                    )
                                }
                            }
                        }
                    ) {
                        tabsToShow.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab == tab.ordinal,
                                onClick = { selectedTab = tab.ordinal },
                                text = {
                                    val label = when (tab) {
                                        SettingsTab.Scan -> stringResource(Res.string.ScanSettings_TabScan)
                                        SettingsTab.Files -> stringResource(Res.string.ScanSettings_TabFiles)
                                        SettingsTab.Detect -> stringResource(Res.string.ScanSettings_TabDetect)
                                        SettingsTab.Signatures -> stringResource(Res.string.ScanSettings_TabSignatures)
                                    }
                                    Text(label, fontSize = if (embedded) 13.sp else 14.sp)
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )

                // Контент выбранной секции — один скролл
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentPad)
                ) {
                    val scrollEnabled = selectedTab != SettingsTab.Files.ordinal
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (scrollEnabled) Modifier.verticalScroll(scrollState) else Modifier)
                            .padding(vertical = contentPad),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        when (selectedTab) {
                            SettingsTab.Scan.ordinal -> {
                                if (!embedded) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(Res.string.ScanSettings_FastScan),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(Res.string.ScanSettings_Tooltip_FastScan),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = fastScan,
                                            onCheckedChange = {
                                                scanSettings.fastScan.value = it
                                                scanSettings.save()
                                            }
                                        )
                                    }
                                }
                                // AWS S3 connection parameters — только когда выбран источник S3
                                if (isS3Source) {
                                    var s3Endpoint by remember { mutableStateOf(screenStateSettings.s3ScreenState.endpoint) }
                                    var s3Bucket by remember { mutableStateOf(screenStateSettings.s3ScreenState.bucket) }
                                    var s3AccessKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.accessKey) }
                                    var s3SecretKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.secretKey) }
                                    LaunchedEffect(selectedTab, isS3Source) {
                                        if (selectedTab == SettingsTab.Scan.ordinal && isS3Source) {
                                            s3Endpoint = screenStateSettings.s3ScreenState.endpoint
                                            s3Bucket = screenStateSettings.s3ScreenState.bucket
                                            s3AccessKey = screenStateSettings.s3ScreenState.accessKey
                                            s3SecretKey = screenStateSettings.s3ScreenState.secretKey
                                        }
                                    }
                                    Text(
                                        text = stringResource(Res.string.S3Screen_Tooltip_ConnectionSettings),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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
                            }
                            SettingsTab.Files.ordinal -> SettingsBoxExtensionsSelection(scanSettings)
                            SettingsTab.Detect.ordinal -> SettingsBoxDetectFunctionsGrouped(scanSettings)
                            SettingsTab.Signatures.ordinal -> SettingsBoxUserSignature(scanSettings)
                        }
                    }

                    if (scrollEnabled) {
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
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
    }
}
