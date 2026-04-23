package org.angryscan.app.common

import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.SavedS3Connections
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class SavedS3ConnectionsRepository(
    private val databaseConnector: DatabaseConnector,
    private val secretStore: S3ConnectionSecretStore,
) {
    fun connectionKey(
        endpoint: String,
        bucket: String,
        accessKey: String,
    ): String = buildString {
        append(endpoint.trim().lowercase())
        append("|")
        append(bucket.trim().lowercase())
        append("|")
        append(accessKey.trim().lowercase())
    }

    suspend fun list(): List<SavedS3Connection> =
        databaseConnector.transaction {
            SavedS3Connections
                .selectAll()
                .map {
                    SavedS3Connection(
                        connectionKey = it[SavedS3Connections.connectionKey],
                        name = it[SavedS3Connections.name],
                        endpoint = it[SavedS3Connections.endpoint],
                        bucket = it[SavedS3Connections.bucket],
                        accessKey = it[SavedS3Connections.accessKey],
                    )
                }
        }

    suspend fun upsert(
        name: String,
        endpoint: String,
        bucket: String,
        accessKey: String,
        secretKey: String,
    ): String {
        val normalizedName = name.trim()
        val normalizedEndpoint = endpoint.trim()
        val normalizedBucket = bucket.trim()
        val normalizedAccessKey = accessKey.trim()
        val key = connectionKey(
            endpoint = normalizedEndpoint,
            bucket = normalizedBucket,
            accessKey = normalizedAccessKey
        )

        databaseConnector.transaction {
            val updated = SavedS3Connections.update({ SavedS3Connections.connectionKey eq key }) {
                it[SavedS3Connections.name] = normalizedName
                it[SavedS3Connections.endpoint] = normalizedEndpoint
                it[SavedS3Connections.bucket] = normalizedBucket
                it[SavedS3Connections.accessKey] = normalizedAccessKey
            }
            if (updated == 0) {
                SavedS3Connections.insert {
                    it[connectionKey] = key
                    it[SavedS3Connections.name] = normalizedName
                    it[SavedS3Connections.endpoint] = normalizedEndpoint
                    it[SavedS3Connections.bucket] = normalizedBucket
                    it[SavedS3Connections.accessKey] = normalizedAccessKey
                }
            }
        }

        if (secretKey.isNotEmpty()) {
            secretStore.setSecretKey(key, secretKey)
        }

        return key
    }

    suspend fun remove(connectionKey: String) {
        databaseConnector.transaction {
            SavedS3Connections.deleteWhere { SavedS3Connections.connectionKey eq connectionKey }
        }
        secretStore.deleteSecretKey(connectionKey)
    }

    suspend fun getSecretKey(connectionKey: String): String? =
        secretStore.getSecretKey(connectionKey)
}

