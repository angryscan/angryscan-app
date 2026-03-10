package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState

private val SIDEBAR_WIDTH = 420.dp
private val ITEM_HEIGHT = 56.dp
private val ACCENT_BAR_WIDTH = 4.dp
private val ITEM_RADIUS = 14.dp
private val CONTAINER_RADIUS = 24.dp

@Composable
fun MainScreenSidebar(
    navController: NavController,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {}
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedRoute = when {
        destination?.hasRoute(MainScreenConnector.FileShare::class) == true -> MainScreenConnector.FileShare
        destination?.hasRoute(MainScreenConnector.S3::class) == true -> MainScreenConnector.S3
        destination?.hasRoute(MainScreenConnector.HTTP::class) == true -> MainScreenConnector.HTTP
        else -> MainScreenConnector.FileShare
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(CONTAINER_RADIUS)
    Column(
        modifier = modifier
            .width(SIDEBAR_WIDTH)
            .fillMaxHeight()
            .clip(containerShape)
            .background(colorScheme.surface.copy(alpha = 0.6f), containerShape)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = containerShape
            )
            .padding(vertical = 20.dp, horizontal = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SidebarNavItem(
            isSelected = selectedRoute == MainScreenConnector.FileShare,
            label = "File Share",
            icon = Icons.Outlined.FolderOpen,
            onClick = {
                if (selectedRoute != MainScreenConnector.FileShare) {
                    navController.navigate(MainScreenConnector.FileShare)
                }
            }
        )
        SidebarNavItem(
            isSelected = selectedRoute == MainScreenConnector.S3,
            label = "AWS S3",
            icon = Icons.Outlined.Cloud,
            onClick = {
                if (selectedRoute != MainScreenConnector.S3) {
                    navController.navigate(MainScreenConnector.S3)
                }
            }
        )
        SidebarNavItem(
            isSelected = selectedRoute == MainScreenConnector.HTTP,
            label = "HTTP",
            icon = Icons.Outlined.Link,
            onClick = {
                if (selectedRoute != MainScreenConnector.HTTP) {
                    navController.navigate(MainScreenConnector.HTTP)
                }
            }
        )
        }

        Spacer(modifier = Modifier.weight(1f))

        extraContent()
    }
}

@Composable
private fun SidebarNavItem(
    isSelected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(200),
        label = "bg"
    )
    val accentWidth by animateDpAsState(
        targetValue = if (isSelected) ACCENT_BAR_WIDTH else 0.dp,
        animationSpec = tween(200),
        label = "accent"
    )
    val colorScheme = MaterialTheme.colorScheme
    val isLightTheme = (colorScheme.surface.red + colorScheme.surface.green + colorScheme.surface.blue) / 3f >= 0.5f
    val itemShape = RoundedCornerShape(ITEM_RADIUS)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .clip(itemShape)
            .then(
                if (isLightTheme) Modifier.border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = itemShape
                ) else Modifier
            )
            .background(backgroundColor)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accentWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .width(accentWidth)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(12.dp + ACCENT_BAR_WIDTH))
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}
