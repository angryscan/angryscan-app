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
import org.angryscan.app.scan.common.files.IMaskFile
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import org.angryscan.app.scan.common.files.extensions.isMaskable
import org.angryscan.app.scan.common.files.extensions.mask
import org.angryscan.app.scan.common.files.locations.BaseLocation
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

@Serializable
object DOCXType : FileType(), IMaskFile, IFileLocation {
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

    override suspend fun maskLocations(
        inputFile: String,
        outputFile: String,
        locations: List<Location>
    ): Int {
        val maskableLocations = locations
            .filter { it.isMaskable() }
            .mapNotNull { parseMaskLocation(it) }
            .sortedBy { it.position }

        var locationsMasked = 0
        withContext(Dispatchers.IO) {
            FileInputStream(inputFile).use { inputStream ->
                XWPFDocument(inputStream).use { document ->
                    val locationsByPosition = maskableLocations.groupBy { it.position }
                    var elemPosition = 1
                    for (elem in document.bodyElements) {
                        val candidates = locationsByPosition[elemPosition].orEmpty()
                        if (candidates.isNotEmpty()) {
                            val elemType = when (elem) {
                                is XWPFParagraph -> "Paragraph"
                                is XWPFTable -> "Table"
                                is XWPFComment -> "Comment"
                                is XWPFFooter -> "Footer"
                                else -> ""
                            }
                            candidates
                                .filter { it.type == elemType }
                                .forEach { maskLocation ->
                                    val replaced = when (elem) {
                                        is XWPFParagraph -> replaceFirstInParagraph(
                                            elem,
                                            maskLocation.location.entry.value,
                                            maskLocation.location.mask()
                                        )

                                        is XWPFTable -> replaceFirstInTable(
                                            elem,
                                            maskLocation.location.entry.value,
                                            maskLocation.location.mask()
                                        )

                                        is XWPFComment -> replaceFirstInParagraphs(
                                            elem.paragraphs,
                                            maskLocation.location.entry.value,
                                            maskLocation.location.mask()
                                        )

                                        is XWPFFooter -> replaceFirstInFooter(
                                            elem,
                                            maskLocation.location.entry.value,
                                            maskLocation.location.mask()
                                        )

                                        else -> false
                                    }
                                    if (replaced) {
                                        locationsMasked++
                                    }
                                }
                        }
                        elemPosition++
                    }
                    FileOutputStream(outputFile).use { outputStream ->
                        document.write(outputStream)
                    }
                }
            }
        }
        return locationsMasked
    }

