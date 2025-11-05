package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.TextType
import kotlin.test.Test

class TextTypeTest : IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.csv"

        CheckLocation.checkByMap(
            fileName,
            TextType::findLocation
        )
    }
}