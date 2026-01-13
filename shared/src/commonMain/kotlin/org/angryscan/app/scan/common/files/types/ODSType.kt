package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement
import java.io.File
import kotlin.coroutines.CoroutineContext

@Serializable
@Suppress("unused")
object ODSType: FileType() {
    override val name = "ODS"
    override val extensions = listOf("ods")
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
                OdfSpreadsheetDocument.loadDocument(file).use { document ->
                    document.spreadsheetTables.forEach { table ->
                        table.rowElementList.forEach { row ->
                            if (row is TableTableRowElement) {
                                for (celIt in 0 until row.length) {
                                    val celElement = row.item(celIt)
                                    if (celElement is TableTableCellElement) {
                                        for (celContIt in 0 until celElement.length) {
                                            celElement.item(celContIt).textContent.also { text ->
                                                if (text.isNotEmpty()) {
                                                    str.append(text).append("\n")
                                                    if (isLengthOverload(str.length, isActive)) {
                                                        engines.forEach { engine ->
                                                            res + withContext(context) {
                                                                scan(
                                                                    str.toString(),
                                                                    engine
                                                                )
                                                            }
                                                        }
                                                        str.clear()
                                                        sample++
                                                        if (isSampleOverload(
                                                                sample,
                                                                fastScan,
                                                                isActive
                                                        )) return@withContext
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
}