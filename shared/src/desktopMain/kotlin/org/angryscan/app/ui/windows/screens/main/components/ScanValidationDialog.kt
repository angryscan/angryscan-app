package org.angryscan.app.ui.windows.screens.main.components

import androidx.compose.runtime.*
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.ui.dialogs.DesktopAlertDialog
import org.angryscan.app.ui.validateScanSettings
import org.jetbrains.compose.resources.stringResource

/**
 * Composable hook for scan validation with error dialog
 * @return Triple of (validationErrorDialog state, validateAndShowError function, dismissError function)
 */
@Composable
fun rememberScanValidation(
    scanSettings: ScanSettings
): Triple<Pair<String, String>?, () -> Boolean, () -> Unit> {
    var validationErrorDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    // Validation error messages
    val noExtensionsTitle = stringResource(Res.string.Validation_NoExtensionsTitle)
    val noExtensionsMessage = stringResource(Res.string.Validation_NoExtensionsMessage)
    val noMatchersTitle = stringResource(Res.string.Validation_NoMatchersTitle)
    val noMatchersMessage = stringResource(Res.string.Validation_NoMatchersMessage)
    
    val validateAndShowError: () -> Boolean = {
        val validationResult = validateScanSettings(
            scanSettings,
            noExtensionsTitle,
            noExtensionsMessage,
            noMatchersTitle,
            noMatchersMessage
        )
        
        if (!validationResult.isValid) {
            validationErrorDialog = Pair(
                validationResult.errorTitle!!,
                validationResult.errorMessage!!
            )
            false
        } else {
            true
        }
    }
    
    val dismissError: () -> Unit = {
        validationErrorDialog = null
    }
    
    return Triple(validationErrorDialog, validateAndShowError, dismissError)
}

/**
 * Composable function to show validation error dialog
 */
@Composable
fun ScanValidationErrorDialog(
    validationError: Pair<String, String>?,
    onDismiss: () -> Unit
) {
    validationError?.let { (title, message) ->
        DesktopAlertDialog(
            onCloseRequest = onDismiss,
            title = title,
            message = message
        )
    }
}

