package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
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
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.aws_s3
import org.jetbrains.compose.resources.painterResource

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

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceSelectorItem(
            modifier = Modifier.weight(1f),
            isSelected = selectedRoute == MainScreenConnector.FileShare,
            label = "File Share",
            sourceType = SourceType.FileShare,
            onClick = {
                if (selectedRoute != MainScreenConnector.FileShare) {
                    navController.navigate(MainScreenConnector.FileShare)
                }
            }
        )
        SourceSelectorItem(
            modifier = Modifier.weight(1f),
            isSelected = selectedRoute == MainScreenConnector.S3,
            label = "AWS S3",
            sourceType = SourceType.S3,
            onClick = {
                if (selectedRoute != MainScreenConnector.S3) {
                    navController.navigate(MainScreenConnector.S3)
                }
            }
        )
        SourceSelectorItem(
            modifier = Modifier.weight(1f),
            isSelected = selectedRoute == MainScreenConnector.HTTP,
            label = "HTTP",
            sourceType = SourceType.HTTP,
            onClick = {
                if (selectedRoute != MainScreenConnector.HTTP) {
                    navController.navigate(MainScreenConnector.HTTP)
                }
            }
        )
    }
}

private enum class SourceType { FileShare, S3, HTTP }

@Composable
private fun RowScope.SourceSelectorItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    sourceType: SourceType,
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
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        border = if (isSelected) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (sourceType) {
                SourceType.FileShare -> {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                SourceType.S3 -> {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.aws_s3),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                SourceType.HTTP -> {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}
