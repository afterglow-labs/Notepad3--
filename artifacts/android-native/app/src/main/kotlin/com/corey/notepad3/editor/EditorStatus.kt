package com.corey.notepad3.editor

object EditorStatus {
    fun summary(
        languageName: String,
        body: String,
        selection: TextSelection,
        readOnly: Boolean,
    ): String {
        val metrics = body.statusMetrics(selection.min)
        val modeSummary = if (readOnly) " | Read Only" else ""
        val selectionLength = selection.max - selection.min
        val selectionSummary = if (selectionLength > 0) " | Sel $selectionLength chars" else ""
        return "$languageName$modeSummary | ${metrics.lineCount} lines | ${body.length} chars | " +
            "Ln ${metrics.line}, Col ${metrics.column}$selectionSummary"
    }
}

private data class StatusMetrics(
    val lineCount: Int,
    val line: Int,
    val column: Int,
)

private fun String.statusMetrics(caret: Int): StatusMetrics {
    val clamped = caret.coerceIn(0, length)
    var lineCount = 1
    var caretLine = 1
    var caretColumn = 1

    for (index in indices) {
        if (this[index] == '\n') {
            lineCount += 1
        }

        if (index < clamped) {
            if (this[index] == '\n') {
                caretLine += 1
                caretColumn = 1
            } else {
                caretColumn += 1
            }
        }
    }

    return StatusMetrics(
        lineCount = lineCount,
        line = caretLine,
        column = caretColumn,
    )
}
