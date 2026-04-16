package org.angryscan.app.scan.common.connectors

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.common.AppVersion
import org.angryscan.app.scan.common.ObjectCounter
import org.angryscan.app.scan.common.files.types.IFileType
import java.io.File

private val logger = KotlinLogging.logger {}

@Serializable
class ConnectorHTTP: IFileConnector, AutoCloseable {

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
            }

//            install(HttpSend) {
//                maxSendCount = 30
//            }
            followRedirects = false
        }
    }

    override suspend fun getFile(filePath: String): File {
        val response = client.get(filePath) {
            header("User-Agent", "DataScanner/${AppVersion}")
        }

        val outputFile = withContext(Dispatchers.IO) {
            File.createTempFile(
                "ADS_",
                ".txt"
            )
        }

        if (response.status.value in 200..299) {
            outputFile.writeBytes(response.body())
            return outputFile
        } else {
            logger.error { "Error downloading page" }
            throw FailedToLoadHTTP()
        }
    }

    override suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (FoundedFile) -> Unit
    ): ObjectCounter {
        logger.info { "HTTP scan page: $dir" }
        val response = client.get(dir) {
            header("User-Agent", "DataScanner/${AppVersion}")
        }

        if (response.status.value in 200..299) {
            fileSelected(FoundedFile(dir, 0L))
        } else {
            throw FailedToLoadHTTP()
        }

        return ObjectCounter()
    }

    class FailedToLoadHTTP: Exception("Failed to load page")

    override fun close() {
        client.close()
    }

    override fun toString(): String {
        return "ConnectorHTTP"
    }
}