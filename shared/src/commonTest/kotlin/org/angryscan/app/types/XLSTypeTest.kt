package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.XLSType
import kotlin.test.Test

class XLSTypeTest : IKoinTestRule {

    @Test
    fun findLocation() {
        val fileName = "first/first.xls"

        CheckLocation.checkByMap(
            fileName,
            XLSType::findLocation
        )
    }

}