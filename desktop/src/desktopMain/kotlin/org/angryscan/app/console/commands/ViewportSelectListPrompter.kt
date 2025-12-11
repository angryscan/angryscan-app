package org.angryscan.app.console.commands

import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.SelectList
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.Viewport

/**
 * Single-select list that scrolls within terminal height.
 *
 * This is a workaround for Mordant issue #251: when the list is taller than the visible terminal
 * area, terminal scrolling can break the rendering and only the last items remain visible.
 *
 * The fix is to render the selectable part inside a [Viewport] with a fixed height and update the
 * viewport's scroll offset when the cursor moves.
 */
internal class ViewportSelectListPrompter(private val terminal: Terminal) : InteractiveSettingsMenu.SelectListPrompter {
    override fun select(entries: List<String>, title: String, startingIndex: Int): String? {
        if (entries.isEmpty()) return null

        val safeStartingIndex = startingIndex.coerceIn(0, entries.lastIndex)
        val isRootMenu = title == "Settings"

        val titleWidget = title.takeIf { it.isNotBlank() }?.let {
            Text(terminal.theme.style("select.title")(it))
        }

        val instructionsWidget = Text(
            if (isRootMenu) {
                dim("↑/↓ move • →/enter/space select • esc exit • ctrl+c abort")
            } else {
                dim("↑/↓ move • →/enter/space select • ← back • esc exit • ctrl+c abort")
            }
        )

        val reservedLines = (if (titleWidget == null) 0 else 1) + 1
        val listHeight = (terminal.size.height - reservedLines).coerceAtLeast(1)

        val entryWidgets = entries.map { SelectList.Entry(it) }

        data class State(val cursor: Int, val scrollDown: Int)

        fun computeScroll(cursor: Int, currentScrollDown: Int): Int {
            return computeScrollDown(
                cursorIndex = cursor,
                currentScrollDown = currentScrollDown,
                viewportHeight = listHeight,
                itemCount = entryWidgets.size,
            )
        }

        var state = State(
            cursor = safeStartingIndex,
            scrollDown = computeScroll(safeStartingIndex, 0),
        )

        val animation = terminal.animation<State> { s ->
            val list = SelectList(
                entries = entryWidgets,
                title = null,
                cursorIndex = s.cursor,
                styleOnHover = true,
                selectedMarker = "",
                unselectedMarker = "",
                captionBottom = null,
            )

            val viewport = Viewport(
                list,
                null,
                listHeight,
                0,
                s.scrollDown,
            )

            verticalLayout {
                if (titleWidget != null) cell(titleWidget) {}
                cell(viewport) {}
                cell(instructionsWidget) {}
            }
        }.apply { update(state) }

        fun finish(result: String?): InputReceiver.Status<String?> {
            // Clear the animation so the menu doesn't leave history.
            animation.clear()
            return InputReceiver.Status.Finished(result)
        }

        return terminal.receiveKeyEvents { key: KeyboardEvent ->
            when {
                key.ctrl && key.key.equals("c", ignoreCase = true) -> throw InteractiveSettingsMenu.AbortException()
                key.key == "Escape" -> throw InteractiveSettingsMenu.ExitRequestedException()
                // In the root menu, Left should do nothing (not even \"back\") to avoid surprises.
                key.key == "ArrowLeft" && !isRootMenu -> finish(null)
                key.key == "Enter" || key.key == "ArrowRight" || key.key == " " -> finish(entries[state.cursor])

                key.key == "ArrowUp" -> {
                    val newCursor = (state.cursor - 1).coerceAtLeast(0)
                    state = state.copy(cursor = newCursor, scrollDown = computeScroll(newCursor, state.scrollDown))
                    animation.update(state)
                    InputReceiver.Status.Continue
                }

                key.key == "ArrowDown" -> {
                    val newCursor = (state.cursor + 1).coerceAtMost(entries.lastIndex)
                    state = state.copy(cursor = newCursor, scrollDown = computeScroll(newCursor, state.scrollDown))
                    animation.update(state)
                    InputReceiver.Status.Continue
                }

                else -> InputReceiver.Status.Continue
            }
        }
    }

