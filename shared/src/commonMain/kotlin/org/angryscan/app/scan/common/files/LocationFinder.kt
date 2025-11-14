package org.angryscan.app.scan.common.files

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.app.scan.common.files.types.DOCXType
import org.angryscan.app.scan.common.files.types.TextType
import org.angryscan.app.scan.common.files.types.XLSXType
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

object LocationFinder {
    fun isSupported(type: FileType): Boolean = when (type) {
        FileType.XLSX,
        FileType.XLS,
        FileType.Text,
        FileType.DOCX,
        FileType.DOC -> true

        else -> false
    }

    fun isMaskSupported(type: FileType): Boolean = when (type) {
        FileType.Text,
        FileType.XLSX -> true

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

    suspend fun maskLocations(filePath: String, locations: List<Location>): Int {
        val file = File(filePath)
        val type = FileType.getFileType(file = file) ?: throw NotSupportedTypeException

        val tmpFile = File.createTempFile("ADS_mask", ".${file.extension}")

        val maskedCount = when (type) {
            FileType.Text -> TextType.maskLocations(
                filePath,
                tmpFile.absolutePath,
                locations
            )
            FileType.XLSX -> XLSXType.maskLocations(
                filePath,
                tmpFile.absolutePath,
                locations
            )
            else -> throw NotSupportedTypeException
        }
        if (maskedCount == locations.size) {
            try {
                Files.move(
                    Path(tmpFile.absolutePath),
                    Path(file.absolutePath),
                    StandardCopyOption.REPLACE_EXISTING
                )
                return maskedCount
            } catch (_: IOException) {
                tmpFile.delete()
                return 0
            }
        } else {
            return 0
        }
    }

    val NotSupportedTypeException = Exception("Not supported file type")
    val ScanException = Exception("Scan error")
}