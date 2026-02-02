package org.angryscan.app.scan.common.files.types

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.IFileLocation
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import org.angryscan.app.scan.common.files.locations.BaseLocation
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hslf.usermodel.HSLFTable
import org.apache.poi.hslf.usermodel.HSLFTextBox
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger { }

@Serializable
object PPTType : FileType(), IFileLocation {
    override val name = "PPT"
    override val extensions = listOf("ppt", "pps", "pot")
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
                FileInputStream(file).use { fileInputStream ->
                    HSLFSlideShow(fileInputStream).use { presentation ->
                        presentation.slides.forEach { slide ->
                            str.append(slide.slideName).append("\n")
                            str.append(slide.title).append("\n")

                            slide.shapes.forEach { shape ->
                                when (shape) {
                                    is HSLFTextBox -> {
                                        str.append(shape.text).append("\n")
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

                                    is HSLFTable -> {
                                        for (row in 0..shape.numberOfRows - 1) {
                                            for (col in 0..shape.numberOfColumns - 1) {
                                                str.append(shape.getCell(row, col).text).append("\n")
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
                                                    if (isSampleOverload(sample, fastScan, isActive))
                                                        return@withContext
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                            slide.comments.forEach { comment ->
                                str.append(comment.text).append("\n")
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
        } catch (e: Exception) {
            logger.error { "Error while scanning ppt ${file.absolutePath}: ${e.message}" }
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
        fastScan: Boolean
    ): List<BaseLocation> {
        var length = 0
        var sample = 0
        val locations = mutableListOf<BaseLocation>()
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                FileInputStream(file).use { fileInputStream ->
                    HSLFSlideShow(fileInputStream).use { presentation ->
                        presentation.slides.forEachIndexed { slideIndex, slide ->

                            if (slide.slideName != null) {
                                engine
                                    .scan(slide.slideName)
                                    .forEach {
                                        locations.add(
                                            BaseLocation(
                                                it,
                                                "Slide: ${slideIndex + 1}"
                                            )
                                        )
                                    }
                                length += slide.slideName.length
                            }

                            if (slide.title != null) {
                                engine
                                    .scan(slide.title)
                                    .forEach {
                                        locations.add(
                                            BaseLocation(
                                                it,
                                                "Slide: ${slideIndex + 1}"
                                            )
                                        )
                                    }
                                length += slide.title.length
                            }

                            slide.shapes.forEach { shape ->
                                when (shape) {
                                    is HSLFTextBox -> {
                                        engine
                                            .scan(shape.text)
                                            .forEach {
                                                locations.add(
                                                    BaseLocation(
                                                        it,
                                                        "Slide: ${slideIndex + 1}"
                                                    )
                                                )
                                            }
                                        length += shape.text.length

                                        if (isLengthOverload(length, isActive)) {
                                            length = 0
                                            sample++
                                            if (isSampleOverload(sample, fastScan, isActive)) return@withContext
                                        }
                                    }

                                    is HSLFTable -> {
                                        for (row in 0..shape.numberOfRows - 1) {
                                            for (col in 0..shape.numberOfColumns - 1) {
                                                engine
                                                    .scan(shape.getCell(row, col).text)
                                                    .forEach {
                                                        locations.add(
                                                            BaseLocation(
                                                                it,
                                                                "Slide: ${slideIndex + 1}"
                                                            )
                                                        )
                                                    }
                                                if (isLengthOverload(length, isActive)) {
                                                    length = 0
                                                    sample++
                                                    if (isSampleOverload(sample, fastScan, isActive)) return@withContext
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                            slide.comments.forEach { comment ->
                                engine
                                    .scan(comment.text)
                                    .forEach {
                                        locations.add(
                                            BaseLocation(
                                                it,
                                                "Slide: ${slideIndex + 1}"
                                            )
                                        )
                                    }
                                if (isLengthOverload(length, isActive)) {
                                    length = 0
                                    sample++
                                    if (isSampleOverload(sample, fastScan, isActive)) return@withContext
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Error while finding locations in ppt ${filePath}: ${e.message}" }
            throw ScanException
        }
        return locations
    }
}