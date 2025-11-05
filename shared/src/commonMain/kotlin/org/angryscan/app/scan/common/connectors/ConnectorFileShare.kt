package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.FilesCounter
import java.io.File

@Serializable
class ConnectorFileShare: IConnector {
    override suspend fun getFile(filePath: String): File =
        withContext(Dispatchers.IO) {
            return@withContext File(filePath)
        }

    override suspend fun scanDirectory(
        dir: String,
        extensions: List<String>,
        fileSelected: (FoundedFile) -> Unit
    ): FilesCounter =
        withContext(Dispatchers.IO) {
            val d = File(dir)
            var filesCounter = FilesCounter()

            if (d.isDirectory) {
                val items = d.listFiles() ?: return@withContext FilesCounter()
                for (item in items) {
                    if (item.isDirectory) {
                        try {
                            filesCounter += scanDirectory(item.absolutePath, extensions, fileSelected)
                        } catch (_: Exception) {

                        }
                    } else {
                        filesCounter.add(item.length())

                        if (extensions.any { item.extension == it }) {
                            val foundedFile = FoundedFile(
                                path = item.absolutePath,
                                size = item.length()
                            )
                            fileSelected(foundedFile)
                        }
                    }
                }
            } else {
                filesCounter.add(d.length())

                if (extensions.any { d.extension == it }) {
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