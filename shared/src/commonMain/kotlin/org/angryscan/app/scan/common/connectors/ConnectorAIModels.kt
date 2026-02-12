package org.angryscan.app.scan.common.connectors

import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.FilesCounter
import org.angryscan.app.scan.common.files.types.IFileType
import java.io.File

/**
 * Placeholder connector for tasks created with AI Models source.
 * Allows deserialization of existing tasks; operations throw UnsupportedOperationException.
 */
@Serializable
class ConnectorAIModels : IConnector {
    override suspend fun getFile(filePath: String): File {
        throw UnsupportedOperationException("ConnectorAIModels is not implemented")
    }

    override suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (FoundedFile) -> Unit
    ): FilesCounter {
        throw UnsupportedOperationException("ConnectorAIModels is not implemented")
    }

    override fun toString(): String = "ConnectorAIModels"
}
