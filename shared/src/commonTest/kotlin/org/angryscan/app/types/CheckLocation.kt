package org.angryscan.app.types

import kotlinx.coroutines.runBlocking
import org.angryscan.app.scan.common.files.Location
import org.angryscan.app.searcher.Matrix
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.hyperscan.IHyperMatcher
import org.angryscan.common.matchers.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val targetMatchers = listOf<IHyperMatcher>(
    Email,
    CardNumber(),
    Phone,
    SNILS,
    Passport,
    OMS,
    INN,
    Address,
    Login,
    BankAccount,
    VehicleRegNumber,
    Password,
    CVV,
    FullName,
    IPv4,
    IPv6
)

object CheckLocation {
    fun checkByMap(fileName: String, scanMethod: suspend (String, HyperScanEngine, IMatcher) -> List<Location>) {
        val map = Matrix.getMap(fileName.replace("/files/", ""))
        assertNotNull(map)

        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val engine = HyperScanEngine(targetMatchers)

        map.keys.forEach { matcher ->
            val locations =
                runBlocking { scanMethod(filePath, engine, matcher) }//runBlocking {  DOCXType.findLocation(fileName, engine, matcher) }
            assertEquals(map[matcher], locations.size, "Wrong number of locations for ${matcher.name}")
            locations.forEach { location ->
                assertTrue(
                    location.location.isNotEmpty(),
                    "Wrong location for ${matcher.name} and entry ${location.entry}"
                )
            }
        }
    }
}