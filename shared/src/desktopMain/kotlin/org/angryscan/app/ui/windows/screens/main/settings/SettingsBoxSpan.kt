package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ширина левой колонки «группа» в табличных блоках (extensions / detection rules). */
object SettingsScanTable {
    val groupLabelWidth = 140.dp
    /** Зазор после «…:» перед кнопкой «Выбрать все» в заголовке секции. */
    val headerInlineActionSpacing = 12.dp
    /** Зазор между заголовком секции и строками «таблицы» (группы / чипы). */
    val contentBelowHeaderSpacing = 6.dp
}

@Composable
private fun settingsTableGridLineColor() =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)

/** Горизонтальная линия между строками таблицы — минимальный зазор, без «высоких» полей. */
@Composable
internal fun SettingsTableBetweenRowsDivider(dense: Boolean) {
    val pad = if (dense) 1.dp else 2.dp
    HorizontalDivider(
        modifier = Modifier.padding(vertical = pad),
        color = settingsTableGridLineColor(),
        thickness = 1.dp
    )
}

/**
 * Строка "таблицы" scan settings с горизонтальным разделителем, ширина которого равна ширине строки.
 *
 * Важно: разделитель рисуется *после* строки (как "underline"), чтобы иметь доступ к измеренной ширине.
 */
@Composable
internal fun SettingsTableRowWithContentWidthDivider(
    dense: Boolean,
    showDividerBelow: Boolean,
    modifier: Modifier = Modifier,
    row: @Composable () -> Unit,
) {
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val rowWidthDp = with(density) { rowWidthPx.toDp() }

    Column(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .onSizeChanged { rowWidthPx = it.width }
            ) {
                row()
            }
        }

        if (showDividerBelow && rowWidthPx > 0) {
            val pad = if (dense) 1.dp else 2.dp
            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = pad)
                    .width(rowWidthDp),
                color = settingsTableGridLineColor(),
                thickness = 1.dp
            )
        }
    }
}

/** Вертикальная линия между колонкой группы и чипами ([Row] должен иметь [Modifier.height(IntrinsicSize.Min)]). */
@Composable
internal fun SettingsTableColumnDivider() {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight(),
        color = settingsTableGridLineColor(),
        thickness = 1.dp
    )
}

private val SectionCardShape = RoundedCornerShape(12.dp)
private val SectionHeaderPadding = 14.dp
private val SectionContentPadding = 14.dp
private val SectionHeaderTextAlignToPathEditOffset = 14.dp

@Composable
private fun SettingsSectionHeaderRow(
    title: String,
    titleTrailing: @Composable () -> Unit,
    /** Ставит [titleTrailing] сразу после заголовка (после двоеточия), без выравнивания вправо. */
    titleTrailingInline: Boolean = false,
    titleTrailingInlineSpacing: Dp = 2.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = if (titleTrailingInline) Arrangement.Start else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (titleTrailingInline) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.offset(x = SectionHeaderTextAlignToPathEditOffset),
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .offset(x = SectionHeaderTextAlignToPathEditOffset)
            )
        }
        if (titleTrailingInline) {
            Spacer(Modifier.width(titleTrailingInlineSpacing))
        }
        titleTrailing()
    }
}

/** Одна общая рамка для scan settings: расширения, правила, сигнатуры. */
@Composable
fun SettingsScanUnifiedPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = SectionCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = SectionHeaderPadding,
                    end = SectionHeaderPadding,
                    top = 6.dp,
                    bottom = 3.dp
                ),
            content = content
        )
    }
}

/**
 * Подпункт внутри [SettingsScanUnifiedPanel]: заголовок и контент без отдельной карточки.
 */
@Composable
fun SettingsUnifiedSubsection(
    title: String,
    modifier: Modifier = Modifier,
    expandContentVertically: Boolean = false,
    contentTopPadding: Dp = SettingsScanTable.contentBelowHeaderSpacing,
    titleTrailing: @Composable () -> Unit = {},
    titleTrailingInline: Boolean = false,
    titleTrailingInlineSpacing: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expandContentVertically) {
        Column(modifier.fillMaxWidth().fillMaxHeight()) {
            SettingsSectionHeaderRow(
                title,
                titleTrailing,
                titleTrailingInline,
                titleTrailingInlineSpacing,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = SectionHeaderTextAlignToPathEditOffset,
                        top = contentTopPadding,
                        bottom = 2.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                content = content
            )
        }
    } else {
        Column(modifier.fillMaxWidth()) {
            SettingsSectionHeaderRow(
                title,
                titleTrailing,
                titleTrailingInline,
                titleTrailingInlineSpacing,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SectionHeaderTextAlignToPathEditOffset,
                        top = contentTopPadding,
                        bottom = 2.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                content = content
            )
        }
    }
}

/** Секция настроек без карточки: заголовок и контент, только типографика и отступы. */
@Composable
fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    /** Когда `true`, карточка забирает выделенную по высоте область (оставшееся место в колонке настроек). */
    expandContentVertically: Boolean = false,
    contentTopPadding: Dp = SettingsScanTable.contentBelowHeaderSpacing,
    titleTrailing: @Composable () -> Unit = {},
    titleTrailingInline: Boolean = false,
    titleTrailingInlineSpacing: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SectionCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        if (expandContentVertically) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SectionHeaderPadding, vertical = 3.dp)
            ) {
                SettingsSectionHeaderRow(
                    title,
                    titleTrailing,
                    titleTrailingInline,
                    titleTrailingInlineSpacing,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = SectionHeaderTextAlignToPathEditOffset,
                            top = contentTopPadding,
                            bottom = 2.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    content = content
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SectionHeaderPadding, vertical = 3.dp)
            ) {
                SettingsSectionHeaderRow(
                    title,
                    titleTrailing,
                    titleTrailingInline,
                    titleTrailingInlineSpacing,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SectionHeaderTextAlignToPathEditOffset,
                            top = contentTopPadding,
                            bottom = 2.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun SettingsBoxSpan(
    text: String,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    textTail: @Composable () -> Unit = {},
    block: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SectionCardShape)
                    .clickable { onExpandClick() }
                    .padding(horizontal = SectionHeaderPadding, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    textTail()
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SectionContentPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    block()
                }
            }
        }
    }
}
