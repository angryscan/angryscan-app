package org.angryscan.app.types

import kotlinx.coroutines.runBlocking
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.app.searcher.Matrix
import org.angryscan.common.engine.IMask
import org.angryscan.common.engine.hyperscan.IHyperMatcher
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

object CheckLocation {
    fun checkByMap(fileName: String, scanMethod: suspend (String, HyperScanEngine, IMatcher) -> List<Location>) {
        val map = Matrix.getMap(fileName.replace("/files/", ""))
        assertNotNull(map)

        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val locations = getLocations(
            filePath,
            map.keys.toList().toHyperScanMatchers(),
            scanMethod
        )

        locations.forEach { location ->
            assertTrue(
                location.location.isNotEmpty(),
                "Wrong location for ${location.entry.matcher.name} and entry ${location.entry}"
            )
        }

        map.keys.forEach { matcher ->
            assertEquals(
                map[matcher],
                locations.filter { it.entry.matcher::class == matcher::class }.size,
                "Wrong number of locations for ${matcher.name}"
            )

        }
    }

    fun getLocations(
        filePath: String,
        matchers: List<IHyperMatcher>,
        scanMethod: suspend (String, HyperScanEngine, IMatcher) -> List<Location>
    ): List<Location> {
        val engine = HyperScanEngine(matchers)
        return runBlocking { matchers.flatMap { scanMethod(filePath, engine, it) } }
    }

    fun maskLocations(
        fileName: String,
        scanMethod: suspend (String, HyperScanEngine, IMatcher) -> List<Location>,
        maskMethod: suspend (String, String, List<Location>) -> Int
    ) {
        val map = Matrix
            .getMap(fileName.replace("/files/", ""))
            ?.filter { m -> m.key is IMask }
        assertNotNull(map)

        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val locations = getLocations(
            filePath,
            map.keys.toList().toHyperScanMatchers(),
            scanMethod
        )

        assertNotEquals(0, locations.size)

        val tmpFile = File.createTempFile(
            "ADS_",
            "." + fileName.substringAfterLast('.')
        )

        runBlocking { maskMethod(filePath, tmpFile.absolutePath, locations) }

        val rescanLocations = getLocations(
            tmpFile.absolutePath,
            map.keys.toList().toHyperScanMatchers(),
            scanMethod
        )
        assertEquals(0, rescanLocations.size)
        tmpFile.delete()
    }
}