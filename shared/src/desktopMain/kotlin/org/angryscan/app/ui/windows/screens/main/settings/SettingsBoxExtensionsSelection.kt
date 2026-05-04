package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_FileExtensions
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.LocalMainScreenAdaptiveTokens
import org.angryscan.app.ui.windows.screens.main.settings.items.SettingsSelectAllTextButton
import org.jetbrains.compose.resources.stringResource

private data class ExtensionsGroup(
    val title: String,
    val typeNames: Set<String>,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsBoxExtensionsSelection(
    scanSettings: ScanSettings,
    showTitle: Boolean = true,
    unifiedBlock: Boolean = false,
    errorHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fileTypeEntries = remember {
        IFileType.getAll().filter { it !in CodeFileType.entries && it !in CertFileType.entries }
    }
    val allSelected = scanSettings.extensions.containsAll(fileTypeEntries)

    val dense = unifiedBlock
    val settingsTokens = LocalMainScreenAdaptiveTokens.current.settings
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    val groupLabelWidth = if (dense) adaptiveScale.dp(108.dp, min = 102.dp, max = 160.dp) else settingsTokens.groupLabelWidth
    val groupRowHGap = if (dense) adaptiveScale.dp(5.dp, min = 4.dp, max = 9.dp) else adaptiveScale.dp(10.dp, min = 8.dp, max = 16.dp)
    val chipCorner = if (dense) adaptiveScale.dp(4.dp, min = 3.dp, max = 7.dp) else adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp)
    val chipPadH = if (dense) adaptiveScale.dp(1.dp, min = 1.dp, max = 2.dp) else adaptiveScale.dp(2.dp, min = 1.dp, max = 4.dp)
    val chipLabelFs = if (dense) adaptiveScale.sp(8.sp, min = 8.sp, max = 10.sp) else adaptiveScale.sp(10.sp, min = 10.sp, max = 12.sp)
    val groupLabelFs = if (dense) adaptiveScale.sp(8.5.sp, min = 8.sp, max = 10.sp) else adaptiveScale.sp(10.sp, min = 10.sp, max = 12.sp)
    val flowHGap = if (dense) adaptiveScale.dp(7.dp, min = 6.dp, max = 12.dp) else adaptiveScale.dp(9.dp, min = 8.dp, max = 14.dp)
    val flowVGap = if (dense) adaptiveScale.dp(5.dp, min = 4.dp, max = 9.dp) else adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp)
    // Сейчас общий левый отступ секции даёт 4.dp (внутренний padding карточки).
    // Чтобы визуально получить "в 3 раза больше" слева в первой колонке, добавляем ещё +8.dp.
    val firstColumnExtraStartPadding = adaptiveScale.dp(8.dp, min = 6.dp, max = 14.dp)

    val content: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.wrapContentWidth()) {
            val groups = remember {
                listOf(
                    ExtensionsGroup(title = "word", typeNames = setOf("DOCX", "DOC", "ODT")),
                    ExtensionsGroup(title = "text", typeNames = setOf("Text")),
                    ExtensionsGroup(title = "presentations", typeNames = setOf("PPTX", "PPT", "ODP")),
                    ExtensionsGroup(title = "tables", typeNames = setOf("XLSX", "XLS", "ODS")),
                    ExtensionsGroup(title = "archives", typeNames = setOf("ZIP", "RAR")),
                )
            }

            val visibleGroups = remember(fileTypeEntries) {
                groups.mapNotNull { group ->
                    val groupTypes = fileTypeEntries.filter { it.name in group.typeNames }
                    if (groupTypes.isEmpty()) null else group to groupTypes
                }
            }

            visibleGroups.forEachIndexed { idx, (group, groupTypes) ->
                SettingsTableRowWithContentWidthDivider(
                    dense = dense,
                    showDividerBelow = idx != visibleGroups.lastIndex,
                ) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(groupRowHGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.title,
                            modifier = Modifier
                                .width(groupLabelWidth)
                                .padding(start = firstColumnExtraStartPadding),
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = groupLabelFs,
                            lineHeight = if (dense) 10.sp else TextUnit.Unspecified,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        SettingsTableColumnDivider()
                        FlowRow(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(flowHGap),
                            verticalArrangement = Arrangement.spacedBy(flowVGap)
                        ) {
                            for (fileType in groupTypes) {
                                val selected = scanSettings.extensions.contains(fileType)
                                DescriptionTooltip(description = fileType.name) {
                                    SettingsFlowToggleChip(
                                        selected = selected,
                                        onClick = {
                                            if (selected) scanSettings.extensions.remove(fileType)
                                            else scanSettings.extensions.add(fileType)
                                            scanSettings.save()
                                        },
                                        label = fileType.name,
                                        chipCorner = chipCorner,
                                        chipPadH = chipPadH,
                                        chipLabelFs = chipLabelFs
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showTitle) {
        if (unifiedBlock) {
            SettingsUnifiedSubsection(
                title = stringResource(Res.string.ScanSettings_FileExtensions),
                modifier = modifier.then(
                    if (errorHighlight) {
                        Modifier.border(
                            width = adaptiveScale.dp(1.5.dp, min = 1.2.dp, max = 2.2.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(adaptiveScale.dp(10.dp, min = 8.dp, max = 14.dp))
                        )
                    } else {
                        Modifier
                    }
                ),
                contentTopPadding = (settingsTokens.contentBelowHeaderSpacing * 3) / 2f,
                titleTrailingInline = true,
                titleTrailingInlineSpacing = settingsTokens.headerInlineActionSpacing,
                titleTrailing = {
                    SettingsSelectAllTextButton(
                        allSelected = allSelected,
                        onClick = {
                            if (allSelected) scanSettings.extensions.clear()
                            else scanSettings.extensions.addAll(fileTypeEntries.filter { it !in scanSettings.extensions })
                            scanSettings.save()
                        }
                    )
                },
                content = content
            )
        } else {
            SettingsSectionCard(
                title = stringResource(Res.string.ScanSettings_FileExtensions),
                modifier = modifier,
                contentTopPadding = (settingsTokens.contentBelowHeaderSpacing * 3) / 2f,
                titleTrailingInline = true,
                titleTrailingInlineSpacing = settingsTokens.headerInlineActionSpacing,
                titleTrailing = {
                    SettingsSelectAllTextButton(
                        allSelected = allSelected,
                        onClick = {
                            if (allSelected) scanSettings.extensions.clear()
                            else scanSettings.extensions.addAll(fileTypeEntries.filter { it !in scanSettings.extensions })
                            scanSettings.save()
                        }
                    )
                },
                content = content
            )
        }
    } else {
        Column(modifier = modifier, content = content)
    }
}
