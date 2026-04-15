package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_DiscardAll
import org.angryscan.app.resources.ScanSettings_SelectAll
import org.jetbrains.compose.resources.stringResource

/**
 * «Выбрать все» / «Сбросить» в одной строке с заголовком секции (тот же кегль, без [Box] и вертикальных отступов,
 * иначе при [Row] + [Alignment.CenterVertically] подпись визуально «плывёт» относительно заголовка).
 */
@Composable
fun SettingsSelectAllTextButton(
    allSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(8.dp)
    val contentColor = if (allSelected) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val label = if (allSelected) {
        stringResource(Res.string.ScanSettings_DiscardAll)
    } else {
        stringResource(Res.string.ScanSettings_SelectAll)
    }
    Text(
        text = label,
        modifier = modifier
            .padding(horizontal = 10.dp)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
        style = MaterialTheme.typography.labelLarge,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        color = contentColor,
    )
}
