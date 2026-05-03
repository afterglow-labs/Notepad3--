package com.corey.notepad3.editor

import java.util.Locale

data class TextSelection(
    val start: Int,
    val end: Int = start,
) {
    val min: Int = kotlin.math.min(start, end)
    val max: Int = kotlin.math.max(start, end)

    fun clamped(length: Int): TextSelection {
        val safeStart = start.coerceIn(0, length)
        val safeEnd = end.coerceIn(0, length)
        return TextSelection(safeStart, safeEnd)
    }
}

data class EditResult(
    val body: String,
    val selection: TextSelection,
)

data class SearchOptions(
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false,
)

object EditorCommands {
    fun trimTrailingSpaces(body: String, selection: TextSelection): EditResult {
        val trimmed = body.split("\n", ignoreCase = false, limit = 0)
            .joinToString("\n") { it.replace(Regex("[ \\t]+$"), "") }
        return EditResult(trimmed, selection.clamped(trimmed.length))
    }

    fun trimLeadingSpaces(body: String, selection: TextSelection): EditResult {
        val safeSelection = selection.clamped(body.length)
        val replacements = lineRanges(body).mapNotNull { range ->
            val indentEnd = body.indentEnd(range.first, range.second)
            val removalLength = indentEnd - range.first
            if (removalLength > 0) {
                TextReplacement(start = range.first, removeLength = removalLength)
            } else {
                null
            }
        }

        val next = replacements.asReversed().fold(body) { current, replacement ->
            current.removeRange(replacement.start, replacement.start + replacement.removeLength)
        }
        return EditResult(next, shiftSelectionForReplacements(safeSelection, replacements, next.length))
    }

    fun sortLines(body: String): EditResult {
        val sorted = body.split("\n")
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .joinToString("\n")
        return EditResult(sorted, TextSelection(0))
    }

    fun reverseLines(body: String): EditResult {
        val reversed = body.split("\n")
            .asReversed()
            .joinToString("\n")
        return EditResult(reversed, TextSelection(0))
    }

    fun removeDuplicateLines(body: String): EditResult {
        val seen = linkedSetOf<String>()
        val deduped = body.split("\n")
            .filter { seen.add(it) }
            .joinToString("\n")
        return EditResult(deduped, TextSelection(0))
    }

    fun joinSelectedLines(body: String, selection: TextSelection): EditResult {
        val safeSelection = selection.clamped(body.length)
        val ranges = lineRanges(body)
        if (ranges.size < 2) return EditResult(body, safeSelection)

        val lookupEnd = if (safeSelection.min == safeSelection.max) {
            safeSelection.max
        } else {
            (safeSelection.max - 1).coerceAtLeast(safeSelection.min)
        }
        val firstLine = lineIndexForCaret(ranges, body, safeSelection.min)
        var lastLine = lineIndexForCaret(ranges, body, lookupEnd)
        if (firstLine == lastLine && lastLine < ranges.lastIndex) {
            lastLine += 1
        }
        if (lastLine <= firstLine) return EditResult(body, safeSelection)

        val replacements = (firstLine until lastLine).map { lineIndex ->
            val lineEnd = ranges[lineIndex].second
            val nextLine = ranges[lineIndex + 1]
            TextReplacement(
                start = lineEnd,
                removeLength = body.indentEnd(nextLine.first, nextLine.second) - lineEnd,
                insert = " ",
            )
        }

        val next = replacements.asReversed().fold(body) { current, replacement ->
            current.replaceRange(
                replacement.start,
                replacement.start + replacement.removeLength,
                replacement.insert,
            )
        }
        return EditResult(next, shiftSelectionForReplacements(safeSelection, replacements, next.length))
    }

    fun duplicateCurrentLine(body: String, caret: Int): EditResult {
        val (lineStart, lineEnd) = lineRange(body, caret)
        val line = body.substring(lineStart, lineEnd)
        val inserted = "\n$line"
        val next = body.replaceRange(lineEnd, lineEnd, inserted)
        return EditResult(next, TextSelection((caret + inserted.length).coerceIn(0, next.length)))
    }

    fun deleteCurrentLine(body: String, caret: Int): EditResult {
        val (lineStart, lineEnd) = lineRange(body, caret)
        val removeEnd = if (lineEnd < body.length && body[lineEnd] == '\n') lineEnd + 1 else lineEnd
        val next = body.removeRange(lineStart, removeEnd)
        return EditResult(next, TextSelection(lineStart.coerceIn(0, next.length)))
    }

