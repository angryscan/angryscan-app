package org.angryscan.app.scan

import MigrationUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.flywaydb.core.internal.exception.FlywayMigrateException
import org.jetbrains.exposed.sql.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.sql.DriverManager
import kotlin.io.path.absolutePathString
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object DatabaseMigration: KoinComponent {
    val logger = KotlinLogging.logger {}

    private val appSettings: AppSettings by inject()
    private val database: DatabaseConnector by inject()

    /**
     * Generates a unique migration script version so that migrations apply correctly
     * even when the user skips app versions. Uses timestamp (yyyyMMddHHmmss) so each
     * generated migration gets a monotonic version and Flyway applies them in order.
     */
    @OptIn(ExperimentalTime::class)
    private fun nextMigrationScriptName(): String {
        return migrationScriptNameFromInstantString(Clock.System.now().toString())
    }

    internal fun migrationScriptNameFromInstantString(instantString: String): String {
        return "V${migrationVersionFromInstantString(instantString)}__SchemaSync"
    }

    internal fun migrationVersionFromInstantString(instantString: String): String {
        val match = ISO_INSTANT_VERSION_REGEX.find(instantString)
            ?: throw IllegalArgumentException("Unexpected instant format: $instantString")

        return buildString {
            append(match.groupValues[1])
            append(match.groupValues[2])
            append(match.groupValues[3])
            append(match.groupValues[4])
            append(match.groupValues[5])
            append(match.groupValues[6])
        }
    }

    @OptIn(ExperimentalDatabaseMigrationApi::class)
    fun migrate() {
        var migrationRequired = false

        transaction(database.connection) {
            SchemaUtils.create(
                Tasks,
                TaskFiles,
                TaskFileExtensions,
                TaskMatchers,
                TaskFileScanResults,
                SavedSqlConnections
            )


            val allTables = arrayOf(
                Tasks,
                TaskFiles,
                TaskFileExtensions,
                TaskMatchers,
                TaskFileScanResults,
                SavedSqlConnections
            )
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*allTables, withLogs = false)
            if (statements.isNotEmpty()) {
                logger.info {
                    "Database migration required."
                }
                migrationRequired = true
                if (!File(AppFiles.MigrationsDirectory.absolutePathString()).exists()) {
                    File(AppFiles.MigrationsDirectory.absolutePathString()).mkdir()
                }

                val scriptName = nextMigrationScriptName()
                MigrationUtils.generateMigrationScript(
                    *allTables,
                    scriptDirectory = AppFiles.MigrationsDirectory.absolutePathString(),
                    scriptName = scriptName,
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
                runMigrate(flyway)
                flyway.validate()
            } catch (e: FlywayValidateException) {
                logger.warn { "Flyway validation failed (migrations may have been applied): ${e.message}" }
            }
        }
    }

    /**
     * Runs Flyway migrate. If a migration fails with "duplicate column" (or "already exists"),
     * the schema change was already applied (e.g. by a previous run or SchemaUtils); we mark
     * that migration as applied in flyway_schema_history and retry so the app can start.
     */
    private fun runMigrate(flyway: Flyway) {
        try {
            val m = runBlocking {
                database.transaction {
                    flyway.migrate()
                }
            }
            if (m.success && m.successfulMigrations.isNotEmpty()) {
                appSettings.firstMigration.value = false
                appSettings.save()
                logger.info { "Database migration completed." }
            } else if (!m.success) {
                logger.error { "Database migration failed." }
            }
        } catch (e: FlywayMigrateException) {
            if (!isAlreadyAppliedError(e)) throw e
            val scriptName = parseFailedScriptName(e) ?: run {
                logger.error { "Cannot parse failed migration script name: ${e.message}" }
                throw e
            }
            logger.warn { "Migration $scriptName failed (column already exists); marking as applied and retrying." }
            markMigrationAsApplied(scriptName)
            runMigrate(flyway)
        }
    }

    private fun isAlreadyAppliedError(e: FlywayMigrateException): Boolean {
        var cause: Throwable? = e.cause
        while (cause != null) {
            val msg = cause.message ?: ""
            if (msg.contains("duplicate column", ignoreCase = true) ||
                msg.contains("already exists", ignoreCase = true)
            ) return true
            cause = cause.cause
        }
        return false
    }

    /** Parses script filename from exception message, e.g. "Script V3__AddColumnName.sql failed" -> "V3__AddColumnName.sql" */
    private fun parseFailedScriptName(e: FlywayMigrateException): String? {
        val msg = e.message ?: return null
        val prefix = "Script "
        val suffix = " failed"
        val start = msg.indexOf(prefix)
        if (start < 0) return null
        val from = start + prefix.length
        val end = msg.indexOf(suffix, from)
        if (end < 0) return null
        return msg.substring(from, end).trim()
    }

    /**
     * Inserts a row into flyway_schema_history so Flyway considers this migration applied.
     * Used when a migration fails with "duplicate column" because the change was already applied.
     */
    private fun markMigrationAsApplied(scriptName: String) {
        // Script name format: V<version>__<description>.sql
        val match = Regex("^V(.+?)__(.+)\\.sql$").find(scriptName)
            ?: throw IllegalArgumentException("Unexpected script name format: $scriptName")
        val version = match.groupValues[1]
        val description = match.groupValues[2]
        DriverManager.getConnection(database.dbSettings.url).use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
                SELECT COALESCE(MAX(installed_rank), 0) + 1, ?, ?, 'SQL', ?, NULL, '', datetime('now'), 0, 1
                FROM flyway_schema_history
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, version)
                ps.setString(2, description)
                ps.setString(3, scriptName)
                ps.executeUpdate()
            }
        }
    }

    private val ISO_INSTANT_VERSION_REGEX =
        Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})""")
}