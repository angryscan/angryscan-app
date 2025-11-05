package org.angryscan.app.scan.common.connectors

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import org.angryscan.app.common.AppVersion
import org.angryscan.app.scan.common.FilesCounter
import java.io.File

private val logger = KotlinLogging.logger {}

@Serializable
class ConnectorHTTP: IConnector, AutoCloseable {

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

        val outputFile = File.createTempFile(
            "ADS_",
            ".txt"
        )

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
        extensions: List<String>,
        fileSelected: (FoundedFile) -> Unit
    ): FilesCounter {
        logger.info { "HTTP scan page: $dir" }
        val response = client.get(dir) {
            header("User-Agent", "DataScanner/${AppVersion}")
        }

        if (response.status.value in 200..299) {
            fileSelected(FoundedFile(dir, 0L))
        } else {
            throw FailedToLoadHTTP()
        }

        return FilesCounter()
    }

    class FailedToLoadHTTP: Exception("Failed to load page")

    override fun close() {
        client.close()
    }

    override fun toString(): String {
        return "ConnectorHTTP"
    }
}