package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsBoxSpan(
    text: String,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    textTail: @Composable () -> Unit = {},
    block: @Composable () -> Unit
) {
    //var expanded by remember { mutableStateOf(false) }
    val iconRotation = remember { Animatable(90f) }

    LaunchedEffect(expanded) {
        iconRotation.animateTo(if (expanded) 90f else 270f)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .height(34.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onExpandClick() },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = 16.sp
                    )
                    textTail()
                }

                Icon(
                    imageVector = Icons.Outlined.ArrowBackIosNew,
                    contentDescription = null,
                    modifier = Modifier
                        .rotate(iconRotation.value)
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(700.dp)
            ) {
                block()
            }
        }
    }
}