package org.angryscan.app.scan.common.files

interface IMaskFile {
    suspend fun maskLocations(
        inputFile: String,
        outputFile: String,
        locations: List<Location>
    ): Int
}