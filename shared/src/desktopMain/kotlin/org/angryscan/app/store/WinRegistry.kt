package org.angryscan.app.store

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger{}

/**
 * Provides access to the Windows Registry.
 *
 * @author Thomas Kuenneth
 */
object WinRegistry {

    /**
     * Gets an entry from the Windows registry.
     *
     * @param key the key, for example
     * `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param value the value, for example `AppsUseLightTheme`
     * @param type the type, for example `REG_DWORD`
     * @return the result or null on error
     */
    fun getWindowsRegistryEntry(
        key: String?,
        value: String?,
        type: REG_TYPE,
        encoding: Charset = Charset.forName("CP866")
    ): String? {
        val cmd = String.format(
            "reg query \"%s\" /v %s",
            key,
            value
        )
        return cmd.runCommand(encoding)?.also { logger.debug { "Get windows registry entry result: $it" } }?.substringAfter(type.toString())?.trim{ it <= ' '}
    }

    /**
     * Gets a `REG_DWORD` from the Windows registry.
     * `NumberFormatException` may be thrown if the result cannot be
     * obtained, or if it is not a number.
     *
     * @param key the key, for example
     * `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param value the value, for example `AppsUseLightTheme`
     * @return the result
     */
    @Suppress("unused")
    fun getWindowsRegistryEntry(key: String?, value: String?): Int? {
        return getWindowsRegistryEntry(key, value, REG_TYPE.REG_DWORD)?.toIntOrNull()
    }

    /**
     * Writes a `REG_DWORD` into the Windows registry.
     *
     * @param key the key, for example
     * `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param value the value, for example `AppsUseLightTheme`
     * @param data the data to be written
     * @return if successful `true`, otherwise `false`
     */
    @Suppress("unused")
    fun setWindowsRegistryEntry(
        key: String?,
        value: String?,
        data: Int
    ): Boolean {
        return setWindowsRegistryEntry(
            key, value,
            Integer.toHexString(data),
            REG_TYPE.REG_DWORD
        )
    }

    /**
     * Writes a `REG_DWORD` into the Windows registry.
     *
     * @param key the key, for example
     * `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param value the value, for example `AppsUseLightTheme`
     * @param data the data to be written
     * @param type the type, for example `REG_DWORD`
     * @return if successful `true`, otherwise `false`
     */
    fun setWindowsRegistryEntry(
        key: String?,
        value: String?,
        data: String?,
        type: REG_TYPE,
        encoding: Charset = Charset.forName("CP866")
    ): Boolean {
        return String.format(
            "reg add \"%s\" /v \"%s\" /t %s /d \"%s\" /f",
            key, value, type.toString(), data
        ).runCommand(encoding)?.also { logger.debug { "Set windows registry result: $it" } }?.isNotEmpty() ?: false
    }

    /**
     * Writes a registry entry without a named value into the Windows registry.
     *
     * @param key the registry key path, for example
     * `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param data the data to be written
     * @param type the registry type, for example `REG_SZ`
     * @param encoding the charset encoding to be used for the command execution, default is `CP866`
     * @return `true` if the operation was successful, otherwise `false`
     */
    fun setWindowsRegistryEntry(
        key: String?,
        data: String?,
        type: REG_TYPE,
        encoding: Charset = Charset.forName("CP866")
    ): Boolean {
        return String.format(
            "reg add \"%s\" /ve /t %s /d \"%s\" /f",
            key, type.toString(), data
        ).runCommand(encoding)?.also { logger.debug { "Set windows registry result: $it" } }?.isNotEmpty() ?: false
    }

    /**
     * Deletes a named value from the Windows registry.
     *
     * @param key the key, for example
     * `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param value the value, for example `AppsUseLightTheme`
     * @param type the type, for example `REG_DWORD`
     * @param encoding the charset encoding to be used for the command execution, default is `CP866`
     * @return if successful `true`, otherwise `false`
     */
    @Suppress("unused")
    fun deleteWindowsRegistryEntry(
        key: String?,
        value: String?,
        type: REG_TYPE,
        encoding: Charset = Charset.forName("CP866")
    ): Boolean {
        return String.format(
            "reg delete \"%s\" /v \"%s\" /f",
            key, value, type.toString()
        ).runCommand(encoding)?.also { logger.debug { "Delete windows registry entry result: $it" } }?.isNotEmpty() ?: false
    }
    /**
     * Deletes a registry key.
     *
     * @param key the key, for example `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
     * @param type the type, for example `REG_DWORD`
     * @param encoding the charset encoding to be used for the command execution, default is `CP866`
     * @return if successful `true`, otherwise `false`
     */
    fun deleteWindowsRegistryKey(
        key: String?,
        type: REG_TYPE,
        encoding: Charset = Charset.forName("CP866")
    ): Boolean {
        return String.format(
            "reg delete \"%s\" /f",
            key, type.toString()
        ).runCommand(encoding)?.also { logger.debug { "Delete windows registry key result: $it" } }?.isNotEmpty() ?: false
    }

    /**
     * Registry types
     */
    @Suppress("unused")
    enum class REG_TYPE {
        REG_BINARY, REG_DWORD, REG_EXPAND_SZ, REG_MULTI_SZ, REG_SZ
    }

    /**
     * Executes a command and returns the output.
     *
     * @param encoding the charset encoding to be used for the command execution
     * @return the output of the command, or null on error
     */
    private fun String.runCommand(encoding: Charset): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(5, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader(charset = encoding).readText()
        } catch(e: IOException) {
            logger.error { e.message }
            return null
        }
    }
}