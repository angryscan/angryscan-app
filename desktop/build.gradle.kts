import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
    alias(libs.plugins.conveyor)
    id("io.github.kdroidfilter.compose.linux.packagedeps").version("0.2.5")
}

kotlin {
    jvm("desktop")
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    sourceSets {
        @Suppress("unused")
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.logging.oshai)
                implementation(libs.logging.logback)
                implementation(libs.logging.log4j.core)

                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.network)

                implementation(libs.clikt.full)
                implementation(libs.clikt.markdown)
                implementation(libs.mordant.coroutines)
                implementation(libs.mordant)
                implementation(libs.jansi)
            }
        }
        @Suppress("unused")
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(libs.koin.core)
                implementation(libs.koin.test.junit4)

                implementation(kotlin("test"))

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

dependencies {
    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
}

compose.desktop {
    application {
        mainClass = "org.angryscan.app.MainKt"

        jvmArgs += listOf(
            "-Xmx8g",
            "-Dsun.stdout.encoding=UTF-8",
            "-Dsun.stderr.encoding=UTF-8",
            "-Dfile.encoding=UTF-8"
        )

        buildTypes.release.proguard {
            version.set("7.4.1")
            isEnabled.set(false)
            configurationFiles.from(project.file("compose-desktop.pro"))
            obfuscate.set(true)
        }

        nativeDistributions {
            packageName = "AngryDataScanner"
            packageVersion = version.toString()
            copyright = "Open Source Software, 2025"
            licenseFile.set(rootProject.file("LICENSE"))

            modules("java.sql", "jdk.charsets", "jdk.unsupported", "java.naming")

            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Dmg
            )

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows {
                menuGroup = "start-menu-group"
                installationPath = "AngryDataScanner"
                upgradeUuid = "baf17015-b8d3-4b88-9a59-0031a7b53b34"
                iconFile.set(project(":shared").projectDir.resolve("src\\desktopMain\\composeResources\\files\\icon.ico"))
                console = true
            }
            linux {
                debMaintainer = "soulofpain.k@gmail.com"
                menuGroup = "Security"
                appCategory = "Utility"
                installationPath = "/opt"
                iconFile.set(project(":shared").projectDir.resolve("src\\desktopMain\\composeResources\\files\\icon.png"))
                modules("jdk.security.auth")
            }
            macOS {
                iconFile.set(project(":shared").projectDir.resolve("src\\desktopMain\\composeResources\\files\\icon.icns"))
                bundleID = "org.angryscan.app"
                appCategory = "public.app-category.utilities"
            }
        }
    }
}

tasks.register<Exec>("conveyUnix") {
    val dir = layout.buildDirectory.dir("packages")
    outputs.dir(dir)
    environment["CONVEYOR_AGREE_TO_LICENSE"] = "1"
    commandLine("conveyor","-f", "conveyor.unix.conf", "make", "--output-dir", dir.get(), "site")
    dependsOn("fixConveyorConfig")
}
tasks.register<Exec>("conveyWindows") {
    val dir = layout.buildDirectory.dir("packages")
    outputs.dir(dir)
    environment["CONVEYOR_AGREE_TO_LICENSE"] = "1"
    commandLine("conveyor", "-f", "conveyor.win.conf", "make", "--output-dir", dir.get(), "site")
    dependsOn("fixConveyorConfig")
}

tasks.register<Exec>("conveyUnixCI") {
    val dir = layout.buildDirectory.dir("packages")
    outputs.dir(dir)
    environment["CONVEYOR_AGREE_TO_LICENSE"] = "1"
    commandLine("conveyor", "-f", "ci.conveyor.unix.conf", "make", "--output-dir", dir.get(),  "site")
    dependsOn("fixConveyorConfig")
}

tasks.register<Exec>("conveyWindowsCI") {
    val dir = layout.buildDirectory.dir("packages")
    outputs.dir(dir)
    environment["CONVEYOR_AGREE_TO_LICENSE"] = "1"
    commandLine("conveyor", "-f", "ci.conveyor.win.conf", "make", "--output-dir", dir.get(),  "site")
    dependsOn("fixConveyorConfig")
}

tasks.register("fixConveyorConfig") {
    val inputFile = file("generated.conveyor.conf")
    val tempFile = layout.buildDirectory.file("generated.conveyor.tmp.conf").get().asFile
    doLast {
        if (!inputFile.exists()) {
            println("generated.conveyor.conf not found")
            return@doLast
        }
        val winBuild = System.getenv("TARGET_OS") == "windows"

        tempFile.bufferedWriter().use { writer ->
            inputFile.forEachLine { line ->
                // оставляем все строки, кроме android/ios
                if (
                    !line.contains("-android-") &&
                    !line.contains("-ios-") &&
                    ((!winBuild && !line.contains("-windows-")) ||
                    (winBuild && !line.contains("-linux-") && !line.contains("-macosx-")))
                    ) {
                    writer.write(line)
                    writer.newLine()
                }
            }
        }

        // заменяем оригинальный файл
        inputFile.delete()
        tempFile.renameTo(inputFile)
        println("Removed android/ios entries from generated.conveyor.conf")
    }
    dependsOn("build", "writeConveyorConfig")
}

tasks.register("printVersion") {
    doLast {
        print(version)
    }
}

configurations.all {
    attributes {
        // https://github.com/JetBrains/compose-jb/issues/1404#issuecomment-1146894731
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

linuxDebConfig {
    // add dependencies to DEBIAN/control
    debDepends.set(listOf("libqt5widgets5t64", "libx11-6"))

    // if you want to add dependencies with alternatives for compatibility with older OSes, add them like this:
    debDepends.set(
        listOf(
            "libqt5core5t64 | libqt5core5a",
            "libqt5gui5t64 | libqt5gui5",
            "libqt5widgets5t64 | libqt5widgets5",
        )
    )
    // set StartupWMClass to fix dock/taskbar icon
    startupWMClass.set("org-angryscan-app-MainKt")

    //for Ubuntu 24 t64 dependencies compatibility with older OSes, see below Under Known jpackage issue: Ubuntu t64 transition
    enableT64AlternativeDeps.set(true)
}