package org.angryscan.app.common

interface SqlConnectionSecretStore {
    suspend fun setPassword(connectionKey: String, password: String)
    suspend fun getPassword(connectionKey: String): String?
    suspend fun deletePassword(connectionKey: String)
}