    fun insertText(body: String, selection: TextSelection, value: String): EditResult {
        val safeSelection = selection.clamped(body.length)
        val next = body.replaceRange(safeSelection.min, safeSelection.max, value)
        return EditResult(next, TextSelection(safeSelection.min + value.length))
    }

    fun uppercaseSelection(body: String, selection: TextSelection): EditResult =
        transformSelection(body, selection) { it.uppercase(Locale.ROOT) }

    fun lowercaseSelection(body: String, selection: TextSelection): EditResult =
        transformSelection(body, selection) { it.lowercase(Locale.ROOT) }

    fun indentSelection(body: String, selection: TextSelection): EditResult {
        val safeSelection = selection.clamped(body.length)
        val starts = selectedLineStarts(body, safeSelection)
        val next = insertAtLineStarts(body, starts, "    ")
        val selectionShift = starts.count { it <= safeSelection.start } * 4
        val endShift = starts.count { it <= safeSelection.end } * 4
        return EditResult(
            body = next,
            selection = TextSelection(safeSelection.start + selectionShift, safeSelection.end + endShift),
        )
    }

    fun unindentSelection(body: String, selection: TextSelection): EditResult {
        val safeSelection = selection.clamped(body.length)
        val starts = selectedLineStarts(body, safeSelection)
        val removals = starts.map { lineStart ->
            lineStart to indentationRemovalLength(body, lineStart)
        }.filter { (_, removalLength) -> removalLength > 0 }

        val next = removals.asReversed().fold(body) { current, (lineStart, removalLength) ->
            current.removeRange(lineStart, lineStart + removalLength)
        }
        val startShift = removals.sumOf { (lineStart, removalLength) ->
            if (lineStart < safeSelection.start) removalLength else 0
        }
        val endShift = removals.sumOf { (lineStart, removalLength) ->
            if (lineStart < safeSelection.end) removalLength else 0
        }
        return EditResult(
            body = next,
            selection = TextSelection(
                (safeSelection.start - startShift).coerceIn(0, next.length),
                (safeSelection.end - endShift).coerceIn(0, next.length),
            ),
        )
    }

    fun gotoLine(body: String, lineNumber: Int): TextSelection {
        val targetLine = lineNumber.coerceAtLeast(1)
        var currentLine = 1
        var index = 0
        var lastLineStart = 0

        while (index < body.length && currentLine < targetLine) {
            if (body[index] == '\n') {
                currentLine += 1
                lastLineStart = index + 1
            }
            index += 1
        }

        return TextSelection(lastLineStart.coerceIn(0, body.length))
    }

    fun selectAll(body: String): TextSelection = TextSelection(0, body.length)

    fun selectLine(body: String, caret: Int): TextSelection {
        val (lineStart, lineEnd) = lineRange(body, caret)
        return TextSelection(lineStart, lineEnd)
    }

    fun selectWord(body: String, caret: Int): TextSelection {
        val clamped = caret.coerceIn(0, body.length)
        if (clamped == body.length || !body[clamped].isEditorWordChar()) {
            return TextSelection(clamped)
        }

        var start = clamped
        while (start > 0 && body[start - 1].isEditorWordChar()) {
            start -= 1
        }

        var end = clamped
        while (end < body.length && body[end].isEditorWordChar()) {
            end += 1
        }

        return TextSelection(start, end)
    }

    fun selectParagraph(body: String, caret: Int): TextSelection {
        val ranges = lineRanges(body)
        val lineIndex = lineIndexForCaret(ranges, body, caret)
        if (lineIsBlank(body, ranges[lineIndex])) {
            return TextSelection(ranges[lineIndex].first, ranges[lineIndex].second)
        }

        var firstLine = lineIndex
        while (firstLine > 0 && !lineIsBlank(body, ranges[firstLine - 1])) {
            firstLine -= 1
        }

        var lastLine = lineIndex
        while (lastLine < ranges.lastIndex && !lineIsBlank(body, ranges[lastLine + 1])) {
            lastLine += 1
        }

        val start = ranges[firstLine].first
        val endBeforeLineBreak = ranges[lastLine].second
        val end = if (endBeforeLineBreak < body.length) endBeforeLineBreak + 1 else endBeforeLineBreak
        return TextSelection(start, end)
    }

    fun findNext(
        body: String,
        query: String,
        selection: TextSelection,
        options: SearchOptions = SearchOptions(),
    ): TextSelection? {
        if (query.isBlank()) return null
        val startAt = selection.clamped(body.length).max
        val matches = findMatches(body, query, options)
        return matches.firstOrNull { it.start >= startAt } ?: matches.firstOrNull()
    }

