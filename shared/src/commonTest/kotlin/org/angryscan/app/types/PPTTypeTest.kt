package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.PPTType
import kotlin.test.Test

class PPTTypeTest : IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.ppt"

        CheckLocation.checkByMap(
            fileName,
            PPTType::findLocation
        )
    }
}