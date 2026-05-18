package com.corey.notepad3.persistence

import com.corey.notepad3.models.DocumentLanguage
import com.corey.notepad3.models.StarterContent
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentStoreTest {
    @Test
    fun startsWithWelcomeScratchpadWhenNoSavedSessionExists() {
        val store = DocumentStore(Files.createTempDirectory("np3").resolve("documents-v1.json").toFile())

        val snapshot = store.state.value

        assertEquals("welcome", snapshot.activeId)
        assertEquals("scratchpad.txt", snapshot.documents.single().title)
        assertTrue(snapshot.documents.single().body.contains("Welcome to Notepad 3"))
        assertTrue(snapshot.documents.single().body.contains("text editor"))
    }

    @Test
    fun canStartWithBlankScratchpadWhenConfigured() {
        val store = DocumentStore(
            file = Files.createTempDirectory("np3").resolve("documents-v1.json").toFile(),
            starterContent = StarterContent.BLANK,
        )

        assertEquals("scratchpad.txt", store.activeDocument.title)
        assertEquals("", store.activeDocument.body)
    }

    @Test
    fun updatesActiveDocumentAndReloadsItFromDisk() {
        val file = Files.createTempDirectory("np3").resolve("documents-v1.json").toFile()
        val store = DocumentStore(file)

        store.updateActive(title = "demo.py", body = "print('hi')", language = DocumentLanguage.PYTHON)
        val reloaded = DocumentStore(file)

        assertEquals("demo.py", reloaded.activeDocument.title)
        assertEquals("print('hi')", reloaded.activeDocument.body)
        assertEquals(DocumentLanguage.PYTHON, reloaded.activeDocument.language)
    }

    @Test
    fun draftBodyUpdatesAreVisibleImmediatelyButPersistOnlyWhenFlushed() {
        val file = Files.createTempDirectory("np3").resolve("documents-v1.json").toFile()
        val store = DocumentStore(file)
        store.updateActive(body = "saved")

        store.updateActiveDraft(body = "typing")

        assertEquals("typing", store.activeDocument.body)
        assertEquals("saved", DocumentStore(file).activeDocument.body)

        store.flushPendingChanges()

        assertEquals("typing", DocumentStore(file).activeDocument.body)
    }

    @Test
    fun backsUpCorruptSnapshotsBeforeStartingFresh() {
        val directory = Files.createTempDirectory("np3")
        val file = directory.resolve("documents-v1.json").toFile()
        file.writeText("{ not json")

        val store = DocumentStore(file, starterContent = StarterContent.BLANK)

        val backup = directory.toFile().listFiles { _, name ->
            name.startsWith("documents-v1.json.corrupt-")
        }.orEmpty().single()
        assertEquals("{ not json", backup.readText())
        assertEquals("scratchpad.txt", store.activeDocument.title)
        assertEquals("", store.activeDocument.body)
    }

    @Test
    fun loadsSnapshotsWithDuplicateDocumentIdsSafely() {
        val file = Files.createTempDirectory("np3").resolve("documents-v1.json").toFile()
        file.writeText(
            """
                {
                  "documents": [
                    {
                      "id": "same",
                      "title": "one.txt",
                      "body": "one",
                      "createdAt": "2026-01-01T00:00:00Z",
                      "updatedAt": "2026-01-01T00:00:00Z",
                      "language": "PLAIN"
                    },
                    {
                      "id": "same",
                      "title": "two.txt",
                      "body": "two",
                      "createdAt": "2026-01-01T00:00:00Z",
                      "updatedAt": "2026-01-01T00:00:00Z",
                      "language": "PLAIN"
                    }
                  ],
                  "activeId": "same"
                }
            """.trimIndent(),
        )

        val snapshot = DocumentStore(file).state.value

        assertEquals(2, snapshot.documents.map { it.id }.toSet().size)
        assertEquals("same", snapshot.activeId)
        assertEquals("one.txt", snapshot.documents[0].title)
        assertEquals("two.txt", snapshot.documents[1].title)
    }

    @Test
    fun importsDocumentsAndDetectsTheirLanguage() {
        val store = DocumentStore(Files.createTempDirectory("np3").resolve("documents-v1.json").toFile())

        val imported = store.importDocument(title = "settings.jsonc", body = "{ }")

        assertEquals(imported.id, store.state.value.activeId)
        assertEquals("settings.jsonc", store.activeDocument.title)
        assertEquals(DocumentLanguage.JSON, store.activeDocument.language)
    }

    @Test
    fun createsUntitledTextFilesAndMakesTheNewestDocumentActive() {
        val store = DocumentStore(Files.createTempDirectory("np3").resolve("documents-v1.json").toFile())

        val first = store.createBlank()
        val second = store.createBlank()

        assertEquals("untitled-1.txt", first.title)
        assertEquals("untitled-2.txt", second.title)
        assertEquals(second.id, store.state.value.activeId)
    }

    @Test
    fun createsUntitledTextFilesWithoutReusingNamesAfterGaps() {
        val store = DocumentStore(Files.createTempDirectory("np3").resolve("documents-v1.json").toFile())
        val first = store.createBlank()
        store.createBlank()
        store.close(first.id)

        val next = store.createBlank()

        assertEquals("untitled-3.txt", next.title)
    }

    @Test
    fun duplicatesRenamesAndClosesDocumentsLikeTheIosStore() {
        val store = DocumentStore(Files.createTempDirectory("np3").resolve("documents-v1.json").toFile())
        store.importDocument(title = "main.py", body = "print('hi')")

        val duplicate = store.duplicateActive()
        store.rename(duplicate.id, "copy.py")
        store.closeOthers(duplicate.id)

        assertEquals(1, store.state.value.documents.size)
        assertEquals("copy.py", store.activeDocument.title)
        assertEquals("print('hi')", store.activeDocument.body)

        store.close(duplicate.id)

        assertEquals(1, store.state.value.documents.size)
        assertEquals("scratchpad.txt", store.activeDocument.title)
        assertEquals("", store.activeDocument.body)
    }

    @Test
    fun duplicatesDocumentsWithoutReusingCopyNames() {
        val store = DocumentStore(Files.createTempDirectory("np3").resolve("documents-v1.json").toFile())
        store.importDocument(title = "main.py", body = "print('hi')")

        val first = store.duplicateActive()
        store.setActive("welcome")
        store.setActive(store.state.value.documents.first { it.title == "main.py" }.id)
        val second = store.duplicateActive()

        assertEquals("main copy.py", first.title)
        assertEquals("main copy 2.py", second.title)
    }
}
