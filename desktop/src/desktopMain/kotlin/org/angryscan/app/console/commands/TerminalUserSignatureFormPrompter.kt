package org.angryscan.app.console.commands

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import org.angryscan.common.matchers.UserSignature

/**
 * Interactive text form for editing/creating a [UserSignature].
 *
 * Implementation note:
 * To support Cyrillic input on Windows, we force console input/output code pages to UTF-8 before
 * reading user input. Mordant's prompt() uses line reading on JVM, which otherwise can crash with
 * MalformedInputException depending on the active console code page.
 */
internal class TerminalUserSignatureFormPrompter(
    private val terminal: Terminal,
) : InteractiveSettingsMenu.UserSignatureFormPrompter {
    override fun edit(existing: UserSignature?, reservedNames: Set<String>): UserSignature? {
        val title = if (existing == null) "Add user signature" else "Edit user signature"

        val initialName = existing?.name.orEmpty()
        val initialSignature = existing?.searchSignatures?.joinToString(", ").orEmpty()

        var name = initialName
        var signature = initialSignature

        val list = ViewportSelectListPrompter(terminal)
        var lastIndex = 0

        fun editLine(title: String, field: String, initial: String): String? {
            WindowsConsoleUtf8.ensureUtf8()

            // Show current value explicitly (no "(default)" UI).
            terminal.println("${field}: $initial")

            // Note: prompt() returns null on cancel. We treat it as "keep current value".
            // We also hide the default to avoid showing it in parentheses.
            return terminal.prompt(
                prompt = "New $field (leave empty to keep current)",
                default = initial,
                showDefault = false,
            ) ?: initial
        }

        fun parseSignatureValues(raw: String): List<String> {
            return raw
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }

        fun isValid(): Boolean {
            val trimmedName = name.trim()
            val sigs = parseSignatureValues(signature)
            val reservedTrimmedNames = reservedNames.map { it.trim() }.toSet()
            if (trimmedName.isEmpty()) return false
            // Name must be unique (consider trimmed values to avoid "Name" vs " Name " duplicates).
            if (trimmedName in reservedTrimmedNames) return false
            if (sigs.isEmpty()) return false
            return true
        }

        fun isChanged(): Boolean {
            return name != initialName || signature != initialSignature
        }

        while (true) {
            val showSave = isValid() && isChanged()
            val entries = buildList {
                add("Name: $name")
                add("Signature: $signature")
                if (showSave) add("Save")
                add("Cancel")
            }

            val selected = list.select(entries = entries, title = title, startingIndex = lastIndex) ?: return null
            lastIndex = entries.indexOf(selected).coerceAtLeast(0)

            when (selected) {
                entries[0] -> {
                    name = editLine(title = title, field = "Name", initial = name) ?: name
                }

                entries[1] -> {
                    signature = editLine(title = title, field = "Signature (comma separated)", initial = signature) ?: signature
                }

                "Cancel" -> return null
                "Save" -> {
                    val trimmedName = name.trim()
                    val sigs = parseSignatureValues(signature)

                    if (trimmedName.isEmpty()) {
                        // Show a simple one-shot error and go back to the form.
                        list.select(entries = listOf("OK"), title = "Name is required", startingIndex = 0)
                        continue
                    }
                    if (trimmedName in reservedNames) {
                        list.select(entries = listOf("OK"), title = "User signature with this name already exists", startingIndex = 0)
                        continue
                    }
                    if (sigs.isEmpty()) {
                        list.select(entries = listOf("OK"), title = "Signature is required", startingIndex = 0)
                        continue
                    }

                    return UserSignature(
                        name = trimmedName,
                        searchSignatures = sigs.toMutableList(),
                    )
                }
            }
        }
    }
}


