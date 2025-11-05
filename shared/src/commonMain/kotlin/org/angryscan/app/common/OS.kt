package org.angryscan.app.common

enum class OS {
    WINDOWS,
    LINUX,
    MAC,
    OTHER;

    companion object {
        fun currentOS(): OS {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("win") -> {
                    WINDOWS
                }
                os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
                    LINUX
                }
                os.contains("mac") -> {
                    MAC
                }
                else -> OTHER
            }
        }
    }
}