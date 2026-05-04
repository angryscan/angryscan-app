package org.angryscan.app.common

interface S3ConnectionSecretStore {
    suspend fun setSecretKey(connectionKey: String, secretKey: String)
    suspend fun getSecretKey(connectionKey: String): String?
    suspend fun deleteSecretKey(connectionKey: String)
}

