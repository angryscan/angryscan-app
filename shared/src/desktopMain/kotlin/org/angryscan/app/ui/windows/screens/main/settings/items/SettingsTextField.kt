package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.angryscan.app.di.PreviewModuleAll

@Composable
fun SettingsTextField(
    placeholder: String,
    value: Any,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    isError: Boolean = false
) {
    OutlinedTextField(
        label = { Text(placeholder) },
        value = value.toString(),
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        isError = isError
    )
}

@Composable
@Preview
fun SettingsTextFieldPreviewDark() {
    Column {
        PreviewModuleAll {
            Surface {
                SettingsTextField(
                    placeholder = "Host",
                    value = "192.168.1.1",
                    onValueChange = {},
                    isPassword = false,
                    isError = false
                )
            }
        }
    }
}