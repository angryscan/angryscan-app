package org.angryscan.app.ui.windows.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.*
import org.angryscan.app.store.ContextMenu
import org.angryscan.app.ui.dialogs.ContactDialog
import org.angryscan.app.ui.dialogs.LicenseDialog
import org.angryscan.app.ui.windows.screens.settings.items.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val PageMaxWidth = 1080.dp
private val SectionShape = RoundedCornerShape(18.dp)
private val PagePaddingX = 18.dp
private val PagePaddingY = 14.dp
private val CanvasPadding = 6.dp
private val GridGap = 14.dp

@Composable
fun SettingsScreen() {
    val appSettings = koinInject<AppSettings>()
    val language by remember { appSettings.language }

    key(language) {
        AnimatedContent(
            targetState = language,
            transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)) },
            label = "settings_content"
        ) { _ ->
            var showContactDialog by remember { mutableStateOf(false) }
            val contactDialogState = rememberDialogState(width = 400.dp, height = 260.dp)

            var showLicenseDialog by remember { mutableStateOf(false) }
            val licenseDialogState = rememberDialogState(width = 600.dp, height = 590.dp)

            SettingsCanvas(
                appSettings = appSettings,
                onOpenContact = { showContactDialog = true },
                onOpenLicense = { showLicenseDialog = true },
            )

            if (showContactDialog) {
                ContactDialog(
                    onCloseRequest = { showContactDialog = false },
                    dialogState = contactDialogState
                )
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
private fun SettingsCanvas(
    appSettings: AppSettings,
    onOpenContact: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val canvasScroll = rememberScrollState()

    val scanService = org.koin.compose.koinInject<org.angryscan.app.scan.ScanService>()
    val scanSettings = org.koin.compose.koinInject<org.angryscan.app.common.ScanSettings>()

    var languageState by remember { appSettings.language }
    var theme by remember { appSettings.theme }

    var sliderPosition by remember { mutableStateOf(appSettings.threadCount.value.toFloat()) }
    var threadCount by remember { appSettings.threadCount }
    val maxThreads = Runtime.getRuntime().availableProcessors()
    val recommendedThreads = (maxThreads + 1) / 2

    var engine by remember { scanSettings.engine }

    var debugModeEnabled by remember { appSettings.debugMode }
    val supported = ContextMenu.supported()
    var contextMenuEnabled by remember { mutableStateOf(ContextMenu.enabled) }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = PagePaddingX, vertical = PagePaddingY)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = PageMaxWidth)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(canvasScroll)
                    .padding(top = 6.dp, bottom = CanvasPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val twoCol = maxWidth >= 900.dp
                    if (twoCol) {
                        SettingsTwoColumnGrid(
                            gap = GridGap,
                            left1 = {
                                SettingsSectionCard(
                                    modifier = Modifier.fillMaxHeight(),
                                    title = stringResource(Res.string.SettingsScreen_Language),
                                    subtitle = stringResource(Res.string.SettingsScreen_LanguageDescription)
                                ) {
                                    LanguageSettingsContent(
                                        language = languageState,
                                        onLanguageSelect = { lang ->
                                            languageState = lang
                                            appSettings.save()
                                            java.util.Locale.setDefault(java.util.Locale.forLanguageTag(lang.locale))
                                        },
                                        showDescription = false,
                                    )
                                }
                            },
                            right1 = {
                                SettingsSectionCard(
                                    modifier = Modifier.fillMaxHeight(),
                                    title = stringResource(Res.string.SettingsScreen_Theme),
                                    subtitle = stringResource(Res.string.SettingsScreen_ThemeDescription)
                                ) {
                                    ThemeSettingsContent(
                                        theme = theme,
                                        onThemeSelect = { th ->
                                            theme = th
                                            appSettings.save()
                                        },
                                        showDescription = false,
                                    )
                                }
                            },
                            left2 = {
                                SettingsSectionCard(
                                    modifier = Modifier.fillMaxHeight(),
                                    title = stringResource(Res.string.SettingsScreen_ThreadsCount),
                                    subtitle = stringResource(Res.string.SettingsScreen_RecommendedThreads, recommendedThreads)
                                ) {
                                    ThreadCountSettingsContent(
                                        sliderPosition = sliderPosition,
                                        maxThreads = maxThreads,
                                        recommendedThreads = recommendedThreads,
                                        colorScheme = colorScheme,
                                        onSliderChange = { sliderPosition = it },
                                        onSliderFinish = {
                                            threadCount = sliderPosition.toInt()
                                            appSettings.save()
                                            scanService.setThreadsCount()
                                        }
                                    )
                                }
                            },
                            right2 = {
                                SettingsSectionCard(
                                    modifier = Modifier.fillMaxHeight(),
                                    title = stringResource(Res.string.SettingsScreen_ScanEngine),
                                    subtitle = stringResource(Res.string.SettingsScreen_ScanEngineDescription)
                                ) {
                                    EngineSettingsContent(
                                        engine = engine,
                                        onEngineSelect = { eng ->
                                            engine = eng
                                            scanSettings.save()
                                        },
                                        showDescription = false,
                                    )
                                }
                            },
                            left3 = {
                                SettingsSectionCard(
                                    modifier = Modifier.fillMaxHeight(),
                                    title = stringResource(Res.string.SettingsScreen_Logging),
                                    subtitle = stringResource(Res.string.SettingsScreen_LoggingDescription)
                                ) {
                                    LoggingSettingsContent(
                                        debugModeEnabled = debugModeEnabled,
                                        onDebugModeChange = {
                                            debugModeEnabled = it
                                            appSettings.save()
                                        },
                                        onOpenFolder = {
                                            java.awt.Desktop.getDesktop()
                                                .open(org.angryscan.app.common.AppFiles.LoggingDir.toFile())
                                        },
                                        showDescription = false,
                                    )
                                }
                            },
                            right3 = {
                                SettingsSectionCard(
                                    modifier = Modifier.fillMaxHeight(),
                                    title = stringResource(Res.string.SideMenu_AboutPage)
                                ) {
                                    AboutSettingsContent(
                                        onOpenContact = onOpenContact,
                                        onOpenLicense = onOpenLicense,
                                        showDescription = false,
                                    )
                                }
                            },
                        )

                        if (supported) {
                            SettingsSectionCard(
                                title = stringResource(Res.string.SettingsScreen_ContextMenu),
                                subtitle = stringResource(Res.string.SettingsScreen_ContextMenuExplorer)
                            ) {
                                ContextMenuSettingsContent(
                                    contextMenuEnabled = contextMenuEnabled,
                                    onContextMenuChange = {
                                        contextMenuEnabled = it
                                        ContextMenu.enabled = it
                                    }
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsSectionCard(
                                title = stringResource(Res.string.SettingsScreen_Language),
                                subtitle = stringResource(Res.string.SettingsScreen_LanguageDescription)
                            ) {
                                LanguageSettingsContent(
                                    language = languageState,
                                    onLanguageSelect = { lang ->
                                        languageState = lang
                                        appSettings.save()
                                        java.util.Locale.setDefault(java.util.Locale.forLanguageTag(lang.locale))
                                    },
                                    showDescription = false,
                                )
                            }
                            SettingsSectionCard(
                                title = stringResource(Res.string.SettingsScreen_Theme),
                                subtitle = stringResource(Res.string.SettingsScreen_ThemeDescription)
                            ) {
                                ThemeSettingsContent(
                                    theme = theme,
                                    onThemeSelect = { th ->
                                        theme = th
                                        appSettings.save()
                                    },
                                    showDescription = false,
                                )
                            }
                            SettingsSectionCard(
                                title = stringResource(Res.string.SettingsScreen_ThreadsCount),
                                subtitle = stringResource(Res.string.SettingsScreen_RecommendedThreads, recommendedThreads)
                            ) {
                                ThreadCountSettingsContent(
                                    sliderPosition = sliderPosition,
                                    maxThreads = maxThreads,
                                    recommendedThreads = recommendedThreads,
                                    colorScheme = colorScheme,
                                    onSliderChange = { sliderPosition = it },
                                    onSliderFinish = {
                                        threadCount = sliderPosition.toInt()
                                        appSettings.save()
                                        scanService.setThreadsCount()
                                    }
                                )
                            }
                            SettingsSectionCard(
                                title = stringResource(Res.string.SettingsScreen_ScanEngine),
                                subtitle = stringResource(Res.string.SettingsScreen_ScanEngineDescription)
                            ) {
                                EngineSettingsContent(
                                    engine = engine,
                                    onEngineSelect = { eng ->
                                        engine = eng
                                        scanSettings.save()
                                    },
                                    showDescription = false,
                                )
                            }
                            SettingsSectionCard(
                                title = stringResource(Res.string.SettingsScreen_Logging),
                                subtitle = stringResource(Res.string.SettingsScreen_LoggingDescription)
                            ) {
                                LoggingSettingsContent(
                                    debugModeEnabled = debugModeEnabled,
                                    onDebugModeChange = {
                                        debugModeEnabled = it
                                        appSettings.save()
                                    },
                                    onOpenFolder = {
                                        java.awt.Desktop.getDesktop()
                                            .open(org.angryscan.app.common.AppFiles.LoggingDir.toFile())
                                    },
                                    showDescription = false,
                                )
                            }
                            if (supported) {
                                SettingsSectionCard(
                                    title = stringResource(Res.string.SettingsScreen_ContextMenu),
                                    subtitle = stringResource(Res.string.SettingsScreen_ContextMenuExplorer)
                                ) {
                                    ContextMenuSettingsContent(
                                        contextMenuEnabled = contextMenuEnabled,
                                        onContextMenuChange = {
                                            contextMenuEnabled = it
                                            ContextMenu.enabled = it
                                        }
                                    )
                                }
                            }
                            SettingsSectionCard(title = stringResource(Res.string.SideMenu_AboutPage)) {
                                AboutSettingsContent(
                                    onOpenContact = onOpenContact,
                                    onOpenLicense = onOpenLicense,
                                    showDescription = false,
                                )
                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(canvasScroll),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp),
                style = LocalScrollbarStyle.current.copy(
                    hoverColor = colorScheme.primary,
                    unhoverColor = colorScheme.outline.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SectionShape,
        color = colorScheme.surfaceVariant.copy(alpha = 0.20f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsTwoColumnGrid(
    gap: Dp,
    left1: @Composable () -> Unit,
    right1: @Composable () -> Unit,
    left2: @Composable () -> Unit,
    right2: @Composable () -> Unit,
    left3: @Composable () -> Unit,
    right3: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        EqualHeightTwoColumnRow(gap = gap, left = left1, right = right1)
        EqualHeightTwoColumnRow(gap = gap, left = left2, right = right2)
        EqualHeightTwoColumnRow(gap = gap, left = left3, right = right3)
    }
}

@Composable
private fun EqualHeightTwoColumnRow(
    gap: Dp,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val clampedGap = gap.roundToPx().coerceAtLeast(0)
        val availableWidth = constraints.maxWidth
        val childWidth = ((availableWidth - clampedGap) / 2).coerceAtLeast(0)

        val widthConstraints = constraints.copy(
            minWidth = childWidth,
            maxWidth = childWidth,
            minHeight = 0,
        )

        val leftFirst = subcompose(slotId = "left_first") { Box { left() } }
            .first()
            .measure(widthConstraints)

        val rightFirst = subcompose(slotId = "right_first") { Box { right() } }
            .first()
            .measure(widthConstraints)

        val rowHeight = maxOf(leftFirst.height, rightFirst.height)

        val forcedHeightConstraints = widthConstraints.copy(
            minHeight = rowHeight,
            maxHeight = rowHeight
        )

        val leftFinal = subcompose(slotId = "left_final") { Box { left() } }
            .first()
            .measure(forcedHeightConstraints)

        val rightFinal = subcompose(slotId = "right_final") { Box { right() } }
            .first()
            .measure(forcedHeightConstraints)

        layout(width = availableWidth, height = rowHeight) {
            leftFinal.placeRelative(0, 0)
            rightFinal.placeRelative(childWidth + clampedGap, 0)
        }
    }
}