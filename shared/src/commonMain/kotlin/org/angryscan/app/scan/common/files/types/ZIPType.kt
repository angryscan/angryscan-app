package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.CoroutineContext

@Serializable
object ZIPType : FileType() {
    override val name = "ZIP"
    override val extensions = listOf("zip")
    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document {
        val res = Document(file.length(), file.absolutePath)
        var skipped = 0
        var all = 0
        var reading = true
        try {
            withContext(Dispatchers.IO) {
                FileInputStream(file).use { fileInputStream ->
                    BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                        ZipInputStream(
                            bufferedInputStream,
                            Charset.forName("windows-1251")
                        ).use { zipInputStream ->
                            var zipEntry: ZipEntry?
                            val buffer = ByteArray(2048)
                            while (reading) {
                                zipEntry = try {
                                    zipInputStream.nextEntry.also {
                                        if (it == null)
                                            reading = false
                                    }
                                } catch (_: NullPointerException) {
                                    null
                                } catch (_: IllegalArgumentException) {
                                    null
                                }
                                if (zipEntry == null) continue

                                // не распаковывать если расширение не из выбранных
                                if (!selectedExtension(zipEntry.name))
                                    continue

                                val tmpFile = File.createTempFile(
                                    "ADS_",
                                    "." + zipEntry.name.substringAfterLast(".")
                                )
                                try {
                                    tmpFile.outputStream().use { fileOutputStream ->
                                        fileOutputStream.buffered(buffer.size).use { bufferedOutputStream ->
                                            while (true) {
                                                val length = zipInputStream.read(buffer)
                                                if (length <= 0) break
                                                bufferedOutputStream.write(buffer, 0, length)
                                            }
                                            bufferedOutputStream.flush()
                                        }
                                    }
                                    IFileType.getFileType(tmpFile)?.scanFile(tmpFile, context, engines, fastScan)
                                        ?.also { doc ->
                                            if (!doc.skipped()) {
                                                res + doc.getDocumentFields()
                                            } else {
                                                skipped++
                                            }
                                        }
                                    all++
                                } catch (_: IOException) {
                                    continue
                                } finally {
                                    tmpFile.delete()
                                }
                                zipInputStream.closeEntry()
                            }
                        }
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