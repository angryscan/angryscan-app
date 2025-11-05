package org.angryscan.app.common

val AppVersion =
    System.getProperty("jpackage.app-version") ?:
    System.getProperty("app.version") ?:
    "Debug"