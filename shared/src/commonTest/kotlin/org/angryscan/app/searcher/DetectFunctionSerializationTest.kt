package org.angryscan.app.searcher

import kotlinx.serialization.json.Json
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.extensions.Matchers
import org.angryscan.common.matchers.FullName
import org.angryscan.app.serializers.PolymorphicFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

internal class DetectFunctionSerializationTest {
    @Test
    fun `Mutable list serialization`() {
        val list: MutableList<IMatcher> = Matchers.toMutableList()
        val serialized = PolymorphicFormatter.encodeToString(list)
        val decoded: MutableList<IMatcher> = PolymorphicFormatter.decodeFromString(serialized)
        assertEquals(
            list.map { it::class },
            decoded.map { it::class }
        )
    }

    @Test
    fun `Single detect function test`() {
        val df: IMatcher = FullName
        val serialized = PolymorphicFormatter.encodeToString(df)
        assertEquals(df, PolymorphicFormatter.decodeFromString(serialized))
    }

    @Test
    fun `Standart serialization fails`() {
        val df: IMatcher = FullName
        val formatter = Json { prettyPrint = false }
        assertFails {
            formatter.encodeToString(df)
        }

    }
}