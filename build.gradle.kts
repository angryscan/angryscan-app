plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.kotlin.jpa).apply(false)
    alias(libs.plugins.compose).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.kover)
    alias(libs.plugins.conveyor).apply(false)
}

val dummyAttribute = Attribute.of("org.angryscan", String::class.java)

group = "org.angryscan"
version = System.getenv("VERSION") ?: "1.5.0"

subprojects {
    group = rootProject.group
    version = rootProject.version


}

dependencies {
    kover(project(":shared"))
    kover(project(":desktop"))
}

tasks.register("getBranch") {
    println(gitBranch)
}
tasks.register("testFiles") {
    println(layout.buildDirectory.file("reports/kover/report.xml").isPresent)
}

fun String.runCommand(currentWorkingDir: File = file("./")): String {
    val process = ProcessBuilder(this.split("\\s+".toRegex()))
        .directory(currentWorkingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()

    return process.inputStream.bufferedReader().use { it.readText() }
}

val gitBranch = System.getProperty("GIT_BRANCH") ?: "git rev-parse --abbrev-ref HEAD".runCommand()

kover.reports {
    filters {
        includes.classes("*")
    }
}
