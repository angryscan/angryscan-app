package org.angryscan.app.common

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class DesktopS3ConnectionSecretStore : S3ConnectionSecretStore {
    private val logger = KotlinLogging.logger {}
    private val macService = "org.angryscan.saved-s3-connections"
    private val linuxService = "org.angryscan.saved-s3-connections"
    private val windowsSecretsFile: File = AppFiles.WorkDir.resolve("s3-secrets-windows.properties")
    private val fallbackFile: File = AppFiles.WorkDir.resolve("s3-secrets.properties")

    override suspend fun setSecretKey(connectionKey: String, secretKey: String) = withContext(Dispatchers.IO) {
        if (secretKey.isBlank()) return@withContext
        when (OS.currentOS()) {
            OS.MAC -> if (tryMacSet(connectionKey, secretKey)) return@withContext
            OS.WINDOWS -> if (tryWindowsSet(connectionKey, secretKey)) return@withContext
            OS.LINUX -> if (tryLinuxSet(connectionKey, secretKey)) return@withContext
            else -> Unit
        }
        setFallback(connectionKey, secretKey)
    }

    override suspend fun getSecretKey(connectionKey: String): String? = withContext(Dispatchers.IO) {
        when (OS.currentOS()) {
            OS.MAC -> tryMacGet(connectionKey)?.let { return@withContext it }
            OS.WINDOWS -> tryWindowsGet(connectionKey)?.let { return@withContext it }
            OS.LINUX -> tryLinuxGet(connectionKey)?.let { return@withContext it }
            else -> Unit
        }
        getFallback(connectionKey)
    }

    override suspend fun deleteSecretKey(connectionKey: String) = withContext(Dispatchers.IO) {
        when (OS.currentOS()) {
            OS.MAC -> tryMacDelete(connectionKey)
            OS.WINDOWS -> tryWindowsDelete(connectionKey)
            OS.LINUX -> tryLinuxDelete(connectionKey)
            else -> Unit
        }
        deleteFallback(connectionKey)
    }

    private fun tryMacSet(connectionKey: String, secret: String): Boolean {
        return runCommand(
            listOf(
                "security",
                "add-generic-password",
                "-U",
                "-s",
                macService,
                "-a",
                connectionKey,
                "-w",
                secret
            )
        ).isSuccess
    }

    private fun tryMacGet(connectionKey: String): String? {
        val result = runCommand(
            listOf(
                "security",
                "find-generic-password",
                "-s",
                macService,
                "-a",
                connectionKey,
                "-w"
            )
        )
        return if (result.isSuccess) result.stdout.trim().ifBlank { null } else null
    }

    private fun tryMacDelete(connectionKey: String): Boolean {
        val result = runCommand(
            listOf(
                "security",
                "delete-generic-password",
                "-s",
                macService,
                "-a",
                connectionKey
            )
        )
        return result.isSuccess
    }

    private fun tryLinuxSet(connectionKey: String, secret: String): Boolean {
        val result = runCommand(
            command = listOf(
                "secret-tool",
                "store",
                "--label=AngryDataScanner S3 Connection",
                "service",
                linuxService,
                "account",
                connectionKey
            ),
            stdin = secret
        )
        return result.isSuccess
    }

    private fun tryLinuxGet(connectionKey: String): String? {
        val result = runCommand(
            command = listOf(
                "secret-tool",
                "lookup",
                "service",
                linuxService,
                "account",
                connectionKey
            )
        )
        return if (result.isSuccess) result.stdout.trim().ifBlank { null } else null
    }

    private fun tryLinuxDelete(connectionKey: String): Boolean {
        val result = runCommand(
            command = listOf(
                "secret-tool",
                "clear",
                "service",
                linuxService,
                "account",
                connectionKey
            )
        )
        return result.isSuccess
    }

    private fun tryWindowsSet(connectionKey: String, secret: String): Boolean {
        val encrypted = runPowerShell(
            """
            ${'$'}sec = ConvertTo-SecureString -String '${psEscape(secret)}' -AsPlainText -Force
            ConvertFrom-SecureString -SecureString ${'$'}sec
            """.trimIndent()
        ).takeIf { it.isSuccess }?.stdout?.trim().orEmpty()
        if (encrypted.isBlank()) return false

        val props = loadProperties(windowsSecretsFile)
        props.setProperty(connectionKey, encrypted)
        storeProperties(windowsSecretsFile, props, "ADS S3 connection secrets (Windows DPAPI)")
        return true
    }

    private fun tryWindowsGet(connectionKey: String): String? {
        val encrypted = loadProperties(windowsSecretsFile).getProperty(connectionKey)?.trim().orEmpty()
        if (encrypted.isBlank()) return null
        val result = runPowerShell(
            """
            ${'$'}sec = ConvertTo-SecureString '${psEscape(encrypted)}'
            ${'$'}ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR(${'$'}sec)
            [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(${'$'}ptr)
            """.trimIndent()
        )
        return if (result.isSuccess) result.stdout.trim().ifBlank { null } else null
    }

    private fun tryWindowsDelete(connectionKey: String): Boolean {
        val props = loadProperties(windowsSecretsFile)
        val existed = props.remove(connectionKey) != null
        if (existed) {
            storeProperties(windowsSecretsFile, props, "ADS S3 connection secrets (Windows DPAPI)")
        }
        return existed
    }

    private fun setFallback(connectionKey: String, secret: String) {
        val props = loadProperties(fallbackFile)
        props.setProperty(connectionKey, secret)
        storeProperties(fallbackFile, props, "ADS S3 connection secrets (fallback store)")
    }

    private fun getFallback(connectionKey: String): String? =
        loadProperties(fallbackFile).getProperty(connectionKey)

    private fun deleteFallback(connectionKey: String) {
        val props = loadProperties(fallbackFile)
        props.remove(connectionKey)
        storeProperties(fallbackFile, props, "ADS S3 connection secrets (fallback store)")
    }

    private fun loadProperties(file: File): Properties {
        val props = Properties()
        if (file.exists()) {
            runCatching {
                file.inputStream().use(props::load)
            }.onFailure {
                logger.warn { "Failed to read fallback s3 secrets: ${it.message}" }
            }
        }
        return props
    }

    private fun storeProperties(file: File, props: Properties, comment: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.outputStream().use { out ->
                props.store(out, comment)
            }
        }.onFailure {
            logger.error(it) { "Failed to write s3 secrets file: ${file.name}" }
        }
    }

    private fun runPowerShell(script: String): CommandResult {
        return runCommand(
            command = listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
            )
        )
    }

    private fun psEscape(value: String): String = value.replace("'", "''")

    private data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    private fun runCommand(command: List<String>, stdin: String? = null): CommandResult {
        return runCatching {
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

            stdin?.let {
                process.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                    writer.write(it)
                }
            }

            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()

            process.inputStream.copyTo(stdout)
            process.errorStream.copyTo(stderr)
            val exitCode = process.waitFor()

            CommandResult(
                exitCode = exitCode,
                stdout = stdout.toString(StandardCharsets.UTF_8),
                stderr = stderr.toString(StandardCharsets.UTF_8)
            )
        }.getOrElse { ex ->
            logger.warn { "Secret command failed (${command.firstOrNull()}): ${ex.message}" }
            CommandResult(exitCode = -1, stdout = "", stderr = ex.message.orEmpty())
        }
    }
}

