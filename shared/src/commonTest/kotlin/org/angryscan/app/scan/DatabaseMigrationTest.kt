package org.angryscan.app.scan

import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationTest {
    @Test
    fun buildsMigrationVersionFromIsoInstant() {
        val version = DatabaseMigration.migrationVersionFromInstantString("2026-03-13T14:44:30.123456789Z")

        assertEquals("20260313144430", version)
    }

    @Test
    fun buildsMigrationScriptName() {
        val scriptName = DatabaseMigration.migrationScriptNameFromInstantString("2026-03-13T14:44:30Z")

        assertEquals("V20260313144430__SchemaSync", scriptName)
    }
}
