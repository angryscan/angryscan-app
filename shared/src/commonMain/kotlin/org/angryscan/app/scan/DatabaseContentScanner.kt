package org.angryscan.app.scan

import org.angryscan.app.scan.common.Document
import org.angryscan.common.engine.IScanEngine

object DatabaseContentScanner {
    fun scanContent(
        path: String,
        content: String,
        engines: List<IScanEngine>
    ): Document {
        val document = Document(content.length.toLong(), path)

        engines.forEach { engine ->
            engine.scan(content)
                .groupBy { it.matcher }
                .forEach { (matcher, matches) ->
                    document.updateDocument(matcher, matches.size)
                }
        }

        return document
    }
}