    override fun multiSelect(
        entries: List<String>,
        title: String,
        initialSelected: Set<String>,
        startingIndex: Int,
    ): Set<String>? {
        if (entries.isEmpty()) return emptySet()

        val safeStartingIndex = startingIndex.coerceIn(0, entries.lastIndex)

        val titleWidget = title.takeIf { it.isNotBlank() }?.let {
            Text(terminal.theme.style("select.title")(it))
        }

        val instructionsWidget = Text(
            dim("↑/↓ move • →/enter/space toggle • ← apply • esc exit • ctrl+c abort")
        )

        val reservedLines = (if (titleWidget == null) 0 else 1) + 1
        val listHeight = (terminal.size.height - reservedLines).coerceAtLeast(1)

        data class State(val cursor: Int, val scrollDown: Int, val selected: Set<String>)

        fun computeScroll(cursor: Int, currentScrollDown: Int): Int {
            return computeScrollDown(
                cursorIndex = cursor,
                currentScrollDown = currentScrollDown,
                viewportHeight = listHeight,
                itemCount = entries.size,
            )
        }

        var state = State(
            cursor = safeStartingIndex,
            scrollDown = computeScroll(safeStartingIndex, 0),
            selected = initialSelected.intersect(entries.toSet()),
        )

        val animation = terminal.animation<State> { s ->
            val list = SelectList(
                entries = entries.map { SelectList.Entry(it, selected = it in s.selected) },
                title = null,
                cursorIndex = s.cursor,
                styleOnHover = false,
                selectedMarker = "[x]",
                unselectedMarker = "[ ]",
                captionBottom = null,
            )

            val viewport = Viewport(
                list,
                null,
                listHeight,
                0,
                s.scrollDown,
            )

            verticalLayout {
                if (titleWidget != null) cell(titleWidget) {}
                cell(viewport) {}
                cell(instructionsWidget) {}
            }
        }.apply { update(state) }

        fun finish(result: Set<String>?): InputReceiver.Status<Set<String>?> {
            animation.clear()
            return InputReceiver.Status.Finished(result)
        }

        return terminal.receiveKeyEvents { key: KeyboardEvent ->
            when {
                key.ctrl && key.key.equals("c", ignoreCase = true) -> throw InteractiveSettingsMenu.AbortException()

                // Apply on exit (no explicit Apply/Cancel in the list).
                key.key == "Escape" -> throw InteractiveSettingsMenu.ExitRequestedException()
                key.key == "ArrowLeft" -> finish(state.selected)

                key.key == "ArrowUp" -> {
                    val newCursor = (state.cursor - 1).coerceAtLeast(0)
                    state = state.copy(cursor = newCursor, scrollDown = computeScroll(newCursor, state.scrollDown))
                    animation.update(state)
                    InputReceiver.Status.Continue
                }

                key.key == "ArrowDown" -> {
                    val newCursor = (state.cursor + 1).coerceAtMost(entries.lastIndex)
                    state = state.copy(cursor = newCursor, scrollDown = computeScroll(newCursor, state.scrollDown))
                    animation.update(state)
                    InputReceiver.Status.Continue
                }

                key.key == " " || key.key == "Enter" || key.key == "ArrowRight" -> {
                    val item = entries[state.cursor]
                    val next = state.selected.toMutableSet().also { sel ->
                        if (!sel.add(item)) sel.remove(item)
                    }.toSet()
                    state = state.copy(selected = next)
                    animation.update(state)
                    InputReceiver.Status.Continue
                }

                else -> InputReceiver.Status.Continue
            }
        }
    }
}

/**
 * Compute a safe vertical scroll offset for a list displayed inside a viewport.
 */
internal fun computeScrollDown(
    cursorIndex: Int,
    currentScrollDown: Int,
    viewportHeight: Int,
    itemCount: Int,
): Int {
    if (itemCount <= 0) return 0

    val height = viewportHeight.coerceAtLeast(1)
    val maxScroll = (itemCount - height).coerceAtLeast(0)

    val cursor = cursorIndex.coerceIn(0, itemCount - 1)
    var scrollDown = currentScrollDown.coerceIn(0, maxScroll)

    val visibleStart = scrollDown
    val visibleEnd = scrollDown + height - 1

    scrollDown = when {
        cursor < visibleStart -> cursor
        cursor > visibleEnd -> cursor - (height - 1)
        else -> scrollDown
    }

    return scrollDown.coerceIn(0, maxScroll)
}
