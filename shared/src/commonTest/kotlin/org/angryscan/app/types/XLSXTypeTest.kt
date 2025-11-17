package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.XLSXType
import kotlin.test.Test


class XLSXTypeTest : IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.xlsx"

        CheckLocation.checkByMap(
            fileName,
            XLSXType::findLocation
        )
    }

    @Test
    fun maskLocation() {
        val fileName = "first/first.xlsx"

        CheckLocation.maskLocations(
            fileName,
            XLSXType::findLocation,
            XLSXType::maskLocations
        )
    }
}