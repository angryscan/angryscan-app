package org.angryscan.app.scan.functions.rkn

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.angryscan.app.common.AppFiles
import org.angryscan.app.resources.Res

@OptIn(ExperimentalResourceApi::class)
class DomainRepository {
    private var blockedDomains: List<String>
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    init {
        val json = Json {
            ignoreUnknownKeys = true
        }
        runBlocking {
            try{
                updateBlockedDomains()
                blockedDomains = json.decodeFromString<List<String>>(
                    AppFiles.BlockedDomainsCache.readText()
                )
            } catch (_: Exception) {
                if(AppFiles.BlockedDomainsCache.exists()) {
                    blockedDomains = json.decodeFromString<List<String>>(
                        AppFiles.BlockedDomainsCache.readText()
                    )
                } else {
                    blockedDomains = json.decodeFromString<List<String>>(
                        runBlocking {
                            Res.readBytes("files/rkn.json").decodeToString()
                        }
                    )
                }
            }
        }
    }

    fun checkDomain(domain: String): Boolean {
        val d = domain.replace("^www.".toRegex(), "")
        blockedDomains.forEach { bd ->
            if(bd.endsWith(d))
                return true
        }
        return false
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun updateBlockedDomains() {
        try {
            val client = HttpClient(CIO)
            val response = client.get("https://reestr.rublacklist.net/api/v3/domains/")
            if (response.status.value == 200) {
                response.bodyAsChannel().copyAndClose(
                    AppFiles.BlockedDomainsCache.writeChannel()
                )
            } else {
                AppFiles.BlockedDomainsCache.delete()
                throw FailRepositoryUpdate()
            }
        } catch (_: Exception) {
            AppFiles.BlockedDomainsCache.writeBytes(
                Res.readBytes("files/rkn.json")
            )
        }
    }

}

class FailRepositoryUpdate(): Exception("Fail to update RKN blacklist")