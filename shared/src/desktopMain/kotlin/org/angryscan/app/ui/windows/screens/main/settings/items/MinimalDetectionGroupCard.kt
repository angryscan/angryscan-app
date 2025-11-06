package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.*
import org.angryscan.app.ui.strings.composableName

@Composable
fun MinimalDetectionGroupCard(
    group: MatchersGroup,
    scanSettings: ScanSettings
) {
    var groupExpanded by remember { mutableStateOf(false) }

    val groupIcon = when (group.name) {
        stringResource(Res.string.DetectGroup_PersonalDataNumbers) -> Icons.Default.Person
        stringResource(Res.string.DetectGroup_PersonalDataText) -> Icons.Default.Description
        stringResource(Res.string.DetectGroup_BankingSecrecy) -> Icons.Default.Security
        stringResource(Res.string.DetectGroup_ITAssets) -> Icons.Default.Storage
        else -> Icons.Default.Category
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isFullySelected = isGroupFullySelected(group, scanSettings)
    val isPartiallySelected = isGroupPartiallySelected(group, scanSettings)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource),
        shape = RoundedCornerShape(10.dp),
        color = if (isHovered)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shadowElevation = if (isHovered) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { groupExpanded = !groupExpanded },
                color = if (isHovered)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                else
                    Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isHovered)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Checkbox(
                            checked = isFullySelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    group.matchers.forEach { matcher ->
                                        if (!scanSettings.matchers.any {
                                                it::class == matcher::class
                                            }) {
                                            scanSettings.matchers.add(matcher)
                                        }
                                    }
                                } else {
                                    group.matchers.forEach { function ->
                                        scanSettings.matchers.remove(function)
                                    }
                                }
                                scanSettings.save()
                            },
                            modifier = Modifier.size(18.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = if (isPartiallySelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isHovered)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = groupIcon,
                            contentDescription = null,
                            tint = if (isHovered)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(4.dp)
                        )
                    }

                    Text(
                        text = group.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isHovered)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (groupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = if (isHovered)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { groupExpanded = !groupExpanded }
                    )
                }
            }

            AnimatedVisibility(
                visible = groupExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                val totalItems = group.matchers.size

                val allFunctionNames = mutableListOf<String>()

                group.matchers.forEach { function ->
                    allFunctionNames.add(function.composableName())
                }

                var containerWidth by remember { mutableStateOf<Dp?>(null) }
                val averageLength = allFunctionNames.map { it.length }.average()
                val maxLength = allFunctionNames.maxOfOrNull { it.length } ?: 0

                val columns = remember(containerWidth, allFunctionNames, totalItems, group.name) {
                    when {
                        averageLength < 6 && maxLength < 10 -> {
                            when {
                                totalItems <= 2 -> 2
                                totalItems <= 4 -> 3
                                totalItems <= 6 -> 4
                                totalItems <= 8 -> 5
                                else -> 6
                            }
                        }

                        averageLength < 10 && maxLength < 15 -> {
                            when {
                                totalItems <= 2 -> 2
                                totalItems <= 4 -> 3
                                totalItems <= 6 -> 4
                                totalItems <= 8 -> 5
                                else -> 4
                            }
                        }

                        averageLength < 15 && maxLength < 25 -> {
                            when {
                                totalItems <= 2 -> 2
                                totalItems <= 4 -> 3
                                totalItems <= 6 -> 3
                                totalItems <= 8 -> 4
                                else -> 3
                            }
                        }

                        averageLength < 25 && maxLength < 40 -> {
                            when {
                                totalItems <= 2 -> 2
                                totalItems <= 4 -> 2
                                totalItems <= 6 -> 3
                                totalItems <= 8 -> 3
                                else -> 2
                            }
                        }

                        else -> {
                            when {
                                totalItems <= 2 -> 1
                                totalItems <= 4 -> 2
                                totalItems <= 6 -> 2
                                totalItems <= 8 -> 2
                                else -> 1
                            }
                        }
                    }.coerceIn(1, 6)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .onSizeChanged { size ->
                            containerWidth = size.width.dp
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val allItems = mutableListOf<IMatcher>()
                    allItems.addAll(group.matchers)

                    allItems.chunked(columns).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ModernDetectionFunctionItem(
                                        matcher = item,
                                        scanSettings = scanSettings
                                    )
                                }
                            }

                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}