    override suspend fun findLocation(
        filePath: String,
        engine: IScanEngine,
        matchers: List<IMatcher>,
        fastScan: Boolean
    ): List<BaseLocation> {
        var length = 0
        var sample = 0
        val locations = mutableListOf<BaseLocation>()
        val matchersClasses = matchers.map { it::class }
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                var elemPosition = 1
                FileInputStream(file).use { fileInputStream ->
                    XWPFDocument(fileInputStream).use { document ->
                        fun attachLocation(source: BaseLocation, embeddedName: String): BaseLocation {
                            val attachmentLabel = source.attachmentName ?: embeddedName
                            val baseLabel = "Attachment: $attachmentLabel"
                            val locationLabel = if (source.location.isNotBlank()) {
                                "$baseLabel, ${source.location}"
                            } else {
                                baseLabel
                            }
                            return source.copy(
                                location = locationLabel,
                                attachmentName = attachmentLabel
                            )
                        }

                        suspend fun scanEmbeddedFile(embeddedFile: File, embeddedName: String) {
                            val embeddedTypes = IFileType.getFileType(embeddedFile)
                                .filterIsInstance<IFileLocation>()
                                .ifEmpty { IFileType.getAll().filterIsInstance<IFileLocation>() }
                            for (embeddedType in embeddedTypes) {
                                val embeddedLocations = runCatching {
                                    embeddedType.findLocation(
                                        embeddedFile.absolutePath,
                                        engine,
                                        matchers,
                                        fastScan
                                    )
                                }.getOrDefault(emptyList())
                                if (embeddedLocations.isNotEmpty()) {
                                    embeddedLocations.forEach { location ->
                                        locations.add(
                                            attachLocation(
                                                BaseLocation(
                                                    entry = location.entry,
                                                    location = location.location,
                                                    attachmentName = location.attachmentName
                                                ),
                                                embeddedName
                                            )
                                        )
                                    }
                                    break
                                }
                            }
                        }

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
                                .filter { it.matcher::class in matchersClasses }
                                .forEach {
                                    locations.add(BaseLocation(it, "$elemType, Position:$elemPosition"))
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

                        for (part in document.allEmbeddedParts) {
                            val partName = part.partName.name
                            val embeddedName = partName.substringAfterLast("/")
                            val embeddedNameLower = embeddedName.lowercase()
                            if (embeddedNameLower.endsWith(".bin")) {
                                try {
                                    part.inputStream.use { input ->
                                        POIFSFileSystem(input).use { fs ->
                                            val ole = Ole10Native.createFromEmbeddedOleObject(fs.root)
                                            val realName = ole.fileName
                                                .substringAfterLast("\\")
                                                .substringAfterLast("/")
                                            val ext = realName.substringAfterLast(".", "")
                                            val tmpFile = File.createTempFile(
                                                "ADS_",
                                                if (ext.isNotEmpty()) ".$ext" else ".tmp"
                                            )
                                            try {
                                                tmpFile.writeBytes(ole.dataBuffer)
                                                scanEmbeddedFile(tmpFile, realName)
                                            } finally {
                                                tmpFile.delete()
                                            }
                                        }
                                    }
                                } catch (_: Exception) {
                                    // Skip broken embedded file.
                                }
                            } else {
                                val ext = embeddedNameLower.substringAfterLast(".", "")
                                val tmpFile = File.createTempFile(
                                    "ADS_",
                                    if (ext.isNotEmpty()) ".$ext" else ".tmp"
                                )
                                try {
                                    part.inputStream.use { input ->
                                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    scanEmbeddedFile(tmpFile, embeddedName)
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
                    val file = File(filePath)
                    FileInputStream(file).use { fileInputStream ->
                        HWPFDocument(fileInputStream).use { document ->
                            WordExtractor(document).use { extractor ->
                                extractor.paragraphText.forEachIndexed { index, text ->
                                    engine
                                        .scan(text)
                                        .filter { it.matcher::class in matchersClasses }
                                        .forEach {
                                            locations.add(BaseLocation(it, "Paragraph:$index"))
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
                                        .filter { it.matcher::class in matchersClasses }
                                        .forEach {
                                            locations.add(BaseLocation(it, "Comment:$index"))
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
                                        .filter { it.matcher::class in matchersClasses }
                                        .forEach {
                                            locations.add(BaseLocation(it, "Footnote:$index"))
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
                                        .filter { it.matcher::class in matchersClasses }
                                        .forEach {
                                            locations.add(BaseLocation(it, "Endnote:$index"))
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

    private data class DocxMaskLocation(
        val location: Location,
        val type: String,
        val position: Int
    )

    private fun parseMaskLocation(location: Location): DocxMaskLocation? {
        val type = location.location.substringBefore(",").trim()
        val position = location.location.substringAfter("Position:", "")
            .trim()
            .toIntOrNull()
            ?: return null
        if (type.isEmpty()) return null
        return DocxMaskLocation(location, type, position)
    }

    private fun replaceFirstInFooter(
        footer: XWPFFooter,
        target: String,
        replacement: String
    ): Boolean {
        if (replaceFirstInParagraphs(footer.paragraphs, target, replacement)) return true
        footer.tables.forEach { table ->
            if (replaceFirstInTable(table, target, replacement)) return true
        }
        return false
    }

    private fun replaceFirstInTable(
        table: XWPFTable,
        target: String,
        replacement: String
    ): Boolean {
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                if (replaceFirstInParagraphs(cell.paragraphs, target, replacement)) return true
            }
        }
        return false
    }

    private fun replaceFirstInParagraphs(
        paragraphs: List<XWPFParagraph>,
        target: String,
        replacement: String
    ): Boolean {
        paragraphs.forEach { paragraph ->
            if (replaceFirstInParagraph(paragraph, target, replacement)) return true
        }
        return false
    }

    private fun replaceFirstInParagraph(
        paragraph: XWPFParagraph,
        target: String,
        replacement: String
    ): Boolean {
        val position = PositionInParagraph(0, 0, 0)
        val segment = paragraph.searchText(target, position) ?: return false
        val beginRun = segment.beginRun
        val endRun = segment.endRun
        if (beginRun < 0 || endRun < 0) return false

        val beginTextIndex = segment.beginText
        val endTextIndex = segment.endText
        val beginChar = segment.beginChar
        val endChar = segment.endChar
        val beginRunText = paragraph.runs[beginRun].getText(beginTextIndex) ?: ""
        val endRunText = paragraph.runs[endRun].getText(endTextIndex) ?: ""

        if (beginRun == endRun) {
            val before = beginRunText.substring(0, beginChar)
            val after = if (endChar + 1 <= beginRunText.length) {
                beginRunText.substring(endChar + 1)
            } else {
                ""
            }
            paragraph.runs[beginRun].setText(before + replacement + after, beginTextIndex)
            return true
        }

        val before = beginRunText.substring(0, beginChar)
        val after = if (endChar + 1 <= endRunText.length) {
            endRunText.substring(endChar + 1)
        } else {
            ""
        }

        paragraph.runs[beginRun].setText(before + replacement, beginTextIndex)
        paragraph.runs[endRun].setText(after, endTextIndex)
        for (runIndex in endRun - 1 downTo beginRun + 1) {
            paragraph.removeRun(runIndex)
        }
        return true
    }
}