package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedRoute = when {
        destination?.hasRoute(MainScreenConnector.FileShare::class) == true -> MainScreenConnector.FileShare
        destination?.hasRoute(MainScreenConnector.S3::class) == true -> MainScreenConnector.S3
        destination?.hasRoute(MainScreenConnector.HTTP::class) == true -> MainScreenConnector.HTTP
        destination?.hasRoute(MainScreenConnector.Postgres::class) == true -> MainScreenConnector.Postgres
        else -> MainScreenConnector.FileShare
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceSelectorTabItem(
                modifier = Modifier.weight(1f),
                isSelected = selectedRoute == MainScreenConnector.FileShare,
                label = "File Share",
                sourceType = SourceSelectorTabType.FileShare,
                onClick = {
                    if (selectedRoute != MainScreenConnector.FileShare) {
                        navController.navigate(MainScreenConnector.FileShare)
                    }
                }
            )
            SourceSelectorTabItem(
                modifier = Modifier.weight(1f),
                isSelected = selectedRoute == MainScreenConnector.S3,
                label = "AWS S3",
                sourceType = SourceSelectorTabType.S3,
                onClick = {
                    if (selectedRoute != MainScreenConnector.S3) {
                        navController.navigate(MainScreenConnector.S3)
                    }
                }
            )
            SourceSelectorTabItem(
                modifier = Modifier.weight(1f),
                isSelected = selectedRoute == MainScreenConnector.HTTP,
                label = "HTTP",
                sourceType = SourceSelectorTabType.HTTP,
                onClick = {
                    if (selectedRoute != MainScreenConnector.HTTP) {
                        navController.navigate(MainScreenConnector.HTTP)
                    }
                }
            )
            SourceSelectorTabItem(
                modifier = Modifier.weight(1f),
                isSelected = selectedRoute == MainScreenConnector.Postgres,
                label = "SQL Database",
                sourceType = SourceSelectorTabType.Postgres,
                onClick = {
                    if (selectedRoute != MainScreenConnector.Postgres) {
                        navController.navigate(MainScreenConnector.Postgres)
                    }
                }
            )
        }
    }
}

private enum class SourceSelectorTabType { FileShare, S3, HTTP, Postgres }

@Composable
private fun SourceSelectorTabItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    sourceType: SourceSelectorTabType,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val alpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isHovered -> 0.9f
            else -> 0.6f
        },
        animationSpec = tween(200),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .alpha(alpha)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        tonalElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (sourceType) {
                SourceSelectorTabType.FileShare -> {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                SourceSelectorTabType.S3 -> {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                SourceSelectorTabType.HTTP -> {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                SourceSelectorTabType.Postgres -> {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