    fun findPrevious(
        body: String,
        query: String,
        selection: TextSelection,
        options: SearchOptions = SearchOptions(),
    ): TextSelection? {
        if (query.isBlank()) return null
        val before = selection.clamped(body.length).min
        val matches = findMatches(body, query, options)
        return matches.lastOrNull { it.start < before } ?: matches.lastOrNull()
    }

    fun replaceAll(
        body: String,
        query: String,
        replacement: String,
        options: SearchOptions = SearchOptions(),
    ): EditResult {
        if (query.isBlank()) return EditResult(body, TextSelection(0))
        val next = findMatches(body, query, options)
            .asReversed()
            .fold(body) { current, match ->
                current.replaceRange(match.start, match.end, replacement)
            }
        return EditResult(next, TextSelection(0))
    }

    fun replaceCurrent(
        body: String,
        query: String,
        replacement: String,
        selection: TextSelection,
        options: SearchOptions = SearchOptions(),
    ): EditResult {
        val safeSelection = selection.clamped(body.length)
        if (safeSelection.min == safeSelection.max) {
            return EditResult(body, findNext(body, query, safeSelection, options) ?: safeSelection)
        }

        val next = body.replaceRange(safeSelection.min, safeSelection.max, replacement)
        val caret = TextSelection(safeSelection.min + replacement.length)
        return EditResult(next, findNext(next, query, caret, options) ?: caret)
    }

    fun findMatches(
        body: String,
        query: String,
        options: SearchOptions = SearchOptions(),
    ): List<TextSelection> {
        if (query.isBlank()) return emptyList()
        val pattern = if (options.regex) query else Regex.escape(query)
        val regex = runCatching {
            Regex(
                pattern = pattern,
                options = if (options.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE),
            )
        }.getOrNull() ?: return emptyList()

        return regex.findAll(body)
            .map { match -> TextSelection(match.range.first, match.range.last + 1) }
            .filter { it.start < it.end }
            .filter { !options.wholeWord || body.hasWordBoundaryAround(it) }
            .toList()
    }

    fun toggleLineComment(body: String, selection: TextSelection, prefix: String): EditResult {
        val safeSelection = selection.clamped(body.length)
        if (prefix.isBlank()) return EditResult(body, safeSelection)

        val ranges = selectedLineRanges(body, safeSelection).filterNot { lineIsBlank(body, it) }
        if (ranges.isEmpty()) return EditResult(body, safeSelection)

        val shouldUncomment = ranges.all { range ->
            val indentEnd = body.indentEnd(range.first, range.second)
            body.startsWithAt(prefix, indentEnd)
        }

        val replacements = ranges.map { range ->
            val indentEnd = body.indentEnd(range.first, range.second)
            if (shouldUncomment) {
                val prefixEnd = indentEnd + prefix.length
                val trailingSpaceLength = if (body.getOrNull(prefixEnd) == ' ') 1 else 0
                TextReplacement(
                    start = indentEnd,
                    removeLength = prefix.length + trailingSpaceLength,
                )
            } else {
                TextReplacement(
                    start = indentEnd,
                    insert = "$prefix ",
                )
            }
        }

        val next = replacements.asReversed().fold(body) { current, replacement ->
            current.replaceRange(
                replacement.start,
                replacement.start + replacement.removeLength,
                replacement.insert,
            )
        }
        return EditResult(next, shiftSelectionForReplacements(safeSelection, replacements, next.length))
    }

    fun moveCurrentLineUp(body: String, caret: Int): EditResult =
        moveCurrentLine(body, caret, direction = -1)

    fun moveCurrentLineDown(body: String, caret: Int): EditResult =
        moveCurrentLine(body, caret, direction = 1)

    private fun lineRange(body: String, caret: Int): Pair<Int, Int> {
        val clamped = caret.coerceIn(0, body.length)
        var start = clamped
        while (start > 0 && body[start - 1] != '\n') start -= 1
        var end = clamped
        while (end < body.length && body[end] != '\n') end += 1
        return start to end
    }

