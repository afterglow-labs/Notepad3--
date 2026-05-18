package com.corey.notepad3.app

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class DocumentImportTest {
    @Test
    fun openPickerAllowsAnyFileTypeSoPlaylistsCanBeImported() {
        assertArrayEquals(arrayOf("*/*"), DocumentImport.openMimeTypes)
    }
}
