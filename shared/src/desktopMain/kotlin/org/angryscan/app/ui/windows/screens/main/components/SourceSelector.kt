package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay

private enum class SourceType(val tooltip: String) {
    FileShare("File share"),
    S3("AWS S3"),
    HTTP("Web scanning")
}

@Composable
fun SourceSelector(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedRoute = when {
        destination?.hasRoute(MainScreenConnector.FileShare::class) == true -> MainScreenConnector.FileShare
        destination?.hasRoute(MainScreenConnector.S3::class) == true -> MainScreenConnector.S3
        destination?.hasRoute(MainScreenConnector.HTTP::class) == true -> MainScreenConnector.HTTP
        else -> MainScreenConnector.FileShare
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingSourceIcon(
                isSelected = selectedRoute == MainScreenConnector.FileShare,
                sourceType = SourceType.FileShare,
                onClick = {
                    if (selectedRoute != MainScreenConnector.FileShare) {
                        navController.navigate(MainScreenConnector.FileShare)
                    }
                }
            )
            FloatingSourceIcon(
                isSelected = selectedRoute == MainScreenConnector.S3,
                sourceType = SourceType.S3,
                onClick = {
                    if (selectedRoute != MainScreenConnector.S3) {
                        navController.navigate(MainScreenConnector.S3)
                    }
                }
            )
            FloatingSourceIcon(
                isSelected = selectedRoute == MainScreenConnector.HTTP,
                sourceType = SourceType.HTTP,
                onClick = {
                    if (selectedRoute != MainScreenConnector.HTTP) {
                        navController.navigate(MainScreenConnector.HTTP)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FloatingSourceIcon(
    isSelected: Boolean,
    sourceType: SourceType,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var pointerX by remember { mutableStateOf(20f) }
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            delay(500)
            showTooltip = true
        } else {
            showTooltip = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isHovered -> 0.9f
            else -> 0.65f
        },
        animationSpec = tween(200),
        label = "iconAlpha"
    )

    val (icon, contentDesc) = when (sourceType) {
        SourceType.FileShare -> Icons.Outlined.FolderOpen to "File share"
        SourceType.S3 -> Icons.Outlined.Cloud to "AWS S3"
        SourceType.HTTP -> Icons.Outlined.Link to "Web scanning"
    }

    val density = LocalDensity.current
    val iconSizePx = with(density) { 40.dp.roundToPx() }
    val iconCenterPx = iconSizePx / 2f
    val tooltipOffset = IntOffset(
        x = (pointerX - iconCenterPx).toInt(),
        y = iconSizePx + with(density) { 8.dp.roundToPx() }
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isHovered -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .onPointerEvent(PointerEventType.Move) {
                pointerX = it.changes.firstOrNull()?.position?.x ?: 20f
            }
            .onPointerEvent(PointerEventType.Exit) {
                pointerX = 20f
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center),
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = tooltipOffset,
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraSmall,
                    tonalElevation = 10.dp,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = sourceType.tooltip,
                        modifier = Modifier.padding(8.dp),
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            }
        }
    }
}
