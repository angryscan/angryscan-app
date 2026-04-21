package org.angryscan.app.scan.common.connectors

import org.angryscan.app.scan.common.ObjectCounter
import org.angryscan.app.scan.common.files.types.IFileType
import java.io.File

interface IConnector

interface IFileConnector : IConnector {
    suspend fun getFile(filePath: String): File
    suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter
}

interface IDatabaseConnector : IConnector {
    suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter

    suspend fun getTableContent(tablePath: String): String

    /** Returns table rows as list of column-name to string value. Used for per-column scanning. */
    suspend fun getTableContentStructured(tablePath: String): List<Map<String, String>>

    /** Human-readable one-line summary of connection parameters for logs (no secrets). */
    fun logSummary(): String
}