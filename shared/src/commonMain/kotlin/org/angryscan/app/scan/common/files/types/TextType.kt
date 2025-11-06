package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.mozilla.universalchardet.UniversalDetector
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.common.files.LocationFinder.ScanException
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

object TextType : IFileType {
    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
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
        matcher: IMatcher,
        fastScan: Boolean
    ): List<Location> {
        var length = 0
        var sample = 0
        var lineNumber = 1
        val locations = mutableListOf<Location>()
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                val encoding = UniversalDetector.detectCharset(file)
                file.bufferedReader(charset = Charset.forName(encoding)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        engine
                            .scan(line)
                            .filter { it.matcher::class == matcher::class }
                            .forEach {
                                locations.add(Location(it, "N $lineNumber"))
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
}