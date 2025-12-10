package org.angryscan.app.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import org.angryscan.app.common.AppVersion
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DescriptionDialog(
    onCloseRequest: () -> Unit,
    dialogState: DialogState = rememberDialogState(width = 600.dp, height = 450.dp)
) {
    SimpleDialogWindow(
        onCloseRequest = onCloseRequest,
        dialogState = dialogState,
        title = stringResource(Res.string.DescriptionDialog_Title)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Column {
                    Text(
                        text = stringResource(Res.string.appName),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(Res.string.AboutScreen_Version, AppVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(Res.string.AboutScreen_Description),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
