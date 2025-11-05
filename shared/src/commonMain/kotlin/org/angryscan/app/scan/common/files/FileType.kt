package org.angryscan.app.scan.common.files

import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement
import org.odftoolkit.odfdom.dom.element.table.TableTableElement
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph
import org.odftoolkit.simple.PresentationDocument
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.types.*
import org.angryscan.app.scan.functions.CertFileType
import org.angryscan.app.scan.functions.CodeFileType
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.CoroutineContext

enum class FileType(val extensions: List<String>) : KoinComponent {
    XLSX(listOf("xlsx")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document = XLSXType.scanFile(file, context, engines, fastScan)
    },
    DOCX(listOf("docx")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document = DOCXType.scanFile(file, context, engines, fastScan)
    },
    PPTX(listOf("pptx", "potx", "ppsx", "pptm")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document = PPTXType.scanFile(file, context, engines, fastScan)
    },
    PPT(listOf("ppt", "pps", "pot")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document = PPTType.scanFile(file, context, engines, fastScan)
    },
    Text((1..999).map { it.toString().padStart(3, '0') } + listOf("txt", "csv", "xml", "json", "log")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document = TextType.scanFile(file, context, engines, fastScan)
    },
    DOC(listOf("doc")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document = DOCType.scanFile(file, context, engines, fastScan)
    },
    XLS(listOf("xls")) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ) = XLSType.scanFile(file, context, engines, fastScan)
    },
    PDF(listOf("pdf")) {
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
                            if (str.length >= scanSettings.sampleLength || !isActive) {
                                engines.forEach { engine ->
                                    res + withContext(context) { scan(str.toString(), engine) }
                                }
                                str.clear()
                                sample++
                                if (isSampleOverload(sample, fastScan) || !isActive) return@withContext
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
    },
    ODT(listOf("odt")) {
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
                    OdfTextDocument.loadDocument(file).contentRoot.also { content ->
                        for (contIt in 0 until content.length) {
                            when (val item = content.item(contIt)) {
                                is OdfTextParagraph -> {
                                    if (item.textContent.isNotEmpty()) {
                                        str.append(item.textContent).append("\n")
                                        if (str.length >= scanSettings.sampleLength || !isActive) {
                                            engines.forEach { engine ->
                                                res + withContext(context) { scan(str.toString(), engine) }
                                            }
                                            str.clear()
                                            sample++
                                            if (isSampleOverload(sample, fastScan) || !isActive) return@withContext
                                        }
                                    }
                                }

                                is TableTableElement -> {
                                    for (rowIt in 0 until item.length) {
                                        item.item(rowIt).also { row ->
                                            if (row is TableTableRowElement) {
                                                for (celIt in 0 until row.length) {
                                                    val celElement = row.item(celIt)
                                                    if (celElement is TableTableCellElement) {
                                                        for (celContIt in 0 until celElement.length) {
                                                            celElement.item(celContIt).textContent.also { text ->
                                                                if (text.isNotEmpty()) {
                                                                    str.append(text).append("\n")
                                                                    if (str.length >= scanSettings.sampleLength || !isActive) {
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
                                                }
                                            }
                                        }
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
    },
    ODP(listOf("odp", "otp")) {
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
                    PresentationDocument.loadDocument(file).use { document ->
                        val slideIterator = document.slides
                        while (slideIterator.hasNext()) {
                            val slide = slideIterator.next()
                            str.append(slide.slideName).append("\n")

                            slide.tableList.forEach { table ->
                                table.rowList.forEach { r ->
                                    for (i in 0..r.cellCount - 1) {
                                        str.append(r.getCellByIndex(i).displayText).append("\n")
                                        if (str.length >= scanSettings.sampleLength || !isActive) {
                                            engines.forEach { engine ->
                                                res + withContext(context) { scan(str.toString(), engine) }
                                            }
                                            str.clear()
                                            sample++
                                            if (isSampleOverload(sample, fastScan) || !isActive) return@withContext
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
                                    if (str.length >= scanSettings.sampleLength || !isActive) {
                                        engines.forEach { engine ->
                                            res + withContext(context) { scan(str.toString(), engine) }
                                        }
                                        str.clear()
                                        sample++
                                        if (isSampleOverload(sample, fastScan) || !isActive) return@withContext
                                    }
                                }
                            }
                            val textboxIterator = slide.textboxIterator
                            while (textboxIterator.hasNext()) {
                                val textbox = textboxIterator.next()
                                str.append(textbox.textContent).append("\n")
                                if (str.length >= scanSettings.sampleLength || !isActive) {
                                    engines.forEach { engine ->
                                        res + withContext(context) { scan(str.toString(), engine) }
                                    }
                                    str.clear()
                                    sample++
                                    if (isSampleOverload(sample, fastScan) || !isActive) return@withContext
                                }
                            }
                            val noteListIterator = slide.notesPage?.listIterator
                            if (noteListIterator != null) {
                                while (noteListIterator.hasNext()) {
                                    val noteList = noteListIterator.next()
                                    noteList.items.forEach {
                                        str.append(it.textContent).append("\n")
                                        if (str.length >= scanSettings.sampleLength || !isActive) {
                                            engines.forEach { engine ->
                                                res + withContext(context) { scan(str.toString(), engine) }
                                            }
                                            str.clear()
                                            sample++
                                            if (isSampleOverload(sample, fastScan) || !isActive) return@withContext
                                        }
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
    },
    ODS(listOf("ods")) {
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
                    OdfSpreadsheetDocument.loadDocument(file).use { document ->
                        document.spreadsheetTables.forEach { table ->
                            table.rowElementList.forEach { row ->
                                if (row is TableTableRowElement) {
                                    for (celIt in 0 until row.length) {
                                        val celElement = row.item(celIt)
                                        if (celElement is TableTableCellElement) {
                                            for (celContIt in 0 until celElement.length) {
                                                celElement.item(celContIt).textContent.also { text ->
                                                    if (text.isNotEmpty()) {
                                                        str.append(text).append("\n")
                                                        if (str.length >= scanSettings.sampleLength || !isActive) {
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
    },
    ZIP(listOf("zip")) {
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
                                        getFileType(tmpFile)?.scanFile(tmpFile, context, engines, fastScan)
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
    },
    RAR(listOf("rar")) {
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
                            getFileType(tmpFile)?.scanFile(tmpFile, context, engines, fastScan)?.also { doc ->
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
    },
    CERT(CertFileType.entries.flatMap { it.extensions }) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document {
            return CertFileType
                .entries.find { it.extensions.contains(file.extension) }
                ?.scanFile(
                    file,
                    context,
                    engines,
                    fastScan
                ) ?: Document(file.length(), file.absolutePath)
                .also {
                    it.skip()
                }
        }
    },
    CODE(CodeFileType.entries.flatMap { it.extensions }) {
        override suspend fun scanFile(
            file: File,
            context: CoroutineContext,
            engines: List<IScanEngine>,
            fastScan: Boolean
        ): Document {
            return CodeFileType.scanFile(
                file
            )
        }
    };

    abstract suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document

    protected fun scan(text: String, engine: IScanEngine): Map<IMatcher, Int> {
        return engine
            .scan(text)
            .groupBy { it.matcher }
            .map { it.key to it.value.size }
            .toMap()
    }

    companion object : KoinComponent {
        fun getFileType(file: File): FileType? {
            return entries.find { fileType -> fileType.extensions.contains(file.extension) }
        }
        fun getFileType(filePath: String): FileType? =
            getFileType(File(filePath))

        private fun selectedExtension(fileName: String): Boolean =
            entries.filter {
                scanSettings.extensions.contains(it) // Заменить на загруженные из задачи, а не из текущих настроек
            }.flatMap {
                it.extensions
            }.any { fileName.endsWith(it) }

        fun isSampleOverload(sample: Int, fastScan: Boolean): Boolean {
            return (fastScan && sample >= scanSettings.sampleCount)
        }

        private val scanSettings: ScanSettings by inject()
    }
}