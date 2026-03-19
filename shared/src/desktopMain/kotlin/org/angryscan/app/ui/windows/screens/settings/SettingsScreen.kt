package org.angryscan.app.ui.windows.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
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

private val maxContentWidth = 860.dp
private val sectionSpacing = 12.dp
private val pagePadding = 14.dp
private val twoColumnMinWidth = 760.dp
private val twoColumnGap = 18.dp
private val innerCardShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
private val innerCornerRadius = 12.dp

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = pagePadding)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = maxContentWidth)

                    var showContactDialog by remember { mutableStateOf(false) }
                    val contactDialogState = rememberDialogState(width = 400.dp, height = 260.dp)

                    var showLicenseDialog by remember { mutableStateOf(false) }
                    val licenseDialogState = rememberDialogState(width = 600.dp, height = 590.dp)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        modifier = contentModifier.fillMaxSize()
                    ) {
                        item {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val twoColumn = maxWidth >= twoColumnMinWidth
                                var languageState by remember { appSettings.language }
                                var theme by remember { appSettings.theme }

                                if (twoColumn) {
                                    EqualHeightTwoColumnRow(
                                        gap = twoColumnGap,
                                        leftTitle = stringResource(Res.string.SettingsScreen_Language),
                                        leftContent = {
                                            LanguageSettingsContent(
                                                language = languageState,
                                                onLanguageSelect = { lang ->
                                                    languageState = lang
                                                    appSettings.save()
                                                    java.util.Locale.setDefault(java.util.Locale.forLanguageTag(lang.locale))
                                                },
                                                showDescription = false,
                                            )
                                        },
                                        rightTitle = stringResource(Res.string.SettingsScreen_Theme),
                                        rightContent = {
                                            ThemeSettingsContent(
                                                theme = theme,
                                                onThemeSelect = { th ->
                                                    theme = th
                                                    appSettings.save()
                                                },
                                                showDescription = false,
                                            )
                                        },
                                    )
                                } else {
                                    LanguageSettingsContent(
                                        language = languageState,
                                        onLanguageSelect = { lang ->
                                            languageState = lang
                                            appSettings.save()
                                            java.util.Locale.setDefault(java.util.Locale.forLanguageTag(lang.locale))
                                        },
                                        showDescription = false,
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    ThemeSettingsContent(
                                        theme = theme,
                                        onThemeSelect = { th ->
                                            theme = th
                                            appSettings.save()
                                        },
                                        showDescription = false,
                                    )
                                }
                            }
                        }

                        item { SectionDivider() }

                        item {
                            val scanService = org.koin.compose.koinInject<org.angryscan.app.scan.ScanService>()
                            val colorScheme = MaterialTheme.colorScheme
                            var sliderPosition by remember { mutableStateOf(appSettings.threadCount.value.toFloat()) }
                            var threadCount by remember { appSettings.threadCount }
                            val maxThreads = Runtime.getRuntime().availableProcessors()
                            val recommendedThreads = (maxThreads + 1) / 2
                            val scanSettings = org.koin.compose.koinInject<org.angryscan.app.common.ScanSettings>()
                            var engine by remember { scanSettings.engine }

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val twoColumn = maxWidth >= twoColumnMinWidth

                                if (twoColumn) {
                                    EqualHeightTwoColumnRow(
                                        gap = twoColumnGap,
                                        leftTitle = stringResource(Res.string.SettingsScreen_ThreadsCount),
                                        leftContent = {
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
                                        },
                                        rightTitle = stringResource(Res.string.SettingsScreen_ScanEngine),
                                        rightContent = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.SettingsScreen_ScanEngineDescription),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                )
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
                                    )
                                } else {
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

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    EngineSettingsContent(
                                        engine = engine,
                                        onEngineSelect = { eng ->
                                            engine = eng
                                            scanSettings.save()
                                        },
                                        showDescription = false,
                                    )
                                }
                            }
                        }

                        item { SectionDivider() }

                        item {
                            var debugModeEnabled by remember { appSettings.debugMode }
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val supported = ContextMenu.supported()
                                val twoColumn = maxWidth >= twoColumnMinWidth

                                if (twoColumn) {
                                    EqualHeightTwoColumnRow(
                                        gap = twoColumnGap,
                                        leftTitle = stringResource(Res.string.SettingsScreen_Logging),
                                        leftContent = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.SettingsScreen_LoggingDescription),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
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
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )

                                                var contextMenuEnabled by remember { mutableStateOf(ContextMenu.enabled) }
                                                ContextMenuSettingsContent(
                                                    contextMenuEnabled = contextMenuEnabled,
                                                    onContextMenuChange = {
                                                        contextMenuEnabled = it
                                                        ContextMenu.enabled = it
                                                    }
                                                )
                                            }
                                        },
                                        rightTitle = stringResource(Res.string.SideMenu_AboutPage),
                                        rightContent = {
                                            AboutSettingsContent(
                                                onOpenContact = { showContactDialog = true },
                                                onOpenLicense = { showLicenseDialog = true },
                                                showDescription = false,
                                            )
                                        },
                                    )
                                } else {
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

                                    if (supported) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )

                                        var contextMenuEnabled by remember { mutableStateOf(ContextMenu.enabled) }
                                        ContextMenuSettingsContent(
                                            contextMenuEnabled = contextMenuEnabled,
                                            onContextMenuChange = {
                                                contextMenuEnabled = it
                                                ContextMenu.enabled = it
                                            }
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    )

                                    AboutSettingsContent(
                                        onOpenContact = { showContactDialog = true },
                                        onOpenLicense = { showLicenseDialog = true },
                                        showDescription = false,
                                    )
                                }
                            }
                        }

                        // About merged into System block
                    }

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
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = sectionSpacing),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

