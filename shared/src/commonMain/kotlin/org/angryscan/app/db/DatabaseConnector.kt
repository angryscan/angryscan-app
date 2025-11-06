package org.angryscan.app.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.sql.Connection

class DatabaseConnector() : KoinComponent {
    val dbSettings: DatabaseSettings by inject()

    val connection: Database = Database.connect(
        url = dbSettings.url,
        driver = dbSettings.driver
    )

    private val context = Dispatchers.IO.limitedParallelism(1)

    suspend fun <T> transaction(block: suspend () -> T): T =
        newSuspendedTransaction(context) { block() }

    init {
        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE
    }
}
