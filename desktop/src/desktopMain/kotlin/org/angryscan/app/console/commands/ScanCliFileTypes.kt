package org.angryscan.app.console.commands

import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType

/**
 * File types selectable via console `scan -e` / `settings -e`.
 * Code/Cert types are added automatically when their matchers are enabled.
 */
object ScanCliFileTypes {
    fun selectableFileTypes(): List<IFileType> =
        IFileType
            .getAll()
            .filterNot { it in (CertFileType.entries + CodeFileType.entries) }

    fun resolveExtension(inputValue: String): IFileType {
        val normalized = inputValue.replace(" ", "_")
        return selectableFileTypes()
            .find { it.name.replace(" ", "_").equals(normalized, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown extension: $inputValue")
    }
}
