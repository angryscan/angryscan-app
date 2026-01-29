package org.angryscan.app.scan.common.files

import org.angryscan.common.engine.IScanEngine

interface IFileLocation {
    suspend fun findLocation(
        filePath: String,
        engine: IScanEngine,
        fastScan: Boolean = false
    ): List<Location>
}