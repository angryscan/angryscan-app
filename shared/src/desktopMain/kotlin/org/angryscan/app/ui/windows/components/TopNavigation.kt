package org.angryscan.app.ui.windows.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberDialogState
import androidx.navigation.NavController
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.AppVersion
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.dialogs.LicenseDialog
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.awt.Desktop
import java.util.*

private val TopBarHeight = 56.dp

@Composable
private fun QuickHoverTextButton(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hovered) cs.primary.copy(alpha = 0.10f) else Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .hoverable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (hovered) cs.primary else cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val cs = MaterialTheme.colorScheme

    val containerColor = when {
        selected -> cs.primary.copy(alpha = 0.16f)
        hovered -> cs.primary.copy(alpha = 0.10f)
        else -> cs.surfaceVariant.copy(alpha = 0.24f)
    }
    val contentColor = when {
        selected || hovered -> cs.primary
        else -> cs.onSurfaceVariant
    }
    val borderColor = when {
        selected || hovered -> cs.primary.copy(alpha = 0.35f)
        else -> cs.outlineVariant.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(width = 1.dp, color = borderColor)
    ) {
        Box(
            modifier = Modifier
                .hoverable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                label()
            }
        }
    }
}

@Composable
fun TopNavigation(
    navController: NavController,
    windowPlacement: androidx.compose.ui.window.WindowPlacement? = null,
    expanded: Boolean = false,
    settingsExpanded: Boolean = false,
    onSettingsExpandedChange: (Boolean) -> Unit = {},
    onMinimizeClick: (() -> Unit)? = null,
    onExpandClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null
) {
    val appSettings = koinInject<AppSettings>()
    val scanSettings = koinInject<ScanSettings>()
    val scanService = koinInject<ScanService>()
    var theme by remember { appSettings.theme }
    var language by remember { appSettings.language }
    var fastScan by remember { scanSettings.fastScan }
    var engine by remember { scanSettings.engine }
    val density = LocalDensity.current
    val popupYOffsetPx = remember(density) { with(density) { TopBarHeight.roundToPx() } }
    var threadCount by remember { appSettings.threadCount }
    var debugMode by remember { appSettings.debugMode }

    var showLicenseDialog by remember { mutableStateOf(false) }
    val licenseDialogState = rememberDialogState(width = 600.dp, height = 590.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(navController = navController)
                }

                if (onMinimizeClick != null && onExpandClick != null && onCloseClick != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSettingsExpandedChange(!settingsExpanded) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(Res.string.QuickSettings_Title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WindowControlButtons(
                            windowPlacement = windowPlacement,
                            expanded = expanded,
                            onMinimizeClick = onMinimizeClick,
                            onExpandClick = onExpandClick,
                            onCloseClick = onCloseClick
                        )
                    }
                }
            }

            if (settingsExpanded) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(x = 0, y = popupYOffsetPx),
                    onDismissRequest = { onSettingsExpandedChange(false) },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        modifier = Modifier
                            .width(430.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val uriHandler = LocalUriHandler.current

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.QuickSettings_Title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.AboutScreen_Version, AppVersion),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { onSettingsExpandedChange(false) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = stringResource(Res.string.close),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(Res.string.AboutScreen_Description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            CompactSettingsRow(label = stringResource(Res.string.QuickSettings_Theme)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AppSettings.ThemeType.entries.forEach { th ->
                                        QuickFilterChip(
                                            selected = theme == th,
                                            onClick = {
                                                theme = th
                                                appSettings.save()
                                            },
                                            label = {
                                                Text(
                                                    text = when (th) {
                                                        AppSettings.ThemeType.System -> stringResource(Res.string.QuickSettings_Theme_System)
                                                        AppSettings.ThemeType.Dark -> stringResource(Res.string.QuickSettings_Theme_Dark)
                                                        AppSettings.ThemeType.Light -> stringResource(Res.string.QuickSettings_Theme_Light)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            CompactSettingsRow(label = stringResource(Res.string.QuickSettings_Language)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        AppSettings.LanguageType.Default to stringResource(Res.string.QuickSettings_Language_Default),
                                        AppSettings.LanguageType.EN to "EN",
                                        AppSettings.LanguageType.RU to "RU",
                                    ).forEach { (lang, label) ->
                                        QuickFilterChip(
                                            selected = language == lang,
                                            onClick = {
                                                language = lang
                                                Locale.setDefault(Locale.forLanguageTag(lang.locale))
                                                appSettings.save()
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            CompactSettingsRow(label = stringResource(Res.string.QuickSettings_FastScan)) {
                                Switch(
                                    checked = fastScan,
                                    onCheckedChange = {
                                        fastScan = it
                                        scanSettings.save()
                                    }
                                )
                            }

                            CompactSettingsRow(label = stringResource(Res.string.QuickSettings_Engine)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    QuickFilterChip(
                                        selected = engine == HyperScanEngine::class,
                                        onClick = {
                                            engine = HyperScanEngine::class
                                            scanSettings.save()
                                        },
                                        label = { Text("HyperScan", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    QuickFilterChip(
                                        selected = engine == KotlinEngine::class,
                                        onClick = {
                                            engine = KotlinEngine::class
                                            scanSettings.save()
                                        },
                                        label = { Text("Kotlin", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            CompactSettingsRow(label = stringResource(Res.string.QuickSettings_Threads)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = threadCount.toFloat(),
                                        onValueChange = {
                                            threadCount = it.toInt().coerceAtLeast(1)
                                            appSettings.save()
                                            scanService.setThreadsCount()
                                        },
                                        valueRange = 1f..Runtime.getRuntime().availableProcessors().toFloat(),
                                        steps = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(0),
                                        modifier = Modifier.weight(1f).height(20.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = threadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            CompactSettingsRow(label = stringResource(Res.string.QuickSettings_Logging)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = debugMode,
                                        onCheckedChange = {
                                            debugMode = it
                                            appSettings.save()
                                        }
                                    )
                                    QuickHoverTextButton(
                                        text = stringResource(Res.string.QuickSettings_OpenLogs),
                                        onClick = { runCatching { Desktop.getDesktop().open(AppFiles.LoggingDir.toFile()) } }
                                    )
                                }
                            }

                            // Contacts (styled like other quick settings)
                            run {
                                val websiteUrl = stringResource(Res.string.ContactDialog_WebsiteUrl)
                                val email = stringResource(Res.string.ContactDialog_Email)
                                val websiteLabel = stringResource(Res.string.ContactDialog_WebsiteLabel)
                                val emailLabel = stringResource(Res.string.ContactDialog_EmailLabel)

                                CompactSettingsRow(label = stringResource(Res.string.ContactDialog_Title)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DescriptionTooltip(description = websiteUrl) {
                                            QuickFilterChip(
                                                selected = false,
                                                onClick = { uriHandler.openUri(websiteUrl) },
                                                label = { Text(websiteLabel, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                        DescriptionTooltip(description = email) {
                                            QuickFilterChip(
                                                selected = false,
                                                onClick = { uriHandler.openUri("mailto:$email") },
                                                label = { Text(emailLabel, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                        QuickFilterChip(
                                            selected = false,
                                            onClick = { showLicenseDialog = true },
                                            label = {
                                                Text(
                                                    stringResource(Res.string.QuickSettings_License),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showLicenseDialog) {
                LicenseDialog(
                    onCloseRequest = { showLicenseDialog = false },
                    dialogState = licenseDialogState
                )
            }
        }
    }
}

@Composable
private fun CompactSettingsRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(132.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

