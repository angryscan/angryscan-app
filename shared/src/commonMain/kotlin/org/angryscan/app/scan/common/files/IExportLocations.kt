package org.angryscan.app.scan.common.files

interface IExportLocations {
    suspend fun exportRows(
        inputFile: String,
        locations: List<Location>,
        outputFile: String
    ): Int
}