package org.angryscan.app.scan.common.files.extensions

import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.files.types.ODSType
import org.angryscan.app.scan.common.files.types.TextType
import org.angryscan.app.scan.common.files.types.XLSType
import org.angryscan.app.scan.common.files.types.XLSXType

/**
* Check if file type requires keywords
*/
fun IFileType.requireKeywords(extension: String? = null): Boolean {
    return when(this) {
        is XLSXType, is XLSType, is ODSType -> false
        is TextType -> !extension.equals("csv", ignoreCase = true)
        else -> true
    }
}

fun List<IFileType>.requireKeywords(extension: String? = null): Boolean {
    return any { it.requireKeywords(extension) }
}