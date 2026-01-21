package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.app.scan.common.files.types.DOCXType
import org.angryscan.app.scan.engine.toKotlinMatchers
import org.angryscan.common.matchers.CardNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DOCXTypeTest: IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.docx"

        CheckLocation.checkByMap(
            fileName,
            DOCXType::findLocation
        )
    }

    @Test
    fun maskLocation() {
        val fileName = "first/first.docx"

        CheckLocation.maskLocations(
            fileName,
            DOCXType::findLocation,
            DOCXType::maskLocations
        )
    }

    @Test
    fun findLocationsInCardDocx() {
        val fileName = "cardNumber/card.docx"
        val filePath = javaClass.getResource("/files/$fileName")?.file
        assertNotNull(filePath)

        val matcher = CardNumber()
        val locations = CheckLocation.getHyperLocations(
            filePath,
            listOf(matcher).toHyperScanMatchers(),
            DOCXType::findLocation
        )
        val count = locations.count { it.entry.matcher::class == matcher::class }
        assertEquals(20, count)

        val kotlinLocations = CheckLocation.getKotlinLocations(
            filePath,
            listOf(matcher).toKotlinMatchers(),
            DOCXType::findLocation
        )
        val kotlinCount = kotlinLocations.count { it.entry.matcher::class == matcher::class }
        assertEquals(20, kotlinCount)
    }
}