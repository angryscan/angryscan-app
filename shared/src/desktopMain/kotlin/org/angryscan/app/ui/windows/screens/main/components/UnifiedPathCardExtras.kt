package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

data class UnifiedPathCardExtras(
    val advancedExpanded: Boolean,
    val onAdvancedExpandedChange: (Boolean) -> Unit,
    val settingsContent: @Composable () -> Unit,
    val maxAdvancedHeight: Dp = 420.dp
)

private val ToggleShape = RoundedCornerShape(14.dp)
private val PanelShape = RoundedCornerShape(16.dp)

@Composable
fun InlineAdvancedSettingsInPathCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    settingsContent: @Composable () -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val expandRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "path_adv_rot"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(10.dp))

        // Плашка без выделения: заголовок, Fast scan, раскрытие по клику на заголовок или «Настроить»
        val scanSettings = koinInject<ScanSettings>()
        val fastScan by scanSettings.fastScan
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ToggleShape)
                .hoverable(interactionSource = interactionSource),
            shape = ToggleShape,
            color = cs.surfaceVariant.copy(alpha = 0.35f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.62f)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onExpandedChange(!expanded) }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            cs.primary.copy(alpha = 0.18f),
                                            cs.primary.copy(alpha = 0.08f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = cs.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = stringResource(Res.string.MainLayout_PathCardTitle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.MainLayout_PathCardSubtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant.copy(alpha = 0.88f),
                                maxLines = 1
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                        Text(
                            text = stringResource(Res.string.ScanSettings_FastScan),
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                        Switch(
                            checked = fastScan,
                            onCheckedChange = {
                                scanSettings.fastScan.value = it
                                scanSettings.save()
                            },
                            modifier = Modifier.height(28.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cs.primary,
                                checkedTrackColor = cs.primaryContainer,
                                uncheckedThumbColor = cs.outlineVariant,
                                uncheckedTrackColor = cs.surfaceVariant
                            )
                        )
                        }

                        Row(
                            modifier = Modifier
                                .width(130.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onExpandedChange(!expanded) }
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                            Text(
                                text = stringResource(
                                    if (expanded) {
                                        Res.string.MainLayout_PathCardActionHide
                                    } else {
                                        Res.string.MainLayout_PathCardActionCustomize
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = cs.primary
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(cs.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = expandRotation },
                                    tint = cs.primary
                                )
                            }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .heightIn(max = maxHeight)
            ) {
                settingsContent()
            }
        }
    }
}
