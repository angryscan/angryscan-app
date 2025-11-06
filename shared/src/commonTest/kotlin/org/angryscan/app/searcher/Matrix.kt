package org.angryscan.app.searcher

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.extensions.Matchers
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class Matrix {
    companion object {
        private val matrix = readMatrix("/common/Matrix.txt")
        private val matrixFastscan = readMatrix("/common/MatrixFastscan.txt")

        private fun readMatrix(file: String) : Map<String, Map<String, Int>?>{
            val path = Matrix::class.java.getResource(file)
            assertNotNull(path)
            val f = File(path.file)
            assertTrue(f.exists())

            val m = mutableMapOf<String, Map<String, Int>?>()

            val lines = f.readLines()
            val headers = lines[0].trim().split("\\s+".toRegex())

            lines
                .drop(1)
                .forEach { str ->
                    val array = str.trim().split("\\s+".toRegex())
                    m[array[0]] = array.drop(1).map { it.toInt() }.mapIndexed { i, v ->
                        headers[i] to v
                    }.toMap()
                }
            return m
        }

        fun getMap(file: String, isFastScan: Boolean = false): Map<IMatcher, Int>? {
            val filename = file.replace('\\', '/')
            val res = mutableMapOf<IMatcher, Int>()
            if (matrix[filename] == null)
                return null
            if (isFastScan)
                matrixFastscan[filename]?.forEach { row ->
                    if(row.value > 0) {
                        val matcher = getFunByName(row.key)
                        if(matcher != null)
                            res[matcher] = row.value
                    }
                }
            else
                matrix[filename]?.forEach { row ->
                    if(row.value > 0) {
                        val matcher = getFunByName(row.key)
                        if(matcher != null)
                            res[matcher] = row.value
                    }
                }
            return res.toMap()
        }

        fun getFunByName(name: String): IMatcher? {
            return Matchers.firstOrNull { it.name.replace(" ", "").equals(name, ignoreCase = true) }
        }

        fun contains(name: String): Boolean {
            return matrix.containsKey(name)
        }
    }

}