package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.poifs.filesystem.Ole10Native
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.xwpf.usermodel.*
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.IFileLocation
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

@Serializable
object DOCXType : FileType(), IFileLocation {
    override val name = "DOCX"
    override val extensions = listOf("docx")

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
        var embeddedAll = 0
        var embeddedSkipped = 0
        try {
            withContext(Dispatchers.IO) {
                FileInputStream(file).use { fileInputStream ->
                    XWPFDocument(fileInputStream).use { document ->
                        for (elem in document.bodyElements) {
                            val text = when (elem) {
                                is XWPFParagraph -> elem.text
                                is XWPFTable -> elem.text
                                is XWPFComment -> elem.text
                                is XWPFFooter -> elem.text
                                else -> ""
                            }
                            str.append(text).append("\n")
                            if (isLengthOverload(str.length, isActive)) {
                                engines.forEach { engine ->
                                    res + withContext(context) { scan(str.toString(), engine) }
                                }
                                str.clear()
                                sample++
                                if (isSampleOverload(
                                        sample,
                                        fastScan
                                    ) || !isActive
                                ) return@withContext
                            }
                        }

                        // Scan embedded files (e.g., XLSX, DOCX) inside DOCX.
                        // Important: Extract only files with extensions present in selectedExtensions.
                        document.allEmbeddedParts.forEach { part ->
                            val partName = part.partName.name
                            val embeddedName = partName.substringAfterLast("/")
                            val embeddedNameLower = embeddedName.lowercase()

                            // Common case: embedded OLE Package is stored as oleObject*.bin without a real extension.
                            if (embeddedNameLower.endsWith(".bin")) {
                                try {
                                    part.inputStream.use { input ->
                                        POIFSFileSystem(input).use { fs ->
                                            val ole = Ole10Native.createFromEmbeddedOleObject(fs.root)
                                            val realName = ole.fileName
                                                .substringAfterLast("\\")
                                                .substringAfterLast("/")
                                            val realNameLower = realName.lowercase()

                                            // Filter before writing to disk (same idea as ZIPType).
                                            if (!selectedExtension(realNameLower, selectedExtensions)) return@forEach

                                            val ext = realNameLower.substringAfterLast(".", "")
                                            val tmpFile = File.createTempFile(
                                                "ADS_",
                                                if (ext.isNotEmpty()) ".$ext" else ".tmp"
                                            )
                                            try {
                                                tmpFile.writeBytes(ole.dataBuffer)
                                                IFileType.getFileType(tmpFile).forEach { ft ->
                                                    ft.scanFile(
                                                        tmpFile,
                                                        context,
                                                        engines,
                                                        fastScan,
                                                        selectedExtensions
                                                    ).also { doc ->
                                                        if (!doc.skipped()) {
                                                            res + doc.getDocumentFields()
                                                        } else {
                                                            embeddedSkipped++
                                                        }
                                                    }
                                                }
                                                embeddedAll++
                                            } finally {
                                                tmpFile.delete()
                                            }
                                        }
                                    }
                                } catch (_: Exception) {
                                    // Not an OLE Package or failed to extract - skip.
                                }
                            } else {
                                // If the part name contains an extension, filter by it before extraction.
                                if (!selectedExtension(embeddedNameLower, selectedExtensions)) return@forEach

                                val ext = embeddedNameLower.substringAfterLast(".", "")
                                val tmpFile = File.createTempFile(
                                    "ADS_",
                                    if (ext.isNotEmpty()) ".$ext" else ".tmp"
                                )
                                try {
                                    part.inputStream.use { input ->
                                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    IFileType.getFileType(tmpFile).forEach { ft ->
                                        ft.scanFile(
                                            tmpFile,
                                            context,
                                            engines,
                                            fastScan,
                                            selectedExtensions
                                        ).also { doc ->
                                            if (!doc.skipped()) {
                                                res + doc.getDocumentFields()
                                            } else {
                                                embeddedSkipped++
                                            }
                                        }
                                    }
                                    embeddedAll++
                                } catch (_: Exception) {
                                    // Skip broken embedded file.
                                } finally {
                                    tmpFile.delete()
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            try {
                withContext(Dispatchers.IO) {
                    FileInputStream(file).use { fileInputStream ->
                        HWPFDocument(fileInputStream).use { document ->
                            WordExtractor(document).use { extractor ->
                                extractor.text.forEach { c ->
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
                    }
                }
            } catch (_: Exception) {
                res.skip()
                return res
            }
        }
        if (str.isNotEmpty() && !isSampleOverload(sample, fastScan)) {
            engines.forEach { engine ->
                res + withContext(context) { scan(str.toString(), engine) }
            }
        }
        // If we scanned only embedded files and all of them were skipped, mark document skipped.
        if (embeddedAll > 0 && embeddedAll == embeddedSkipped && res.isEmpty()) {
            res.skip()
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
                var elemPosition = 1
                FileInputStream(file).use { fileInputStream ->
                    XWPFDocument(fileInputStream).use { document ->
                        for (elem in document.bodyElements) {
                            val text = when (elem) {
                                is XWPFParagraph -> elem.text
                                is XWPFTable -> elem.text
                                is XWPFComment -> elem.text
                                is XWPFFooter -> elem.text
                                else -> ""
                            }
                            val elemType = when (elem) {
                                is XWPFParagraph -> "Paragraph"
                                is XWPFTable -> "Table"
                                is XWPFComment -> "Comment"
                                is XWPFFooter -> "Footer"
                                else -> ""
                            }
                            engine
                                .scan(text)
                                .filter { it.matcher::class == matcher::class }
                                .forEach {
                                locations.add(Location(it, "$elemType, Position:$elemPosition"))
                            }

                            length += text.length

                            elemPosition++

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
        } catch (_: Exception) {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    FileInputStream(file).use { fileInputStream ->
                        HWPFDocument(fileInputStream).use { document ->
                            WordExtractor(document).use { extractor ->
                                extractor.paragraphText.forEachIndexed { index, text ->
                                    engine
                                        .scan(text)
                                        .filter { it.matcher::class == matcher::class }
                                        .forEach {
                                        locations.add(Location(it, "Paragraph:$index"))
                                    }
                                    length += text.length
                                    if (isLengthOverload(length, isActive)) {
                                        length = 0
                                        sample++
                                        if (isSampleOverload(sample, fastScan, isActive))
                                            return@withContext
                                    }
                                }
                                extractor.commentsText.forEachIndexed { index, text ->
                                    engine
                                        .scan(text)
                                        .filter { it.matcher::class == matcher::class }
                                        .forEach {
                                        locations.add(Location(it, "Comment:$index"))
                                    }
                                    length += text.length
                                    if (isLengthOverload(length, isActive)) {
                                        length = 0
                                        sample++
                                        if (isSampleOverload(sample, fastScan, isActive))
                                            return@withContext
                                    }
                                }
                                extractor.footnoteText.forEachIndexed { index, text ->
                                    engine
                                        .scan(text)
                                        .filter { it.matcher::class == matcher::class }
                                        .forEach {
                                        locations.add(Location(it, "Footnote:$index"))
                                    }
                                    length += text.length
                                    if (isLengthOverload(length, isActive)) {
                                        length = 0
                                        sample++
                                        if (isSampleOverload(sample, fastScan, isActive))
                                            return@withContext
                                    }
                                }
                                extractor.endnoteText.forEachIndexed { index, text ->
                                    engine
                                        .scan(text)
                                        .filter { it.matcher::class == matcher::class }
                                        .forEach {
                                        locations.add(Location(it, "Endnote:$index"))
                                    }
                                    length += text.length
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
            } catch (_: Exception) {
                throw ScanException
            }
        }
        return locations
    }

}