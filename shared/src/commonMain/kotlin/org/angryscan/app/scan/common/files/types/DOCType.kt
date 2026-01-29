package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.HWPFOldDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.hwpf.usermodel.Paragraph
import org.apache.poi.hwpf.usermodel.Range
import org.apache.poi.poifs.filesystem.DirectoryNode
import org.apache.poi.poifs.filesystem.Ole10Native
import org.apache.poi.poifs.filesystem.POIFSFileSystem
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
object DOCType : FileType(), IMaskFile, IFileLocation {
    override val name = "DOC"
    override val extensions = listOf("doc")

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

        suspend fun scanEmbeddedFromDirectory(dir: DirectoryNode) {
            dir.entries.forEach { entry ->
                if (entry is DirectoryNode) {
                    try {
                        val ole = Ole10Native.createFromEmbeddedOleObject(entry)
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
                                ft.scanFile(tmpFile, context, engines, fastScan, selectedExtensions).also { doc ->
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
                    } catch (_: Exception) {
                        // Not an embedded OLE Package directory - traverse deeper.
                        scanEmbeddedFromDirectory(entry)
                    }
                }
            }
        }

        try {
            withContext(Dispatchers.IO) {
                FileInputStream(file).use { inputStream ->
                    WordExtractor(inputStream).use { wordExtractor ->
                        wordExtractor.text.forEach { c ->
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

                // Scan embedded OLE packages (e.g., embedded XLS/DOC) inside DOC.
                try {
                    POIFSFileSystem(file).use { fs ->
                        scanEmbeddedFromDirectory(fs.root)
                    }
                } catch (_: Exception) {
                    // Skip embedded scan errors; main text scan result is still valid.
                }
            }
        } catch (_: Exception) {
            try {
                withContext(Dispatchers.IO) {
                    POIFSFileSystem(file).use { inputStream ->
                        HWPFOldDocument(inputStream).use { hwpfOldDocument ->
                            hwpfOldDocument.documentText.forEach { c ->
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

                    // Scan embedded OLE packages in fallback path as well.
                    try {
                        POIFSFileSystem(file).use { fs ->
                            scanEmbeddedFromDirectory(fs.root)
                        }
                    } catch (_: Exception) {
                        // Skip embedded scan errors.
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

        var locationsMasked = 0
        withContext(Dispatchers.IO) {
            FileInputStream(inputFile).use { inputStream ->
                HWPFDocument(inputStream).use { document ->
                    val locationsByType = maskableLocations.groupBy { it.type }
                    locationsMasked += maskRange(
                        document.range,
                        locationsByType["Paragraph"].orEmpty()
                    )
                    runCatching { document.commentsRange }.getOrNull()?.let { range ->
                        locationsMasked += maskRange(
                            range,
                            locationsByType["Comment"].orEmpty()
                        )
                    }
                    runCatching { document.footnoteRange }.getOrNull()?.let { range ->
                        locationsMasked += maskRange(
                            range,
                            locationsByType["Footnote"].orEmpty()
                        )
                    }
                    runCatching { document.endnoteRange }.getOrNull()?.let { range ->
                        locationsMasked += maskRange(
                            range,
                            locationsByType["Endnote"].orEmpty()
                        )
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
        fastScan: Boolean
    ): List<BaseLocation> {
        var length = 0
        var sample = 0
        val locations = mutableListOf<BaseLocation>()
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                val embeddedNames = mutableListOf<String>()
                var fallbackText: String? = null
                var hasEmbeddedObjects = false
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

                suspend fun scanEmbeddedFromDirectory(dir: DirectoryNode) {
                    dir.entries.forEach { entry ->
                        if (entry is DirectoryNode) {
                            if (entry.name.equals("ObjectPool", ignoreCase = true)) {
                                hasEmbeddedObjects = true
                            }
                            try {
                                val ole = Ole10Native.createFromEmbeddedOleObject(entry)
                                val realName = ole.fileName
                                    .substringAfterLast("\\")
                                    .substringAfterLast("/")
                                if (realName.isNotBlank()) {
                                    embeddedNames.add(realName)
                                }
                                hasEmbeddedObjects = true
                                val ext = realName.substringAfterLast(".", "")
                                val tmpFile = File.createTempFile(
                                    "ADS_",
                                    if (ext.isNotEmpty()) ".$ext" else ".tmp"
                                )
                                try {
                                    tmpFile.writeBytes(ole.dataBuffer)
                                    val embeddedTypes = IFileType.getFileType(tmpFile)
                                        .filterIsInstance<IFileLocation>()
                                        .ifEmpty { IFileType.getAll().filterIsInstance<IFileLocation>() }
                                    for (embeddedType in embeddedTypes) {
                                        val embeddedLocations = runCatching {
                                            embeddedType.findLocation(
                                                tmpFile.absolutePath,
                                                engine,
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
                                                        ), realName
                                                    )
                                                )
                                            }
                                            break
                                        }
                                    }
                                } finally {
                                    tmpFile.delete()
                                }
                            } catch (_: Exception) {
                                if (entry.name.isNotBlank()) {
                                    embeddedNames.add(entry.name)
                                }
                                scanEmbeddedFromDirectory(entry)
                            }
                        }
                    }
                }

                FileInputStream(file).use { inputStream ->
                    HWPFDocument(inputStream).use { document ->
                        fun scanRange(range: Range, typeLabel: String): Boolean {
                            for (index in 0 until range.numParagraphs()) {
                                val text = range.getParagraph(index).text()
                                    .replace("\u0007", "\n")
                                    .replace("\u000B", "\n")
                                engine
                                    .scan(text)
                                    .forEach {
                                        locations.add(BaseLocation(it, "$typeLabel:$index"))
                                    }
                                length += text.length
                                if (isLengthOverload(length, isActive)) {
                                    length = 0
                                    sample++
                                    if (isSampleOverload(sample, fastScan, isActive))
                                        return true
                                }
                            }
                            return false
                        }

                        if (scanRange(document.range, "Paragraph")) return@withContext
                        runCatching { document.commentsRange }.getOrNull()?.let { range ->
                            if (scanRange(range, "Comment")) return@withContext
                        }
                        runCatching { document.footnoteRange }.getOrNull()?.let { range ->
                            if (scanRange(range, "Footnote")) return@withContext
                        }
                        runCatching { document.endnoteRange }.getOrNull()?.let { range ->
                            if (scanRange(range, "Endnote")) return@withContext
                        }
                        if (locations.isEmpty()) {
                            WordExtractor(document).use { extractor ->
                                fallbackText = extractor.text
                            }
                        }
                    }
                }

                try {
                    POIFSFileSystem(file).use { fs ->
                        scanEmbeddedFromDirectory(fs.root)
                    }
                } catch (_: Exception) {
                    // Skip embedded scan errors.
                }

                if (locations.isEmpty() && !fallbackText.isNullOrBlank()) {
                    val attachmentName = if (hasEmbeddedObjects) {
                        embeddedNames.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Embedded file"
                    } else {
                        null
                    }
                    engine
                        .scan(fallbackText)
                        .forEach { match ->
                            val locationLabel = if (attachmentName != null) {
                                "Attachment: $attachmentName, Text"
                            } else {
                                "Text"
                            }
                            locations.add(
                                BaseLocation(
                                    match,
                                    locationLabel,
                                    attachmentName = attachmentName
                                )
                            )
                        }
                }
            }
        } catch (_: Exception) {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    val str = StringBuilder()
                    POIFSFileSystem(file).use { inputStream ->
                        HWPFOldDocument(inputStream).use { hwpfOldDocument ->
                            hwpfOldDocument.documentText.forEach { c ->
                                str.append(c)
                                if (isLengthOverload(str.length, isActive)) {
                                    engine
                                        .scan(str.toString())
                                        .forEach {
                                            locations.add(BaseLocation(it, ""))
                                        }
                                    str.clear()
                                    sample++
                                    if (isSampleOverload(sample, fastScan, isActive))
                                        return@withContext
                                }
                            }
                        }
                    }

                    try {
                        POIFSFileSystem(file).use { fs ->
                            suspend fun scanEmbeddedFromDirectory(dir: DirectoryNode) {
                                dir.entries.forEach { entry ->
                                    if (entry is DirectoryNode) {
                                        try {
                                            val ole = Ole10Native.createFromEmbeddedOleObject(entry)
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
                                                val embeddedTypes = IFileType.getFileType(tmpFile)
                                                    .filterIsInstance<IFileLocation>()
                                                    .ifEmpty { IFileType.getAll().filterIsInstance<IFileLocation>() }
                                                for (embeddedType in embeddedTypes) {
                                                    val embeddedLocations = runCatching {
                                                        embeddedType.findLocation(
                                                            tmpFile.absolutePath,
                                                            engine,
                                                            fastScan
                                                        ).map {
                                                            it as BaseLocation
                                                        }
                                                    }.getOrDefault(emptyList())
                                                    if (embeddedLocations.isNotEmpty()) {
                                                        embeddedLocations.forEach { location ->
                                                            val attachmentLabel = location.attachmentName ?: realName
                                                            val baseLabel = "Attachment: $attachmentLabel"
                                                            val locationLabel = if (location.location.isNotBlank()) {
                                                                "$baseLabel, ${location.location}"
                                                            } else {
                                                                baseLabel
                                                            }
                                                            locations.add(
                                                                location.copy(
                                                                    location = locationLabel,
                                                                    attachmentName = attachmentLabel
                                                                )
                                                            )
                                                        }
                                                        break
                                                    }
                                                }
                                            } finally {
                                                tmpFile.delete()
                                            }
                                        } catch (_: Exception) {
                                            scanEmbeddedFromDirectory(entry)
                                        }
                                    }
                                }
                            }

                            scanEmbeddedFromDirectory(fs.root)
                        }
                    } catch (_: Exception) {
                        // Skip embedded scan errors.
                    }
                }
            } catch (_: Exception) {
                throw ScanException
            }
        }
        return locations
    }

    private data class DocMaskLocation(
        val location: Location,
        val type: String,
        val index: Int
    )

    private fun parseMaskLocation(location: Location): DocMaskLocation? {
        val type = location.location.substringBefore(":", "").trim()
        val index = location.location.substringAfter(":", "")
            .trim()
            .toIntOrNull()
            ?: return null
        if (type.isEmpty()) return null
        return DocMaskLocation(location, type, index)
    }

    private fun maskRange(range: Range, locations: List<DocMaskLocation>): Int {
        var masked = 0
        locations
            .sortedBy { it.index }
            .forEach { maskLocation ->
                if (maskLocation.index < 0 || maskLocation.index >= range.numParagraphs()) {
                    return@forEach
                }
                val paragraph = range.getParagraph(maskLocation.index)
                val target = maskLocation.location.entry.value
                val replacement = maskLocation.location.mask()
                var replaced = replaceFirstInParagraph(paragraph, target, replacement)
                if (!replaced && paragraph.text().contains(target)) {
                    paragraph.replaceText(target, replacement)
                    replaced = true
                }
                if (replaced || !paragraph.text().contains(target)) {
                    masked++
                }
            }
        return masked
    }

    private fun replaceFirstInParagraph(
        paragraph: Paragraph,
        target: String,
        replacement: String
    ): Boolean {
        for (runIndex in 0 until paragraph.numCharacterRuns()) {
            val run = paragraph.getCharacterRun(runIndex)
            val text = run.text() ?: ""
            if (text.contains(target)) {
                val replaced = text.replaceFirst(target, replacement)
                if (replaced != text) {
                    run.replaceText(text, replaced)
                }
                return true
            }
        }
        return false
    }
}