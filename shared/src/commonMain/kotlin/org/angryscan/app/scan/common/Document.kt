@file:Suppress("BooleanMethodIsAlwaysInverted")

package org.angryscan.app.scan.common

import org.angryscan.common.engine.IMatcher

class Document(val size: Long, val path: String) {
/* This is main struct in this library - searcher.Document. All texts represent as searcher.Document finally */

    private var skipped = false

    private var documentFields: MutableMap<IMatcher, Int> = mutableMapOf()

    fun skip(): Document {
        skipped = true
        return this
    }

    fun skipped() = skipped

    // update document value
    fun updateDocument(field: IMatcher, value: Int) {
        if(value > 0)
            documentFields[field] = (documentFields[field] ?: 0) + value
    }

    // getValue document funDetected
    fun funDetected(): Int = documentFields.size

    // is document empty
    fun isEmpty(): Boolean = documentFields.isEmpty()

    fun length(): Int = this.documentFields.size

    // getValue document
    fun getDocumentFields(): Map<IMatcher, Int> {
        return documentFields.toMap()
    }

    operator fun plus(other: Map<IMatcher, Int>): Document {
        other.forEach { (f, v) ->
            updateDocument(f, v)
        }
        return this
    }
    operator fun plus(other: Pair<IMatcher, Int>): Document {
        updateDocument(other.first, other.second)
        return this
    }

    override fun toString(): String {
        return this.getDocumentFields().toString()
    }
}