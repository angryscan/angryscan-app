package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.app.scan.common.files.types.DOCType
import org.angryscan.app.scan.engine.toKotlinMatchers
import org.angryscan.common.matchers.CardNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DOCTypeTest : IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.doc"

        CheckLocation.checkByMap(
            fileName = fileName,
            DOCType::findLocation
        )
    }

    @Test
    fun maskLocation() {
        val fileName = "first/first.doc"

        CheckLocation.maskLocations(
            fileName,
            DOCType::findLocation,
            DOCType::maskLocations
        )
    }

    @Test
    fun findLocationsInCardDoc() {
        val fileName = "cardNumber/card.doc"
        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val matcher = CardNumber()
        val locations = CheckLocation.getHyperLocations(
            filePath,
            listOf(matcher).toHyperScanMatchers(),
            DOCType::findLocation
        )
        val kotlinLocations = CheckLocation.getKotlinLocations(
            filePath,
            listOf(matcher).toKotlinMatchers(),
            DOCType::findLocation
        )
        val count = locations.count { it.entry.matcher::class == matcher::class }
        val kotlinCount = kotlinLocations.count { it.entry.matcher::class == matcher::class }
        assertEquals(20, count)
        assertEquals(20, kotlinCount)
    }
}