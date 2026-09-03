package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import org.angryscan.app.scan.common.files.types.IFileType
import java.io.File

@Serializable
class ConnectorFileShare: IFileConnector {
    override suspend fun getFile(filePath: String): File =
        withContext(Dispatchers.IO) {
            return@withContext File(filePath)
        }

    override suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (FoundedFile) -> Unit
    ): ObjectCounter =
        withContext(Dispatchers.IO) {
            val d = File(dir)
            var filesCounter = ObjectCounter()

            if (d.isDirectory) {
                val items = d.listFiles() ?: return@withContext ObjectCounter()
                for (item in items) {
                    if (item.isDirectory) {
                        try {
                            filesCounter += scanDirectory(item.absolutePath, extensions, fileSelected)
                        } catch (_: Exception) {

                        }
                    } else if (item.isFile) {
                        filesCounter.add(item.length())

                        if (extensions.any { it.allowExtension(item.extension) }) {
                            val foundedFile = FoundedFile(
                                path = item.absolutePath,
                                size = item.length()
                            )
                            fileSelected(foundedFile)
                        }
                    }
                }
            } else if (d.isFile) {
                // Skip non-existent paths (e.g. CSV data rows mistaken for paths).
                filesCounter.add(d.length())

                if (extensions.any { it.allowExtension(d.extension) }) {
                    val foundedFile = FoundedFile(
                        path = d.absolutePath,
                        size = d.length()
                    )
                    fileSelected(foundedFile)
                }
            }
            return@withContext filesCounter
        }

    override fun toString(): String {
        return "ConnectorFileShare"
    }
}