package org.angryscan.app.db.models

import org.jetbrains.exposed.sql.Table

object SavedS3Connections : Table("saved_s3_connections") {
    val connectionKey = text("connection_key").uniqueIndex()
    val name = text("name")
    val endpoint = text("endpoint")
    val bucket = text("bucket")
    val accessKey = text("access_key")
}

