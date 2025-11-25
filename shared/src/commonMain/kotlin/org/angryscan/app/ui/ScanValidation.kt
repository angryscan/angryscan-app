package org.angryscan.app.ui

import org.angryscan.app.common.ScanSettings

/**
 * Validation result for scan settings
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorTitle: String? = null,
    val errorMessage: String? = null
)

/**
 * Validates scan settings (extensions and matchers)
 * @param scanSettings The scan settings to validate
 * @param noExtensionsTitle Title for error when no extensions selected
 * @param noExtensionsMessage Message for error when no extensions selected
 * @param noMatchersTitle Title for error when no matchers selected
 * @param noMatchersMessage Message for error when no matchers selected
 * @return ValidationResult with validation status and error messages if invalid
 */
fun validateScanSettings(
    scanSettings: ScanSettings,
    noExtensionsTitle: String,
    noExtensionsMessage: String,
    noMatchersTitle: String,
    noMatchersMessage: String
): ValidationResult {
    // Validate extensions
    if (scanSettings.extensions.isEmpty()) {
        return ValidationResult(
            isValid = false,
            errorTitle = noExtensionsTitle,
            errorMessage = noExtensionsMessage
        )
    }
    
    // Validate matchers
    if (scanSettings.matchers.isEmpty() && scanSettings.userSignatures.isEmpty()) {
        return ValidationResult(
            isValid = false,
            errorTitle = noMatchersTitle,
            errorMessage = noMatchersMessage
        )
    }
    
    return ValidationResult(isValid = true)
}

