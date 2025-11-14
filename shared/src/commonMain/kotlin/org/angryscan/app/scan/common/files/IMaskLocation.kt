package org.angryscan.app.scan.common.files

interface IMaskLocation {
    suspend fun maskLocations(
        inputFile: String,
        outputFile: String,
        locations: List<Location>
    ): Int
}