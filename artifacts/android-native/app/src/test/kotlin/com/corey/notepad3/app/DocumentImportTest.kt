package com.corey.notepad3.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentImportTest {
    @Test
    fun openPickerAllowsAnyFileTypeSoPlaylistsCanBeImported() {
        assertArrayEquals(arrayOf("*/*"), DocumentImport.openMimeTypes)
    }

    @Test
    fun incomingIntentClassifierAcceptsExternalFileOpenAndSharePayloads() {
        assertTrue(DocumentImport.canImportFromIncomingIntent("android.intent.action.VIEW", hasDataUri = true))
        assertTrue(DocumentImport.canImportFromIncomingIntent("android.intent.action.EDIT", hasDataUri = true))
        assertTrue(DocumentImport.canImportFromIncomingIntent("android.intent.action.SEND", hasStreamUri = true))
        assertTrue(DocumentImport.canImportFromIncomingIntent("android.intent.action.SEND", hasPlainText = true))

        assertFalse(DocumentImport.canImportFromIncomingIntent("android.intent.action.MAIN", hasDataUri = true))
        assertFalse(DocumentImport.canImportFromIncomingIntent("android.intent.action.VIEW"))
        assertFalse(DocumentImport.canImportFromIncomingIntent("android.intent.action.SEND"))
    }
}
