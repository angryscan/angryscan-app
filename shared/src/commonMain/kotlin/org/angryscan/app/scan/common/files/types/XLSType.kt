package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

object XLSType : IFileType {
    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document {
        val str = StringBuilder()
        val res = Document(file.length(), file.absolutePath)
        var sample = 0
        try {
            //Create Workbook instance holding reference to .xlsx file
            withContext(Dispatchers.IO) {
                FileInputStream(file).use { fileInputStream ->
                    HSSFWorkbook(fileInputStream).use { workbook ->
                        val dataFormatter = DataFormatter()
                        dataFormatter.isEmulateCSV = true
                        workbook.forEach workbook@{ sheet ->
                            sheet?.forEach { row ->
                                row?.forEach { cell ->
                                    if (cell != null) {
                                        when (cell.cellType) {
                                            CellType.NUMERIC -> str.append(dataFormatter.formatCellValue(cell))
                                                .append("\n")

                                            CellType.STRING -> str.append(dataFormatter.formatCellValue(cell))
                                                .append("\n")

                                            else -> {}
                                        }
                                        if (isLengthOverload(str.length, isActive)) {
                                            engines.forEach { engine ->
                                                res + withContext(context) { scan(str.toString(), engine) }
                                            }
                                            str.clear()
                                            sample++
                                            if (isSampleOverload(sample, fastScan, isActive))
                                                return@withContext
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
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
        matcher: IMatcher,
        fastScan: Boolean
    ): List<Location> {
        var length = 0
        var sample = 0
        val locations = mutableListOf<Location>()
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                FileInputStream(file).use { fileInputStream ->
                    HSSFWorkbook(fileInputStream).use { workbook ->
                        val dataFormatter = DataFormatter()
                        dataFormatter.isEmulateCSV = true
                        workbook.forEach workbook@{ sheet ->
                            sheet?.forEach { row ->
                                row?.forEach { cell ->
                                    if (cell != null) {
                                        val text = when (cell.cellType) {
                                            CellType.NUMERIC -> dataFormatter.formatCellValue(cell)

                                            CellType.STRING -> dataFormatter.formatCellValue(cell)

                                            else -> ""
                                        }

                                        engine
                                            .scan(text)
                                            .filter { it.matcher::class == matcher::class }
                                            .forEach {
                                                locations.add(Location(it, "${sheet.sheetName}:${cell.address}"))
                                            }

                                        if (isLengthOverload(length, isActive)) {
                                            length = 0
                                            sample++
                                            if (isSampleOverload(sample, fastScan, isActive))
                                                return@withContext
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            throw ScanException
        }
        return locations
    }
}