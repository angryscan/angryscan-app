package org.angryscan.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LicenseDialog(
    onCloseRequest: () -> Unit,
    dialogState: DialogState = rememberDialogState(width = 600.dp, height = 600.dp)
) {
    SimpleDialogWindow(
        onCloseRequest = onCloseRequest,
        dialogState = dialogState,
        title = stringResource(Res.string.LicenseDialog_Title)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.license_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = stringResource(Res.string.license_copyright),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(Res.string.license_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
