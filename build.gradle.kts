import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.kotlin.jpa).apply(false)
    alias(libs.plugins.compose).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.kover)
    alias(libs.plugins.conveyor).apply(false)
}

/**
 * hive-jdbc standalone embeds SLF4J 1.7.x; if its jar precedes slf4j-api 2.x on the classpath,
 * LoggerFactory loads the shaded 1.x API and falls back to NOP (no StaticLoggerBinder).
 */
fun prioritizeSlf4jBindingsOnClasspath(files: Collection<File>): List<File> {
    val (bindings, rest) = files.partition { file ->
        val name = file.name
        name.startsWith("slf4j-api-") ||
            name.startsWith("logback-classic-") ||
            name.startsWith("logback-core-")
    }
    val orderedBindings = bindings.sortedBy { file ->
        when {
            file.name.startsWith("slf4j-api-") -> 0
            file.name.startsWith("logback-classic-") -> 1
            file.name.startsWith("logback-core-") -> 2
            else -> 3
        }
    }
    return orderedBindings + rest
}

fun slf4jBindingPriority(classpathEntry: String): Int? = when {
    "slf4j-api-" in classpathEntry -> 0
    "logback-classic-" in classpathEntry -> 1
    "logback-core-" in classpathEntry -> 2
    else -> null
}

/** Reorders jar path lines inside Conveyor `app.inputs = [...]` blocks (plugin lists deps alphabetically). */
fun reorderConveyorJarInputBlocks(cfgFile: File) {
    val lines = cfgFile.readLines()
    val result = ArrayList<String>(lines.size)
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        if (trimmed.endsWith("[") && trimmed.contains(".inputs")) {
            result.add(line)
            i++
            val jarLines = ArrayList<String>()
            while (i < lines.size && !lines[i].trim().startsWith("]")) {
                val current = lines[i]
                if (current.trim().endsWith(".jar")) {
                    jarLines.add(current)
                } else {
                    result.add(current)
                }
                i++
            }
            if (jarLines.isNotEmpty()) {
                val indent = jarLines.first().takeWhile(Char::isWhitespace)
                val ordered = jarLines
                    .map { it.trim() }
                    .sortedWith(compareBy({ slf4jBindingPriority(it) ?: 3 }, { it }))
                    .map { "$indent$it" }
                result.addAll(ordered)
            }
            if (i < lines.size) {
                result.add(lines[i])
            }
            i++
        } else {
            result.add(line)
            i++
        }
    }
    cfgFile.writeText(result.joinToString("\n", postfix = "\n"))
}

/** Reorders app.classpath lines in jpackage-generated .cfg (alphabetical order puts hive before slf4j-api). */
fun reorderJpackageAppClasspath(cfgFile: File) {
    val lines = cfgFile.readLines()
    if (lines.none { it.startsWith("app.classpath=") }) return

    val sortedClasspath = lines
        .filter { it.startsWith("app.classpath=") }
        .sortedWith(compareBy({ slf4jBindingPriority(it) ?: 3 }, { it }))

    val result = buildList(lines.size) {
        var classpathWritten = false
        for (line in lines) {
            if (line.startsWith("app.classpath=")) {
                if (!classpathWritten) {
                    addAll(sortedClasspath)
                    classpathWritten = true
                }
            } else {
                add(line)
            }
        }
    }
    cfgFile.writeText(result.joinToString("\n", postfix = "\n"))
}

val dummyAttribute = Attribute.of("org.angryscan", String::class.java)

group = "org.angryscan"
version = System.getenv("VERSION") ?: "1.5.1"

subprojects {
    group = rootProject.group
    version = rootProject.version

    tasks.withType<JavaExec>().configureEach {
        doFirst {
            classpath = files(prioritizeSlf4jBindingsOnClasspath(classpath.files))
        }
    }

    tasks.withType<Test>().configureEach {
        doFirst {
            classpath = files(prioritizeSlf4jBindingsOnClasspath(classpath.files))
        }
    }

    if (name == "desktop") {
        tasks.matching {
            it.name == "createDistributable" || it.name == "createReleaseDistributable"
        }.configureEach {
            doLast {
                val appRoot = layout.buildDirectory.dir("compose/binaries/main/app").get().asFile
                if (!appRoot.exists()) return@doLast
                appRoot.walkTopDown().filter { it.isFile && it.extension == "cfg" }.forEach { cfg ->
                    reorderJpackageAppClasspath(cfg)
                }
            }
        }

        tasks.named("fixConveyorConfig").configure {
            doLast {
                val configFile = layout.projectDirectory.file("generated.conveyor.conf").asFile
                if (configFile.exists()) {
                    reorderConveyorJarInputBlocks(configFile)
                }
            }
        }
    }
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
