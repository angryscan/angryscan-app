package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import org.angryscan.app.resources.MainScreen_SourceType_FileShare
import org.angryscan.app.resources.MainScreen_SourceType_HTTP
import org.angryscan.app.resources.MainScreen_SourceType_S3
import org.angryscan.app.resources.Res
import org.jetbrains.compose.resources.stringResource

/**
 * Выбор источника данных: отличается от верхнего меню навигации —
 * секция с подписью + компактные чипы с обводкой (secondary), не «второй ряд табов».
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
        else -> MainScreenConnector.FileShare
    }

    val options = listOf(
        Triple(
            MainScreenConnector.FileShare,
            Icons.Outlined.FolderOpen,
            stringResource(Res.string.MainScreen_SourceType_FileShare)
        ),
        Triple(
            MainScreenConnector.S3,
            Icons.Outlined.Cloud,
            stringResource(Res.string.MainScreen_SourceType_S3)
        ),
        Triple(
            MainScreenConnector.HTTP,
            Icons.Outlined.Link,
            stringResource(Res.string.MainScreen_SourceType_HTTP)
        )
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (route, icon, label) ->
            SegmentedButton(
                selected = selectedRoute == route,
                onClick = {
                    if (selectedRoute != route) navController.navigate(route)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(text = label, maxLines = 1) }
            )
        }
    }
}

@Composable
fun SourceSelectorSideRail(
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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceChip(
            modifier = Modifier.weight(1f),
            selected = selectedRoute == MainScreenConnector.FileShare,
            label = stringResource(Res.string.MainScreen_SourceType_FileShare),
            icon = Icons.Outlined.FolderOpen,
            onClick = {
                if (selectedRoute != MainScreenConnector.FileShare) {
                    navController.navigate(MainScreenConnector.FileShare)
                }
            }
        )
        SourceChip(
            modifier = Modifier.weight(1f),
            selected = selectedRoute == MainScreenConnector.S3,
            label = stringResource(Res.string.MainScreen_SourceType_S3),
            icon = Icons.Outlined.Cloud,
            onClick = {
                if (selectedRoute != MainScreenConnector.S3) {
                    navController.navigate(MainScreenConnector.S3)
                }
            }
        )
        SourceChip(
            modifier = Modifier.weight(1f),
            selected = selectedRoute == MainScreenConnector.HTTP,
            label = stringResource(Res.string.MainScreen_SourceType_HTTP),
            icon = Icons.Outlined.Link,
            onClick = {
                if (selectedRoute != MainScreenConnector.HTTP) {
                    navController.navigate(MainScreenConnector.HTTP)
                }
            }
        )
    }
}

@Composable
private fun SourceChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val cs = MaterialTheme.colorScheme

    val scale by animateFloatAsState(
        targetValue = when {
            selected -> 1f
            hovered -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )

    val bg by animateColorAsState(
        targetValue = when {
            selected -> cs.secondaryContainer.copy(alpha = 0.65f)
            hovered -> cs.surfaceVariant.copy(alpha = 0.45f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> cs.secondary
            hovered -> cs.secondary.copy(alpha = 0.45f)
            else -> cs.outline.copy(alpha = 0.35f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBorder"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> cs.onSecondaryContainer
            else -> cs.onSurface.copy(alpha = if (hovered) 0.92f else 0.72f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipContent"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            selected -> cs.secondary
            else -> cs.onSurfaceVariant.copy(alpha = if (hovered) 0.95f else 0.75f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipIcon"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        color = bg,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
