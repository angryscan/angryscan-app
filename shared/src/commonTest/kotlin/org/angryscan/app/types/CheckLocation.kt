package org.angryscan.app.types

import kotlinx.coroutines.runBlocking
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.app.scan.engine.toKotlinMatchers
import org.angryscan.app.searcher.Matrix
import org.angryscan.common.engine.IMask
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.hyperscan.IHyperMatcher
import org.angryscan.common.engine.kotlin.IKotlinMatcher
import org.angryscan.common.engine.kotlin.KotlinEngine
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

object CheckLocation {
    fun checkByMap(fileName: String, scanMethod: suspend (String, IScanEngine, List<IMatcher>) -> List<Location>) {
        val map = Matrix.getMap(fileName.replace("/files/", ""))
        assertNotNull(map)

        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val hyperLocations = getHyperLocations(
            filePath,
            map.keys.toList().toHyperScanMatchers(),
            scanMethod
        )

        hyperLocations.forEach { location ->
            assertTrue(
                location.location.isNotEmpty(),
                "Wrong location for ${location.entry.matcher.name} and entry ${location.entry}"
            )
        }

        map.keys.forEach { matcher ->
            assertEquals(
                map[matcher],
                hyperLocations.filter { it.entry.matcher::class == matcher::class }.size,
                "Wrong number of locations for ${matcher.name}"
            )
        }
        val kotlinLocations = getKotlinLocations(
            filePath,
            map.keys.toList().toKotlinMatchers(),
            scanMethod
        )

        kotlinLocations.forEach { location ->
            assertTrue(
                location.location.isNotEmpty(),
                "Wrong location for ${location.entry.matcher.name} and entry ${location.entry}"
            )
        }

        map.keys.forEach { matcher ->
            assertEquals(
                map[matcher],
                kotlinLocations.filter { it.entry.matcher::class == matcher::class }.size,
                "Wrong number of locations for ${matcher.name}"
            )
        }
    }

    fun getHyperLocations(
        filePath: String,
        matchers: List<IHyperMatcher>,
        scanMethod: suspend (String, HyperScanEngine, List<IMatcher>) -> List<Location>
    ): List<Location> {
        val engine = HyperScanEngine(matchers)
        return runBlocking { scanMethod(filePath, engine, matchers) }
    }

    fun getKotlinLocations(
        filePath: String,
        matchers: List<IKotlinMatcher>,
        scanMethod: suspend (String, KotlinEngine, List<IMatcher>) -> List<Location>
    ): List<Location> {
        val engine = KotlinEngine(matchers)
        return runBlocking { scanMethod(filePath, engine, matchers) }
    }

    fun maskLocations(
        fileName: String,
        scanMethod: suspend (String, IScanEngine, List<IMatcher>) -> List<Location>,
        maskMethod: suspend (String, String, List<Location>) -> Int
    ) {
        val map = Matrix
            .getMap(fileName.replace("/files/", ""))
            ?.filter { m -> m.key is IMask }
        assertNotNull(map)

        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val hyperLocations = getHyperLocations(
            filePath,
            map.keys.toList().toHyperScanMatchers(),
            scanMethod
        )
        val kotlinLocations = getKotlinLocations(
            filePath,
            map.keys.toList().toKotlinMatchers(),
            scanMethod
        )
        assertEquals(hyperLocations.size, kotlinLocations.size)

        assertNotEquals(0, hyperLocations.size)

        val tmpFile = File.createTempFile(
            "ADS_",
            "." + fileName.substringAfterLast('.')
        )

        runBlocking { maskMethod(filePath, tmpFile.absolutePath, hyperLocations) }

        val rescanLocations = getHyperLocations(
            tmpFile.absolutePath,
            map.keys.toList().toHyperScanMatchers(),
            scanMethod
        )
        assertEquals(0, rescanLocations.size)
        tmpFile.delete()

        runBlocking { maskMethod(filePath, tmpFile.absolutePath, kotlinLocations) }

        val rescanKotlinLocations = getKotlinLocations(
            tmpFile.absolutePath,
            map.keys.toList().toKotlinMatchers(),
            scanMethod
        )
        assertEquals(0, rescanKotlinLocations.size)
        tmpFile.delete()
    }
}