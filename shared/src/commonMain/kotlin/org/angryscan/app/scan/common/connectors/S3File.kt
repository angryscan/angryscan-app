package org.angryscan.app.scan.common.connectors

data class S3File(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
)
