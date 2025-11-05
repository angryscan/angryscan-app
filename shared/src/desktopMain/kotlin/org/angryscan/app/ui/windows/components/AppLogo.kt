package org.angryscan.app.ui.windows.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.navigation.AppScreen
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.appName
import org.angryscan.app.resources.icon

@Composable
fun AppLogo(
    navController: NavController
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isMainScreen = currentDestination?.hasRoute(AppScreen.Main::class) ?: false
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isHovered -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.8f
            isHovered -> 0.95f
            else -> 1f
        },
        animationSpec = tween(150, easing = EaseInOutCubic),
        label = "alpha"
    )
    
    val elevation by animateFloatAsState(
        targetValue = when {
            isPressed -> 4f
            isHovered -> 12f
            else -> 8f
        },
        animationSpec = tween(200, easing = EaseInOutCubic),
        label = "elevation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        radius = 50f
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .shadow(
                    elevation = elevation.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .scale(scale)
                .alpha(alpha)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        radius = 200.dp
                    ),
                    onClick = {
                        try {
                            if (!isMainScreen) {
                                println("Logo clicked - navigating to Main")
                                navController.navigate(AppScreen.Main)
                                println("Navigation to Main successful")
                            } else {
                                println("Already on Main screen - no navigation needed")
                            }
                        } catch (e: Exception) {
                            println("Navigation error: ${e.message}")
                        }
                    }
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.icon),
                contentDescription = stringResource(Res.string.appName),
                modifier = Modifier
                    .size(28.dp)
                    .scale(1.15f)
            )
        }

        Text(
            text = stringResource(Res.string.appName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

