package org.angryscan.app.common

data class SavedS3Connection(
    val connectionKey: String,
    val name: String,
    val endpoint: String,
    val bucket: String,
    val accessKey: String,
)

