package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.*
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

object DOCXType : IFileType {
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
                FileInputStream(file).use { fileInputStream ->
                    XWPFDocument(fileInputStream).use { document ->
                        document.bodyElements
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