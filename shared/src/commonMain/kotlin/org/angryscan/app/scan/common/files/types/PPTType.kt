package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hslf.usermodel.HSLFTable
import org.apache.poi.hslf.usermodel.HSLFTextBox
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

object PPTType : IFileType {
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
                FileInputStream(file).use { fileInputStream ->
                    HSLFSlideShow(fileInputStream).use { presentation ->
                        presentation.slides.forEachIndexed { slideIndex, slide ->

                            if (slide.slideName != null) {
                                engine
                                    .scan(slide.slideName)
                                    .filter { it.matcher::class == matcher::class }
                                    .forEach {
                                        locations.add(
                                            Location(
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
                                    .filter { it.matcher::class == matcher::class }
                                    .forEach {
                                        locations.add(
                                            Location(
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
                                            .filter { it.matcher::class == matcher::class }
                                            .forEach {
                                                locations.add(
                                                    Location(
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
                                                    .filter { it.matcher::class == matcher::class }
                                                    .forEach {
                                                        locations.add(
                                                            Location(
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
                                    .filter { it.matcher::class == matcher::class }
                                    .forEach {
                                        locations.add(
                                            Location(
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
        } catch (_: Exception) {
            throw ScanException
        }
        return locations
    }
}