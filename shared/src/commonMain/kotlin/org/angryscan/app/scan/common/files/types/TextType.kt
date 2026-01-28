package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.IExportLocations
import org.angryscan.app.scan.common.files.IFileLocation
import org.angryscan.app.scan.common.files.IMaskFile
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import org.angryscan.app.scan.common.files.extensions.isMaskable
import org.angryscan.app.scan.common.files.extensions.mask
import org.angryscan.app.scan.common.files.locations.TextLocation
import org.angryscan.app.ui.strings.readableName
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

@Serializable
object TextType : FileType(), IMaskFile, IFileLocation, IExportLocations {
    override val name = "Text"
    override val extensions = (
            listOf(
                // Plain text / docs
                "txt",
                "md",
                "markdown",
                "rst",
                "adoc",

                // Structured data
                "csv",
                "tsv",
                "json",
                "jsonl",
                "ndjson",
                "xml",
                "yml",
                "yaml",
                "toml",

                // Config formats
                "ini",
                "cfg",
                "conf",
                "properties",
                "prop",
                "prefs",
                "env",   // sometimes used as file.env

                // IaC / deployment configs (non-code)
                "hcl",

                // Web text formats (still plain text)
                "html",
                "htm",
                "css",
                "scss",
                "sass",
                "less",
                "svg",

                // Logs / traces
                "log",
                "out",
                "err",
                "trace",

                // Diffs / patches
                "diff",
                "patch",

                // Lock / manifests (dependency & tooling metadata)
                "lock",

                // No extension (e.g., Dockerfile, Makefile, README, .env)
                ""
            ) +
                    //Code extensions also text format
                    CodeFileType.entries.flatMap { it.extensions }
            )
        .distinct()

    override fun allowExtension(ext: String): Boolean {
        return ext in extensions || """^\d+$""".toRegex().matches(ext)
    }

    override fun extensions() =
        extensions.filter { !"^\\d{1,3}$".toRegex().matches(it) }

    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean,
        selectedExtensions: List<IFileType>
    ): Document {
        val str = StringBuilder()
        val res = Document(file.length(), file.absolutePath)
        val buf = CharArray(1000)
        var sample = 0
        try {
            withContext(Dispatchers.IO) {
                val encoding = UniversalDetector.detectCharset(file)
                FileInputStream(file).use { fileInputStream ->
                    fileInputStream.bufferedReader(charset = Charset.forName(encoding)).use { reader ->
                        var actualRead: Int
                        while (true) {
                            actualRead = reader.read(buf)
                            if (actualRead <= 0) {
                                break
                            }

                            str.append(buf)

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
        matchers: List<IMatcher>,
        fastScan: Boolean
    ): List<TextLocation> {
        var length = 0
        var sample = 0
        var lineNumber = 1
        val locations = mutableListOf<TextLocation>()
        val matchersClasses = matchers.map { it::class }
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                val encoding = UniversalDetector.detectCharset(file)
                file.bufferedReader(charset = Charset.forName(encoding)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        engine
                            .scan(line)
                            .filter { it.matcher::class in matchersClasses }
                            .forEach {
                                locations.add(TextLocation(
                                    entry = it,
                                    line = lineNumber,
                                    position = it.startPosition
                                ))
                            }

                        length += line.length
                        lineNumber++
                        if (isLengthOverload(length, isActive)) {
                            length = 0
                            sample++
                            if (isSampleOverload(sample, fastScan, isActive))
                                return@withContext
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (_: Exception) {
            throw ScanException
        }
        return locations
    }

    override suspend fun maskLocations(
        inputFile: String,
        outputFile: String,
        locations: List<Location>
    ): Int {
        val sortedLocations = locations
            .map { it as TextLocation }
            .filter { it.isMaskable() }
            .sortedBy { it.line }
        var currentLine = 1
        var locationIndex = 0
        var locationLine = sortedLocations[locationIndex].line
        var locationsMasked = 0

        withContext(Dispatchers.IO) {
            val file = File(inputFile)
            val encoding = UniversalDetector.detectCharset(file) ?: "UTF-8"
            val charset = runCatching { Charset.forName(encoding) }.getOrDefault(Charsets.UTF_8)

            File(outputFile)
                .bufferedWriter(charset = charset)
                .use { writer ->
                    file
                        .bufferedReader(charset = Charset.forName(encoding))
                        .use { reader ->
                            var line = reader.readLine()
                            while (line != null) {
                                var writeLine = line

                                while (currentLine == locationLine) {
                                    val tmp = writeLine
                                        .replaceFirst(
                                            sortedLocations[locationIndex].entry.value,
                                            sortedLocations[locationIndex].mask()
                                        )
                                    if (tmp != writeLine) {
                                        writeLine = tmp
                                        locationsMasked++
                                        if(locationsMasked == locations.count())
                                            return@withContext
                                    }
                                    locationIndex++
                                    if (locationIndex < sortedLocations.size) {
                                        locationLine = sortedLocations[locationIndex]
                                            .location
                                            .substring(2)
                                            .toInt()
                                    } else {
                                        locationLine = -1
                                    }
                                }
                                writer.write(writeLine)
                                writer.newLine()

                                currentLine++
                                line = reader.readLine()
                            }
                        }
                }
        }
        return locationsMasked
    }

    override suspend fun exportRows(
        inputFile: String,
        locations: List<Location>,
        outputFile: String
    ): Int {
        val rows = locations
            .map { it as TextLocation }
            .groupBy { it.line }
            .map { it.key to it.value }
            .sortedBy { it.first }

        var currentLine = 1
        var rowIndex = 0
        var rowsExported = 0

        withContext(Dispatchers.IO) {
            val file = File(inputFile)
            val encoding = UniversalDetector.detectCharset(file) ?: "UTF-8"
            val charset = runCatching { Charset.forName(encoding) }.getOrDefault(Charsets.UTF_8)

            File(outputFile)
                .bufferedWriter(charset = charset)
                .use { writer ->
                    file
                        .bufferedReader(charset = Charset.forName(encoding))
                        .use { reader ->
                            var line = reader.readLine()
                            while (line != null && rowIndex < rows.size) {
                                if (currentLine == rows[rowIndex].first) {
                                    //Write matchers names
                                    writer.write(rows[rowIndex].second.map { it.entry.matcher.readableName() }.joinToString(", ") + ";")
                                    //Write row
                                    writer.write(line)
                                    writer.newLine()
                                    rowIndex++
                                    rowsExported++
                                }

                                currentLine++
                                line = reader.readLine()
                            }
                        }
                }
        }
        return rowsExported
    }
}