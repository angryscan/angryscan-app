package org.angryscan.app.scan.common.connectors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ParseMongoCollectionPathTest {

    @Test
    fun `first dot separates database from collection rest may contain dots`() {
        assertEquals("testdb" to "contacts.v2", parseMongoCollectionPath("testdb.contacts.v2"))
    }

    @Test
    fun `simple two segment path`() {
        assertEquals("app" to "users", parseMongoCollectionPath("app.users"))
    }

    @Test
    fun `invalid path throws`() {
        assertFailsWith<IllegalArgumentException> { parseMongoCollectionPath("nodot") }
        assertFailsWith<IllegalArgumentException> { parseMongoCollectionPath(".onlysuffix") }
    }
}
