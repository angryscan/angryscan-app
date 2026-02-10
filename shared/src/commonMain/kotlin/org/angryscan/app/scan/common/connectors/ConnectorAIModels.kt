package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.FilesCounter
import org.angryscan.app.scan.common.files.types.IFileType
import java.io.File

@Serializable
class ConnectorAIModels : IConnector {
    override suspend fun getFile(filePath: String): File =
        withContext(Dispatchers.IO) { File(filePath) }

    override suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (FoundedFile) -> Unit
    ): FilesCounter = FilesCounter()

    override fun toString(): String = "ConnectorAIModels"
}
