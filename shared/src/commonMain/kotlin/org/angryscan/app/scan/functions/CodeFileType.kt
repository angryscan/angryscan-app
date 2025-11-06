package org.angryscan.app.scan.functions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.angryscan.app.scan.common.Document
import java.io.File

enum class CodeFileType(val extensions: List<String>) {
    Java(listOf("java", "class")),
    Kotlin(listOf("kt", "kts", "ktm")),
    Python(listOf("py", "pyc", "pyo")),
    JavaScript(listOf("js", "mjs")),
    Cpp(listOf("cpp", "cc", "cxx", "hpp")),
    CSharp(listOf("cs")),
    Ruby(listOf("rb")),
    PHP(listOf("php", "php3", "php4", "php5", "phtml")),
    Go(listOf("go")),
    Rust(listOf("rs")),
    Swift(listOf("swift")),
    TypeScript(listOf("ts", "tsx")),
    HTML(listOf("html", "htm")),
    CSS(listOf("css")),
    SQL(listOf("sql")),
    Assembly(listOf("asm", "s", "inc")),
    COBOL(listOf("cob", "cpy", "cbl")),
    Fortran(listOf("f90", "f95", "f03", "f08")),
    Pascal(listOf("pas", "pp")),
    Delphi(listOf("dpr", "pas", "dpk")),
    VisualBasic(listOf("vb", "vbs")),
    MATLAB(listOf("m", "mat", "mdl")),
    Perl(listOf("pl", "pm")),
    Haskell(listOf("hs", "lhs")),
    Scala(listOf("scala")),
    Erlang(listOf("erl", "hrl")),
    Lua(listOf("lua")),
    R(listOf("R", "r", "Rmd")),
    C(listOf("c", "h")),
    Batch(listOf("bat", "cmd")),
    Shell(listOf("sh", "bash", "zsh", "ps1", "psd1", "psm1"))

    ;
    companion object {
        suspend fun scanFile(
            file: File
        ): Document  {
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
            return  res
        }
    }
}