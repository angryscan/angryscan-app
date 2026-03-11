package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsTextField(
    placeholder: String,
    value: Any,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        isError = isError
    )
}