    private fun lineRanges(body: String): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()
        var start = 0
        for (index in body.indices) {
            if (body[index] == '\n') {
                ranges += start to index
                start = index + 1
            }
        }
        ranges += start to body.length
        return ranges
    }

    private fun lineIndexForCaret(ranges: List<Pair<Int, Int>>, body: String, caret: Int): Int {
        val clamped = caret.coerceIn(0, body.length)
        return ranges.indexOfFirst { (_, end) -> clamped <= end }.takeIf { it >= 0 } ?: ranges.lastIndex
    }

    private fun lineIsBlank(body: String, range: Pair<Int, Int>): Boolean =
        body.substring(range.first, range.second).isBlank()

    private fun transformSelection(
        body: String,
        selection: TextSelection,
        transform: (String) -> String,
    ): EditResult {
        val safeSelection = selection.clamped(body.length)
        if (safeSelection.min == safeSelection.max) return EditResult(body, safeSelection)
        val replacement = transform(body.substring(safeSelection.min, safeSelection.max))
        val next = body.replaceRange(safeSelection.min, safeSelection.max, replacement)
        return EditResult(next, TextSelection(safeSelection.min, safeSelection.min + replacement.length))
    }

    private fun selectedLineStarts(body: String, selection: TextSelection): List<Int> {
        return selectedLineRanges(body, selection).map { it.first }
    }

    private fun selectedLineRanges(body: String, selection: TextSelection): List<Pair<Int, Int>> {
        val ranges = lineRanges(body)
        val lookupEnd = if (selection.min == selection.max) {
            selection.max
        } else {
            (selection.max - 1).coerceAtLeast(selection.min)
        }
        val firstLine = lineIndexForCaret(ranges, body, selection.min)
        val lastLine = lineIndexForCaret(ranges, body, lookupEnd)
        return (firstLine..lastLine).map { ranges[it] }
    }

    private fun insertAtLineStarts(body: String, lineStarts: List<Int>, value: String): String {
        var next = body
        var offset = 0
        lineStarts.forEach { lineStart ->
            val insertionPoint = lineStart + offset
            next = next.replaceRange(insertionPoint, insertionPoint, value)
            offset += value.length
        }
        return next
    }

    private fun indentationRemovalLength(body: String, lineStart: Int): Int {
        if (lineStart >= body.length) return 0
        if (body[lineStart] == '\t') return 1
        var count = 0
        while (lineStart + count < body.length && count < 4 && body[lineStart + count] == ' ') {
            count += 1
        }
        return count
    }

    private fun moveCurrentLine(body: String, caret: Int, direction: Int): EditResult {
        val ranges = lineRanges(body)
        val safeCaret = caret.coerceIn(0, body.length)
        val lineIndex = lineIndexForCaret(ranges, body, safeCaret)
        val targetIndex = lineIndex + direction
        if (targetIndex !in ranges.indices) {
            return EditResult(body, TextSelection(safeCaret))
        }

        val lines = body.split("\n").toMutableList()
        val currentLine = lines[lineIndex]
        lines[lineIndex] = lines[targetIndex]
        lines[targetIndex] = currentLine

        val next = lines.joinToString("\n")
        val column = safeCaret - ranges[lineIndex].first
        val movedLineStart = lineStartAt(lines, targetIndex)
        val movedLineEnd = movedLineStart + lines[targetIndex].length
        return EditResult(next, TextSelection((movedLineStart + column).coerceIn(movedLineStart, movedLineEnd)))
    }

    private fun lineStartAt(lines: List<String>, lineIndex: Int): Int =
        lines.take(lineIndex).sumOf { it.length + 1 }

    private fun Char.isEditorWordChar(): Boolean =
        isLetterOrDigit() || this == '_' || this == '-' || this == '\''

    private fun String.hasWordBoundaryAround(selection: TextSelection): Boolean {
        val before = getOrNull(selection.start - 1)
        val after = getOrNull(selection.end)
        return before?.isEditorWordChar() != true && after?.isEditorWordChar() != true
    }

    private fun String.indentEnd(start: Int, end: Int): Int {
        var index = start
        while (index < end && (this[index] == ' ' || this[index] == '\t')) {
            index += 1
        }
        return index
    }

    private fun String.startsWithAt(prefix: String, index: Int): Boolean =
        index >= 0 && index + prefix.length <= length && substring(index, index + prefix.length) == prefix

    private fun shiftSelectionForReplacements(
        selection: TextSelection,
        replacements: List<TextReplacement>,
        nextLength: Int,
    ): TextSelection {
        var start = selection.start
        var end = selection.end
        replacements.forEach { replacement ->
            val delta = replacement.insert.length - replacement.removeLength
            if (replacement.start < selection.start) start += delta
            if (replacement.start < selection.end) end += delta
        }
        return TextSelection(
            start.coerceIn(0, nextLength),
            end.coerceIn(0, nextLength),
        )
    }

    private data class TextReplacement(
        val start: Int,
        val removeLength: Int = 0,
        val insert: String = "",
    )
}
