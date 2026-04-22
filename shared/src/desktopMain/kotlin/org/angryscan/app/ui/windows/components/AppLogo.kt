package org.angryscan.app.ui.windows.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.angryscan.app.navigation.AppScreen
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.appName
import org.jetbrains.compose.resources.*

val logger = KotlinLogging.logger { }

private const val RES_BASE = "composeResources/org.angryscan.app.resources/"

@OptIn(InternalResourceApi::class)
private fun logoDrawableResource(isDarkTheme: Boolean): DrawableResource = if (isDarkTheme) {
    DrawableResource(
        "drawable:favicon_dark_128",
        setOf(ResourceItem(setOf(), "${RES_BASE}drawable/favicon_dark_128.png", -1, -1))
    )
} else {
    DrawableResource(
        "drawable:favicon_light_128",
        setOf(ResourceItem(setOf(), "${RES_BASE}drawable/favicon_light_128.png", -1, -1))
    )
}

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
            isPressed -> 0.97f
            isHovered -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = (colorScheme.surface.red + colorScheme.surface.green + colorScheme.surface.blue) / 3f < 0.5f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 22.dp),
                    onClick = {
                        try {
                            if (!isMainScreen) {
                                navController.navigate(AppScreen.Main)
                            }
                        } catch (e: Exception) {
                            logger.error { "Navigation error: ${e.message}" }
                        }
                    }
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(logoDrawableResource(isDarkTheme)),
                contentDescription = stringResource(Res.string.appName),
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = stringResource(Res.string.appName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )
    }
}

