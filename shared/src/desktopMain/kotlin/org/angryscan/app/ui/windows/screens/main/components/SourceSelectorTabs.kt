package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Селектор источника в виде табов: иконка + подпись в ряд.
 * File Share, AWS S3, HTTP — каждый таб занимает равную ширину.
 * (Для S3 используется Cloud; можно заменить на aws-s3.png через painterResource)
 */
@Composable
fun SourceSelectorTabs(
    navController: NavController,
    modifier: Modifier = Modifier,
    onLabelClick: () -> Unit = {},
    settingsModeActive: Boolean = false,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedRoute = when {
        destination?.hasRoute(MainScreenConnector.FileShare::class) == true -> MainScreenConnector.FileShare
        destination?.hasRoute(MainScreenConnector.S3::class) == true -> MainScreenConnector.S3
        destination?.hasRoute(MainScreenConnector.HTTP::class) == true -> MainScreenConnector.HTTP
        else -> MainScreenConnector.FileShare
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceSelectorTabItem(
            modifier = Modifier,
            isSelected = selectedRoute == MainScreenConnector.FileShare,
            label = "File Share",
            sourceType = SourceSelectorTabType.FileShare,
            settingsModeActive = settingsModeActive,
            onClick = {
                if (selectedRoute != MainScreenConnector.FileShare) {
                    navController.navigate(MainScreenConnector.FileShare)
                }
            },
            onLabelClick = onLabelClick
        )
        SourceSelectorTabItem(
            modifier = Modifier,
            isSelected = selectedRoute == MainScreenConnector.S3,
            label = "AWS S3",
            sourceType = SourceSelectorTabType.S3,
            settingsModeActive = settingsModeActive,
            onClick = {
                if (selectedRoute != MainScreenConnector.S3) {
                    navController.navigate(MainScreenConnector.S3)
                }
            },
            onLabelClick = onLabelClick
        )
        SourceSelectorTabItem(
            modifier = Modifier,
            isSelected = selectedRoute == MainScreenConnector.HTTP,
            label = "HTTP",
            sourceType = SourceSelectorTabType.HTTP,
            settingsModeActive = settingsModeActive,
            onClick = {
                if (selectedRoute != MainScreenConnector.HTTP) {
                    navController.navigate(MainScreenConnector.HTTP)
                }
            },
            onLabelClick = onLabelClick
        )
    }
}

private enum class SourceSelectorTabType { FileShare, S3, HTTP }

@Composable
private fun RowScope.SourceSelectorTabItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    sourceType: SourceSelectorTabType,
    onClick: () -> Unit,
    onLabelClick: () -> Unit,
    settingsModeActive: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val expanded = isHovered || isSelected
    val highlightProgress by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isHovered -> 0.45f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = 0.96f,
            stiffness = 340f
        ),
        label = "sourceHighlightProgress"
    )
    val animatedInnerWidth by animateDpAsState(
        targetValue = if (expanded) 200.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = 0.95f,
            stiffness = 320f
        ),
        label = "sourceInnerWidth"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.95f,
            stiffness = 340f
        ),
        label = "sourceLabelAlpha"
    )
    val labelInteractionSource = remember { MutableInteractionSource() }
    val isLabelHovered by labelInteractionSource.collectIsHoveredAsState()
    val animatedLabelWidth by animateDpAsState(
        targetValue = if (expanded) 126.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.95f,
            stiffness = 320f
        ),
        label = "sourceLabelWidth"
    )
    val iconHaloSize by animateDpAsState(
        targetValue = if (isSelected) 30.dp else if (isHovered) 26.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = 0.96f,
            stiffness = 360f
        ),
        label = "sourceIconHaloSize"
    )

    Box(
        modifier = modifier
            .width(210.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(animatedInnerWidth)
                .align(Alignment.Center)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconHaloSize)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f + 0.13f * highlightProgress)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (sourceType) {
                    SourceSelectorTabType.FileShare -> {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (expanded) 0.9f else 0.72f)
                        )
                    }
                    SourceSelectorTabType.S3 -> {
                        Icon(
                            imageVector = Icons.Outlined.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (expanded) 0.9f else 0.72f)
                        )
                    }
                    SourceSelectorTabType.HTTP -> {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (expanded) 0.9f else 0.72f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.width(animatedLabelWidth),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textDecoration = if (isLabelHovered || (settingsModeActive && isSelected)) TextDecoration.Underline else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = labelAlpha)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f * labelAlpha),
                    modifier = Modifier
                        .hoverable(interactionSource = labelInteractionSource)
                        .clickable(
                            interactionSource = labelInteractionSource,
                            indication = null,
                            onClick = onLabelClick
                        )
                )
            }
        }
    }
}
