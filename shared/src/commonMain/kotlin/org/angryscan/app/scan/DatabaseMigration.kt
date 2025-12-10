package org.angryscan.app.scan

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.TaskFileExtensions
import org.angryscan.app.db.models.TaskFileScanResults
import org.angryscan.app.db.models.TaskFiles
import org.angryscan.app.db.models.TaskMatchers
import org.angryscan.app.db.models.Tasks
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.sql.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.getValue
import kotlin.io.path.absolutePathString

object DatabaseMigration: KoinComponent {
    val logger = KotlinLogging.logger {}

    private val appSettings: AppSettings by inject()
    private val database: DatabaseConnector by inject()

    @OptIn(ExperimentalDatabaseMigrationApi::class)
    fun migrate() {
        var migrationRequired = false
        if (File(
                AppFiles
                    .MigrationsDirectory
                    .resolve("V2__AddMissingColumns.sql")
                    .absolutePathString()
            ).exists()
        ) {
            File(AppFiles.MigrationsDirectory.absolutePathString()).listFiles()?.forEach {
                it.delete()
            }
            appSettings.firstMigration.value = true
            appSettings.save()
        }

        transaction(database.connection) {
            SchemaUtils.create(
                Tasks,
                TaskFiles,
                TaskFileExtensions,
                TaskMatchers,
                TaskFileScanResults
            )


            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(Tasks, withLogs = false)
            if (statements.isNotEmpty()) {
                logger.info {
                    "Database migration required."
                }
                migrationRequired = true
                if (!File(AppFiles.MigrationsDirectory.absolutePathString()).exists()) {
                    File(AppFiles.MigrationsDirectory.absolutePathString()).mkdir()
                }

                MigrationUtils.generateMigrationScript(
                    Tasks,
                    scriptDirectory = AppFiles.MigrationsDirectory.absolutePathString(),
                    scriptName = "V2__FirstMigration",
                )
            }
        }

        if (migrationRequired) {
            val flyway = Flyway.configure()
                .dataSource(database.dbSettings.url, "", "")
                .defaultSchema("main")
                .schemas("main")
                .locations("filesystem:${AppFiles.MigrationsDirectory}")
                .baselineOnMigrate(appSettings.firstMigration.value)
                .load()
            try {
                flyway.validate()

                val m = runBlocking {
                    database.transaction {
                        flyway.migrate()
                    }
                }

                if (m.success && m.successfulMigrations.isNotEmpty()) {
                    appSettings.firstMigration.value = false
                    appSettings.save()
                    logger.info {
                        "Database migration completed."
                    }
                } else {
                    logger.error {
                        "Database migration failed."
                    }
                }
            } catch (_: FlywayValidateException) {

            }
        }
    }
}