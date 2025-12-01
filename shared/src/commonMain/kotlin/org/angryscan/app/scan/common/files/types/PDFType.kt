package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.text.forEach

@Serializable
@Suppress("unused")
object PDFType: FileType() {
    override val name = "PDF"
    override val extensions = listOf("pdf")
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
            withContext(Dispatchers.IO) {
                PDDocument.load(file).use { document ->
                    PDFTextStripper().getText(document).forEach { c ->
                        str.append(c)
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