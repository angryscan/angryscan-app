package org.angryscan.app.scan.common.connectors

import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import java.util.concurrent.TimeUnit

/** Shared MongoDB sync driver client factory for [ConnectorMongoDB] and [MongoConnectionValidator]. */
internal fun createMongoClient(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String,
): MongoClient {
    val builder = MongoClientSettings.builder()
        .applyToClusterSettings { it.hosts(listOf(ServerAddress(host, port))) }
        .applyToSocketSettings {
            it.connectTimeout(15_000, TimeUnit.MILLISECONDS)
                .readTimeout(120_000, TimeUnit.MILLISECONDS)
        }
    if (user.isNotBlank()) {
        builder.credential(MongoCredential.createCredential(user, database, password.toCharArray()))
    }
    return MongoClients.create(builder.build())
}
