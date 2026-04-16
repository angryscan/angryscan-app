package org.angryscan.app.scan

import org.angryscan.app.scan.common.FileSize
import org.angryscan.common.matchers.Email
import org.angryscan.common.matchers.FullName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class SqlResultCardsTest {
    @Test
    fun `groupSqlResultsByTable groups rows into table cards and aggregates columns`() {
        val results = listOf(
            TaskFileResult(
                id = 1,
                path = "public.users",
                size = FileSize(512),
                foundAttributes = mapOf(Email to 2),
                count = 2,
                score = 20,
                columnName = "email"
            ),
            TaskFileResult(
                id = 1,
                path = "public.users",
                size = FileSize(512),
                foundAttributes = mapOf(FullName to 1),
                count = 1,
                score = 5,
                columnName = "email"
            ),
            TaskFileResult(
                id = 1,
                path = "public.users",
                size = FileSize(512),
                foundAttributes = mapOf(FullName to 3),
                count = 3,
                score = 9,
                columnName = "full_name"
            ),
            TaskFileResult(
                id = 2,
                path = "sales.orders",
                size = FileSize(256),
                foundAttributes = mapOf(Email to 1),
                count = 1,
                score = 30,
                columnName = "card_number"
            )
        )

        val cards = groupSqlResultsByTable(results)

        assertEquals(2, cards.size)
        assertEquals("public.users", cards[0].tablePath)
        assertEquals("sales.orders", cards[1].tablePath)

        val usersCard = cards[0]
        assertEquals("public", usersCard.schemaName)
        assertEquals("users", usersCard.tableName)
        assertEquals(6, usersCard.count)
        assertEquals(34, usersCard.score)
        assertEquals(2, usersCard.columns.size)
        assertEquals(0, usersCard.size.compareTo(FileSize(512)))
        assertEquals(2, usersCard.foundAttributes.size)
        assertEquals(2, usersCard.foundAttributes[Email])
        assertEquals(4, usersCard.foundAttributes[FullName])

        val emailColumn = usersCard.columns.first()
        assertEquals("email", emailColumn.name)
        assertEquals(3, emailColumn.count)
        assertEquals(25, emailColumn.score)
        assertEquals(2, emailColumn.foundAttributes.size)
        assertEquals(2, emailColumn.foundAttributes[Email])
        assertEquals(1, emailColumn.foundAttributes[FullName])
    }

    @Test
    fun `groupSqlResultsByTable handles table path without schema`() {
        val results = listOf(
            TaskFileResult(
                id = 1,
                path = "users",
                size = FileSize(128),
                foundAttributes = mapOf(Email to 1),
                count = 1,
                score = 10,
                columnName = "email"
            )
        )

        val cards = groupSqlResultsByTable(results)

        assertEquals(1, cards.size)
        assertNull(cards.first().schemaName)
        assertEquals("users", cards.first().tableName)
        assertTrue(cards.first().columns.isNotEmpty())
    }
}
