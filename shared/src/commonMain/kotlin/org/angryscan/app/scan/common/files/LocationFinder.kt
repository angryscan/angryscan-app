package org.angryscan.app.scan.common.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

object LocationFinder {
    fun isSupported(type: IFileType): Boolean = type is IFileLocation

    fun isMaskSupported(type: IFileType): Boolean = type is IMaskFile

    fun isExportSupported(type: IFileType): Boolean = type is IExportLocations

    suspend fun findLocations(filePath: String, engine: IScanEngine, matcher: IMatcher): List<Location> {
        val file = File(filePath)
        val type = IFileType.getFileType(file = file).find{ it is IFileLocation }

        if (type is IFileLocation) {
            return type.findLocation(filePath, engine, matcher)
        } else {
            throw NotSupportedTypeException
        }
    }

    suspend fun maskLocations(filePath: String, locations: List<Location>): Int {
        val file = File(filePath)
        val type = IFileType.getFileType(file = file).find { it is IMaskFile } ?: throw NotSupportedTypeException

        val tmpFile = withContext(Dispatchers.IO) {
            File.createTempFile("ADS_mask", ".${file.extension}")
        }

        val maskedCount = if (type is IMaskFile) {
            type.maskLocations(filePath, tmpFile.absolutePath, locations)
        } else {
            throw NotSupportedTypeException
        }

        if (maskedCount == locations.size) {
            try {
                withContext(Dispatchers.IO) {
                    Files.move(
                        Path(tmpFile.absolutePath),
                        Path(file.absolutePath),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
                return maskedCount
            } catch (_: IOException) {
                tmpFile.delete()
                return 0
            }
        } else {
            tmpFile.delete()
            return 0
        }
    }

    suspend fun exportRows(inputFile: String, locations: List<Location>, outputFile: String): Int {
        val type = IFileType.getFileType(inputFile).find { it is IExportLocations } ?: throw NotSupportedTypeException

        if (type is IExportLocations) {
            return type.exportRows(inputFile, locations, outputFile)
        } else {
            throw NotSupportedTypeException
        }
    }

    val NotSupportedTypeException = Exception("Not supported file type")
    val ScanException = Exception("Scan error")
}