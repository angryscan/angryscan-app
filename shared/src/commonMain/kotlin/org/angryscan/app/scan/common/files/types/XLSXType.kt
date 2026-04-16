package org.angryscan.app.scan.common.files.types

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.IExportLocations
import org.angryscan.app.scan.common.files.IFileLocation
import org.angryscan.app.scan.common.files.IMaskFile
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import org.angryscan.app.scan.common.files.extensions.isMaskable
import org.angryscan.app.scan.common.files.extensions.mask
import org.angryscan.app.scan.common.files.locations.XLSLocation
import org.angryscan.app.ui.strings.readableName
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger { }

@Serializable
object XLSXType : FileType(), IMaskFile, IFileLocation, IExportLocations {
    override val name = "XLSX"
    override val extensions = listOf("xlsx")
    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean,
        selectedExtensions: List<IFileType>
    ): Document {
        val str = StringBuilder()
        val res = Document(file.length(), file.absolutePath)
        var sample = 0
        try {
            withContext(Dispatchers.IO) {
                FileInputStream(file).use { inputStream ->
                    ReadableWorkbook(inputStream).use { workbook ->
                        workbook.sheets.use { sheets ->
                            sheets.forEach sheet@{ sheet ->
                                sheet?.openStream().use { rowStream ->
                                    rowStream?.forEach rowStream@{ row ->
                                        if (isSampleOverload(sample, fastScan, isActive))
                                            return@rowStream
                                        row?.forEach { cell ->
                                            if (cell != null) {
                                                str.append(cell.text).append("\n")
                                                if (isLengthOverload(str.length, isActive)) {
                                                    engines.forEach { engine ->
                                                        res + scan(str.toString(), engine)
                                                    }
                                                    str.clear()
                                                    sample++
                                                    if (isSampleOverload(sample, fastScan, isActive))
                                                        return@rowStream
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Filed to scan XLSX file ${file.absolutePath}: ${e.message}" }
            res.skip()
            return res
        }
        if (str.isNotEmpty() && !isSampleOverload(sample, fastScan)) {
            engines.forEach { engine ->
                res + withContext(context) { scan(str.toString(), engine) }
            }
        }
        return res
    }

    override suspend fun findLocation(
        filePath: String,
        engine: IScanEngine,
        fastScan: Boolean
    ): List<XLSLocation> {
        var length = 0
        var sample = 0
        val locations = mutableListOf<XLSLocation>()
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                FileInputStream(file).use { inputStream ->
                    ReadableWorkbook(inputStream).use { workbook ->
                        workbook.sheets.use { sheets ->
                            sheets.forEach sheet@{ sheet ->
                                sheet?.openStream().use { rowStream ->
                                    rowStream?.forEach rowStream@{ row ->
                                        if (isSampleOverload(sample, fastScan, isActive)) return@rowStream
                                        var before = ""

                                        row?.forEach { cell ->
                                            if (cell != null) {
                                                var after = ""
                                                engine
                                                    .scan(cell.text)
                                                    .also { r ->
                                                        if (r.isNotEmpty()) {
                                                            var i = 1
                                                            while (cell.address.column + i < row.cellCount) {
                                                                val c = row.getCell(cell.address.column + i)
                                                                if (c != null) {
                                                                    after = c
                                                                        .text
                                                                        .substring(
                                                                            startIndex = 0,
                                                                            endIndex = (c.text.length - 1).coerceAtMost(
                                                                                19
                                                                            )
                                                                        )
                                                                    break
                                                                }
                                                                i++
                                                            }
                                                        }
                                                    }
                                                    .forEach {
                                                        locations.add(
                                                            XLSLocation(
                                                                entry = it.copy(
                                                                    after = after,
                                                                    before = before
                                                                ),
                                                                sheet = sheet.name,
                                                                col = cell.address.column,
                                                                row = cell.address.row,
                                                                cell = cell.address.toString()
                                                            )
                                                        )
                                                    }

                                                length += cell.text.length
                                                if (isLengthOverload(length, isActive)) {
                                                    length = 0
                                                    sample++
                                                    if (isSampleOverload(sample, fastScan, isActive))
                                                        return@rowStream
                                                }
                                                before = cell
                                                    .text
                                                    .substring(
                                                        (cell.text.length - 20).coerceAtLeast(0)
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Failed to find locations in XLSX file ${filePath}: ${e.message}" }
            throw ScanException
        }
        return locations
    }

    override suspend fun maskLocations(
        inputFile: String,
        outputFile: String,
        locations: List<Location>
    ): Int {
        var locationsMasked = 0
        val sortedLocations = locations
            .filter { it.isMaskable() }
            .map { it as XLSLocation }
            .groupBy { it.sheet }
        withContext(Dispatchers.IO) {
            FileInputStream(inputFile).use { inputStream ->
                XSSFWorkbook(inputStream).use { workbook ->
                    sortedLocations.keys.forEach { sheetName ->
                        val sheet = workbook.getSheet(sheetName)

                        sortedLocations[sheetName]!!.forEach { location ->
                            val row = sheet.getRow(location.row)
                            val cell = row.getCell(location.col)
                            val value = when (cell.cellType) {
                                CellType.STRING -> {
                                    cell.stringCellValue
                                }

                                CellType.NUMERIC -> {
                                    cell.numericCellValue.toString()
                                }

                                CellType.BOOLEAN -> {
                                    cell.booleanCellValue.toString()
                                }

                                else -> {
                                    cell.rawValue
                                }
                            }
                            val replaced = value.replace(location.entry.value, location.mask())
                            if (value != replaced) {
                                cell.setCellValue(replaced)
                                locationsMasked++
                            } else {
                                logger.error { "Failed to mask location in file $inputFile in cell ${cell.address}" }
                            }
                        }
                    }
                    FileOutputStream(outputFile).use { outputStream ->
                        workbook.write(outputStream)
                    }
                }
            }
        }
        return locationsMasked
    }

    override suspend fun exportRows(
        inputFile: String,
        locations: List<Location>,
        outputFile: String
    ): Int {
        var rowsExported = 0
        val sortedLocations = locations
            .map { it as XLSLocation }
            .groupBy { it.sheet }
        withContext(Dispatchers.IO) {
            File(outputFile)
                .bufferedWriter()
                .use { writer ->
                    FileInputStream(inputFile).use { inputStream ->
                        XSSFWorkbook(inputStream).use { workbook ->
                            sortedLocations.keys.forEach { sheetName ->
                                val sheet = workbook.getSheet(sheetName)

                                val rows = sortedLocations[sheetName]!!.groupBy { it.row }
                                rows.keys.forEach { rowNum ->
                                    val row = sheet.getRow(rowNum)
                                    writer.write(rows[rowNum]!!.map { it.entry.matcher.readableName() }
                                        .joinToString(", ") + ";")

                                    val writeRow = row.joinToString(";") { cell ->
                                        try {
                                            when (cell.cellType) {
                                                CellType.STRING -> {
                                                    cell.stringCellValue
                                                }

                                                CellType.NUMERIC -> {
                                                    cell.numericCellValue.toString()
                                                }

                                                CellType.BOOLEAN -> {
                                                    cell.booleanCellValue.toString()
                                                }

                                                CellType.FORMULA -> {
                                                    cell.cellFormula
                                                }

                                                else -> {
                                                    cell.stringCellValue
                                                }
                                            }
                                        } catch (e: Exception) {
                                            logger.debug { "Failed to extract cell value. ${e.message}" }
                                            ""
                                        }
                                    }

                                    writer.write(writeRow)
                                    writer.newLine()
                                    rowsExported++
                                }
                            }
                        }
                    }
                }
        }
        return rowsExported
    }
}