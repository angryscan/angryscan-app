package org.angryscan.app.console.extensions

import com.sun.jna.Library
import com.sun.jna.Native
import org.fusesource.jansi.internal.Kernel32.SetConsoleOutputCP

private interface Kernel32 : Library {
    fun FreeConsole(): Boolean

    companion object {
        val INSTANCE: Kernel32 =
            Native.load("kernel32", Kernel32::class.java)
    }
}

object WindowsCLI {
    fun setup() {
        SetConsoleOutputCP(65001)
    }
    fun freeConsole() {
        Kernel32.INSTANCE.FreeConsole()
    }
}
