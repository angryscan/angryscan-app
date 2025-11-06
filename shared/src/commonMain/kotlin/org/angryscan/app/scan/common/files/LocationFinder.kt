package org.angryscan.app.scan.common.files

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.app.scan.common.files.types.DOCXType
import org.angryscan.app.scan.common.files.types.TextType
import org.angryscan.app.scan.common.files.types.XLSXType
import java.io.File

object LocationFinder {
    fun isSupported(type: FileType): Boolean = when (type) {
        FileType.XLSX -> true
        FileType.XLS -> true
        FileType.Text -> true
        FileType.DOCX -> true
        FileType.DOC -> true
        else -> false
    }

    suspend fun findLocations(filePath: String, engine: IScanEngine, matcher: IMatcher): List<Location> {
        val file = File(filePath)
        val type = FileType.getFileType(file = file)
        if (type == null || !isSupported(type))
            throw NotSupportedTypeException

        return when (type) {
            FileType.XLSX -> XLSXType.findLocation(filePath, engine, matcher)
            FileType.XLS -> XLSXType.findLocation(filePath, engine, matcher)
            FileType.Text -> TextType.findLocation(filePath, engine, matcher)
            FileType.DOCX -> DOCXType.findLocation(filePath, engine, matcher)
            FileType.DOC -> DOCXType.findLocation(filePath, engine, matcher)
            else -> throw NotSupportedTypeException
        }
    }

    val NotSupportedTypeException = Exception("Not supported file type")
    val ScanException = Exception("Scan error")
}