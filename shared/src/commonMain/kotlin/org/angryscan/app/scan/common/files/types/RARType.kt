package org.angryscan.app.scan.common.files.types

import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext

@Serializable
object RARType: FileType() {
    override val name = "RAR"
    override val extensions = listOf("rar")

    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document {
        val res = Document(file.length(), file.absolutePath)
        var skipped = 0
        var all = 0
        try {
            withContext(Dispatchers.IO) {
                val archive = Archive(file)
                while (true) {
                    val fileHeader = archive.nextFileHeader() ?: break

                    if (!selectedExtension(fileHeader.fileName))
                        continue

                    val tmpFile = File.createTempFile(
                        "ADS_",
                        "." + fileHeader.fileName.substringAfterLast(".")
                    )

                    try {
                        archive.extractFile(fileHeader, tmpFile.outputStream())
                        IFileType.getFileType(tmpFile).forEach { ft ->
                            ft.scanFile(tmpFile, context, engines, fastScan).also { doc ->
                            if (!doc.skipped()) {
                                res + doc.getDocumentFields()
                            } else {
                                skipped++
                            }
                        }
                        }
                        all++
                    } catch (_: IOException) {
                        continue
                    } finally {
                        tmpFile.delete()
                    }
                }
            }
        } catch (_: Exception) {
            if (res.isEmpty()) {
                res.skip()
                return res
            }
        }
        if (skipped == all)
            res.skip()
        return res
    }
}