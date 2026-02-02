package org.angryscan.app.scan.common.files.types

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine
import org.odftoolkit.simple.PresentationDocument
import java.io.File
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {  }

@Serializable
@Suppress("unused")
object ODPType: FileType() {
    override val name = "ODP"
    override val extensions = listOf("odp", "otp")
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
                PresentationDocument.loadDocument(file).use { document ->
                    val slideIterator = document.slides
                    while (slideIterator.hasNext()) {
                        val slide = slideIterator.next()
                        str.append(slide.slideName).append("\n")

                        slide.tableList.forEach { table ->
                            table.rowList.forEach { r ->
                                for (i in 0..r.cellCount - 1) {
                                    str.append(r.getCellByIndex(i).displayText).append("\n")
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

                        val listIterator = slide.listIterator
                        while (listIterator.hasNext()) {
                            val list = listIterator.next()
                            str.append(list.header)
                            list.items.forEach {
                                str.append(it.textContent).append("\n")
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
                        val textboxIterator = slide.textboxIterator
                        while (textboxIterator.hasNext()) {
                            val textbox = textboxIterator.next()
                            str.append(textbox.textContent).append("\n")
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
                        val noteListIterator = slide.notesPage?.listIterator
                        if (noteListIterator != null) {
                            while (noteListIterator.hasNext()) {
                                val noteList = noteListIterator.next()
                                noteList.items.forEach {
                                    str.append(it.textContent).append("\n")
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
            }
        } catch (e: Exception) {
            logger.error { "Filed to scan ODP file ${file.absolutePath}: ${e.message}" }
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