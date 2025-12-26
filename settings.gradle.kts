rootProject.name = "AngryDataScanner"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.hq.hydraulic.software")
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")

            content {
                includeModule("org.angryscan", "gitleaks")
                includeModule("org.angryscan", "gitleaks-jvm")
            }
        }
    }
}

include(":shared")
include(":desktop")