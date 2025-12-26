package org.angryscan.app.scan.common.connectors

import org.angryscan.app.scan.common.FilesCounter
import org.angryscan.app.scan.common.files.types.IFileType
import java.io.File

interface IConnector {
    suspend fun getFile(filePath: String): File
    suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (file: FoundedFile) -> Unit
    ): FilesCounter
}