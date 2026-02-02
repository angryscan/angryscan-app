package org.angryscan.app.scan.common.files.types

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement
import org.odftoolkit.odfdom.dom.element.table.TableTableElement
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph
import java.io.File
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger { }

@Serializable
@Suppress("unused")
object ODTType : FileType() {
    override val name = "ODT"
    override val extensions = listOf("odt")
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
                OdfTextDocument.loadDocument(file).contentRoot.also { content ->
                    for (contIt in 0 until content.length) {
                        when (val item = content.item(contIt)) {
                            is OdfTextParagraph -> {
                                if (item.textContent.isNotEmpty()) {
                                    str.append(item.textContent).append("\n")
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

                            is TableTableElement -> {
                                for (rowIt in 0 until item.length) {
                                    item.item(rowIt).also { row ->
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
                                                                            !isActive
                                                                        )
                                                                    ) return@withContext
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
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Error while scanning odt ${file.absolutePath}: ${e.message}" }
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