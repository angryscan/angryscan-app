package org.angryscan.app.scan.common.files.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.common.engine.IScanEngine
import java.io.File
import kotlin.coroutines.CoroutineContext

@Serializable
enum class CodeFileType : IFileType {
    Java {
        override val extensions = listOf("java", "class")
    },
    Kotlin {
        override val extensions = listOf("kt", "kts", "ktm")
    },
    Python {
        override val extensions = listOf("py", "pyc", "pyo")
    },
    JavaScript{
        override val extensions = listOf("js", "mjs")
    },
    Cpp{
        override val extensions = listOf("cpp", "cc", "cxx", "hpp")
    },
    CSharp{
        override val extensions = listOf("cs")
    },
    Ruby{
        override val extensions = listOf("rb")
    },
    PHP{
        override val extensions = listOf("php", "php3", "php4", "php5", "phtml")
    },
    Go{
        override val extensions = listOf("go")
    },
    Rust{
        override val extensions = listOf("rs")
    },
    Swift{
        override val extensions = listOf("swift")
    },
    TypeScript{
        override val extensions = listOf("ts", "tsx")
    },
    HTML{
        override val extensions = listOf("html", "htm")
    },
    CSS{
        override val extensions = listOf("css")
    },
    SQL{
        override val extensions = listOf("sql")
    },
    Assembly{
        override val extensions = listOf("asm", "s", "inc")
    },
    COBOL{
        override val extensions = listOf("cob", "cpy", "cbl")
    },
    Fortran{
        override val extensions = listOf("f90", "f95", "f03", "f08")
    },
    Pascal{
        override val extensions = listOf("pas", "pp")
    },
    Delphi{
        override val extensions = listOf("dpr", "pas", "dpk")
    },
    VisualBasic{
        override val extensions = listOf("vb", "vbs")
    },
    MATLAB{
        override val extensions = listOf("m", "mat", "mdl")
    },
    Perl{
        override val extensions = listOf("pl", "pm")
    },
    Haskell{
        override val extensions = listOf("hs", "lhs")
    },
    Scala{
        override val extensions = listOf("scala")
    },
    Erlang{
        override val extensions = listOf("erl", "hrl")
    },
    Lua{
        override val extensions = listOf("lua")
    },
    R{
        override val extensions = listOf("R", "r", "Rmd")
    },
    C{
        override val extensions = listOf("c", "h")
    },
    Batch{
        override val extensions = listOf("bat", "cmd")
    },
    Shell{
        override val extensions = listOf("sh", "bash", "zsh", "ps1", "psd1", "psm1")
    };

    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean,
        selectedExtensions: List<IFileType>
    ): Document {
        var count = 0
        val res = Document(file.length(), file.absolutePath)
        withContext(Dispatchers.IO) {
            file.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (!line.isBlank()) {
                        count++
                    }
                }
            }
        }
        res + (CodeDetectFun to count)
        return res
    }
}