@Composable
private fun InlinePanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = innerCardShape
            ),
        shape = innerCardShape,
        color = colorScheme.surface.copy(alpha = 0.30f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
            content()
        }
    }
}

@Composable
private fun TwoColumnGap() {
    Spacer(modifier = Modifier.width(twoColumnGap))
}

@Composable
private fun EqualHeightTwoColumnRow(
    gap: androidx.compose.ui.unit.Dp,
    leftTitle: String,
    leftContent: @Composable () -> Unit,
    rightTitle: String,
    rightContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val cornerPx = with(androidx.compose.ui.platform.LocalDensity.current) { innerCornerRadius.toPx() }
    val borderPx = with(androidx.compose.ui.platform.LocalDensity.current) { 1.dp.toPx() }
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { gap.toPx() }
    val padX = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    val padY = with(androidx.compose.ui.platform.LocalDensity.current) { 10.dp.toPx() }
    val labelBodyGap = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    val legendErasePadX = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }

    var leftLegendWidthPx by remember { mutableStateOf(0f) }
    var rightLegendWidthPx by remember { mutableStateOf(0f) }
    var legendHeightPx by remember { mutableStateOf(0f) }
    var legendHalfPx by remember { mutableStateOf(0f) }

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                val w = size.width
                val h = size.height
                val childW = ((w - gapPx) / 2f).coerceAtLeast(0f)
                val topInset = legendHalfPx.coerceAtLeast(0f)
                val r = CornerRadius(cornerPx, cornerPx)
                val stroke = Stroke(width = borderPx)
                val bg = colorScheme.surface.copy(alpha = 0.30f)
                val border = colorScheme.outlineVariant.copy(alpha = 0.55f)

                // Left panel chrome
                drawRoundRect(
                    color = bg,
                    topLeft = Offset(0f, topInset),
                    size = Size(childW, (h - topInset).coerceAtLeast(0f)),
                    cornerRadius = r
                )
                drawRoundRect(
                    color = border,
                    topLeft = Offset(0f, topInset),
                    size = Size(childW, (h - topInset).coerceAtLeast(0f)),
                    cornerRadius = r,
                    style = stroke
                )

                // Right panel chrome
                val rightX = (childW + gapPx).coerceAtLeast(0f)
                drawRoundRect(
                    color = bg,
                    topLeft = Offset(rightX, topInset),
                    size = Size(childW, (h - topInset).coerceAtLeast(0f)),
                    cornerRadius = r
                )
                drawRoundRect(
                    color = border,
                    topLeft = Offset(rightX, topInset),
                    size = Size(childW, (h - topInset).coerceAtLeast(0f)),
                    cornerRadius = r,
                    style = stroke
                )

                // True "split border around text" using a Clear blend-mode cutout.
                val cutH = maxOf(borderPx * 4f, legendHeightPx)
                val cutY = (topInset - cutH / 2f).coerceAtLeast(0f)

                val leftStart = (padX - legendErasePadX).coerceAtLeast(0f)
                val leftEnd = (padX + leftLegendWidthPx + legendErasePadX).coerceAtMost(childW)
                if (leftEnd > leftStart) {
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(leftStart, cutY),
                        size = Size(leftEnd - leftStart, cutH),
                        blendMode = BlendMode.Clear,
                    )
                }

                val rightStart = (rightX + padX - legendErasePadX).coerceAtLeast(rightX)
                val rightEnd = (rightX + padX + rightLegendWidthPx + legendErasePadX).coerceAtMost(rightX + childW)
                if (rightEnd > rightStart) {
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(rightStart, cutY),
                        size = Size(rightEnd - rightStart, cutH),
                        blendMode = BlendMode.Clear,
                    )
                }

                // Draw labels/content on top of chrome + cutout.
                drawContent()
            },
        content = {
            // left label, left body, right label, right body
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = leftTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
            Box { leftContent() }
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = rightTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
            Box { rightContent() }
        }
    ) { measurables, constraints ->
        val gapPxI = gap.roundToPx()
        val availableWidth = (constraints.maxWidth - gapPxI).coerceAtLeast(0)
        val childWidth = (availableWidth / 2).coerceAtLeast(0)
        val innerPadX = (padX.toInt() * 2).coerceAtLeast(0)
        val contentWidth = (childWidth - innerPadX).coerceAtLeast(0)
        // Body is constrained to the "inside" width of the drawn panel (excluding horizontal padding).
        // Label should wrap its text (no forced minWidth), but still be limited by the same maxWidth.
        val bodyConstraints = constraints.copy(minWidth = contentWidth, maxWidth = contentWidth)
        val labelConstraints = constraints.copy(minWidth = 0, maxWidth = contentWidth)

        val leftLabel = measurables[0].measure(labelConstraints)
        val leftBody = measurables[1].measure(bodyConstraints)
        val rightLabel = measurables[2].measure(labelConstraints)
        val rightBody = measurables[3].measure(bodyConstraints)

        // Store legend metrics so drawBehind can "break" the border under the label.
        leftLegendWidthPx = leftLabel.width.toFloat()
        rightLegendWidthPx = rightLabel.width.toFloat()
        legendHeightPx = maxOf(leftLabel.height, rightLabel.height).toFloat()
        legendHalfPx = legendHeightPx / 2f

        fun requiredHeight(labelH: Int, bodyH: Int): Int {
            val topInset = legendHalfPx.toInt()
            val top = topInset + padY.toInt() + (labelH / 2) + labelBodyGap.toInt()
            val bottom = padY.toInt()
            return top + bodyH + bottom
        }

        val leftRequired = requiredHeight(leftLabel.height, leftBody.height)
        val rightRequired = requiredHeight(rightLabel.height, rightBody.height)
        val height = maxOf(leftRequired, rightRequired)

        layout(constraints.maxWidth, height) {
            val leftX = 0
            val rightX = childWidth + gapPxI

            val topInset = legendHalfPx.toInt()
            val labelGapY = labelBodyGap.toInt()
            // Floating label centered on the panel's top border line (at y = topInset).
            val leftLabelY = topInset - (leftLabel.height / 2)
            val rightLabelY = topInset - (rightLabel.height / 2)

            val leftMinBodyTop = topInset + padY.toInt() + (leftLabel.height / 2) + labelGapY
            val rightMinBodyTop = topInset + padY.toInt() + (rightLabel.height / 2) + labelGapY
            val bottomY = (height - padY.toInt())

            val leftBodyY = maxOf(bottomY - leftBody.height, leftMinBodyTop)
            val rightBodyY = maxOf(bottomY - rightBody.height, rightMinBodyTop)

            val padXi = padX.toInt()
            leftLabel.placeRelative(leftX + padXi, leftLabelY)
            leftBody.placeRelative(leftX + padXi, leftBodyY)
            rightLabel.placeRelative(rightX + padXi, rightLabelY)
            rightBody.placeRelative(rightX + padXi, rightBodyY)
        }
    }
}