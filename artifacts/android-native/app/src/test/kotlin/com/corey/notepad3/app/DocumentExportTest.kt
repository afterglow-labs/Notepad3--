package com.corey.notepad3.app

import com.corey.notepad3.models.DocumentLanguage
import com.corey.notepad3.models.TextDocument
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentExportTest {
    @Test
    fun choosesUsefulMimeTypesFromLanguageAndFileExtension() {
        assertEquals(
            "text/markdown",
            DocumentExport.mimeTypeFor(TextDocument(title = "notes.md", language = DocumentLanguage.MARKDOWN)),
        )
        assertEquals(
            "application/json",
            DocumentExport.mimeTypeFor(TextDocument(title = "settings.jsonc", language = DocumentLanguage.JSON)),
        )
        assertEquals(
            "text/x-python",
            DocumentExport.mimeTypeFor(TextDocument(title = "script.py", language = DocumentLanguage.PYTHON)),
        )
        assertEquals(
            "text/plain",
            DocumentExport.mimeTypeFor(TextDocument(title = "scratch", language = DocumentLanguage.PLAIN)),
        )
    }

    @Test
    fun normalizesBlankExportNamesToTextFiles() {
        assertEquals("untitled.txt", DocumentExport.fileNameFor(TextDocument(title = "   ")))
        assertEquals("report", DocumentExport.fileNameFor(TextDocument(title = " report ")))
    }
}
