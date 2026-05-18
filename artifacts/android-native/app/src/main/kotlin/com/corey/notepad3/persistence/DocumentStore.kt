package com.corey.notepad3.persistence

import com.corey.notepad3.models.DocumentLanguage
import com.corey.notepad3.models.DocumentSnapshot
import com.corey.notepad3.models.StarterContent
import com.corey.notepad3.models.TextDocument
import com.corey.notepad3.models.duplicateTitleFor
import com.corey.notepad3.models.nextUntitledName
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DocumentStore(
    private val file: File,
    private val starterContent: StarterContent = StarterContent.WELCOME,
) {
    private val persistLock = Any()
    private var pendingPersistSnapshot: DocumentSnapshot? = null

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _state = MutableStateFlow(loadSnapshot())
    val state: StateFlow<DocumentSnapshot> = _state.asStateFlow()

    val activeDocument: TextDocument
        get() = _state.value.documents.firstOrNull { it.id == _state.value.activeId }
            ?: _state.value.documents.first()

    fun setActive(id: String) {
        if (_state.value.documents.none { it.id == id } || _state.value.activeId == id) return
        mutate { it.copy(activeId = id) }
    }

    fun updateActive(
        title: String? = null,
        body: String? = null,
        language: DocumentLanguage? = null,
        persistImmediately: Boolean = true,
    ) {
        updateDocument(
            activeDocument.id,
            title = title,
            body = body,
            language = language,
            persistImmediately = persistImmediately,
        )
    }

    fun updateActiveDraft(body: String) {
        updateActive(body = body, persistImmediately = false)
    }

    fun flushPendingChanges() {
        synchronized(persistLock) {
            val snapshot = pendingPersistSnapshot ?: return
            persist(snapshot)
            if (pendingPersistSnapshot == snapshot) {
                pendingPersistSnapshot = null
            }
        }
    }

    fun updateDocument(
        id: String,
        title: String? = null,
        body: String? = null,
        language: DocumentLanguage? = null,
        persistImmediately: Boolean = true,
    ) {
        val snapshot = _state.value
        val index = snapshot.documents.indexOfFirst { it.id == id }
        if (index < 0) return

        val nextDocuments = snapshot.documents.toMutableList()
        val current = nextDocuments[index]
        val nextTitle = title?.normalizedTitle() ?: current.title
        nextDocuments[index] = current.copy(
            title = nextTitle,
            body = body ?: current.body,
            language = language ?: title?.let { DocumentLanguage.detect(nextTitle) } ?: current.language,
            updatedAt = Instant.now(),
        )
        mutate(persistImmediately = persistImmediately) { it.copy(documents = nextDocuments) }
    }

    fun createBlank(): TextDocument {
        val document = TextDocument(title = _state.value.documents.nextUntitledName())
        mutate { snapshot ->
            snapshot.copy(
                documents = listOf(document) + snapshot.documents,
                activeId = document.id,
            )
        }
        return document
    }

    fun importDocument(
        title: String,
        body: String,
        language: DocumentLanguage = DocumentLanguage.detect(title),
    ): TextDocument {
        val document = TextDocument(title = title.normalizedTitle(), body = body, language = language)
        mutate { snapshot ->
            snapshot.copy(
                documents = listOf(document) + snapshot.documents,
                activeId = document.id,
            )
        }
        return document
    }

    fun duplicateActive(): TextDocument {
        val source = activeDocument
        val now = Instant.now()
        val duplicate = source.copy(
            id = UUID.randomUUID().toString(),
            title = _state.value.documents.duplicateTitleFor(source),
            createdAt = now,
            updatedAt = now,
        )
        mutate { snapshot ->
            snapshot.copy(
                documents = listOf(duplicate) + snapshot.documents,
                activeId = duplicate.id,
            )
        }
        return duplicate
    }

    fun rename(id: String, title: String) {
        updateDocument(id = id, title = title)
    }

    fun close(id: String) {
        val snapshot = _state.value
        if (snapshot.documents.none { it.id == id }) return

        if (snapshot.documents.size <= 1) {
            val scratchpad = TextDocument.blankScratchpad()
            mutate { DocumentSnapshot(documents = listOf(scratchpad), activeId = scratchpad.id) }
            return
        }

        val nextDocuments = snapshot.documents.filterNot { it.id == id }
        val nextActive = if (snapshot.activeId == id) nextDocuments.first().id else snapshot.activeId
        mutate { it.copy(documents = nextDocuments, activeId = nextActive) }
    }

    fun closeOthers(id: String) {
        val document = _state.value.documents.firstOrNull { it.id == id } ?: return
        mutate { DocumentSnapshot(documents = listOf(document), activeId = document.id) }
    }

    private fun mutate(
        persistImmediately: Boolean = true,
        block: (DocumentSnapshot) -> DocumentSnapshot,
    ) {
        val next = block(_state.value)
        _state.value = next
        if (persistImmediately) {
            synchronized(persistLock) {
                pendingPersistSnapshot = null
                persist(next)
            }
        } else {
            synchronized(persistLock) {
                pendingPersistSnapshot = next
            }
        }
    }

    private fun loadSnapshot(): DocumentSnapshot {
        val loaded = if (file.exists()) {
            val decoded = runCatching {
                json.decodeFromString<DocumentSnapshot>(file.readText())
            }
            if (decoded.isFailure) {
                backupUnreadableSnapshot()
            }
            decoded.getOrNull()
        } else {
            null
        }

        normalizeSnapshot(loaded)?.let { return it }

        val starter = TextDocument.scratchpad(starterContent)
        return DocumentSnapshot(documents = listOf(starter), activeId = starter.id)
    }

    private fun normalizeSnapshot(snapshot: DocumentSnapshot?): DocumentSnapshot? {
        if (snapshot == null || snapshot.documents.isEmpty()) return null

        val seenIds = mutableSetOf<String>()
        val documents = snapshot.documents.map { document ->
            val existingId = document.id.takeIf { it.isNotBlank() && seenIds.add(it) }
            val id = existingId ?: nextUniqueId(seenIds)
            if (id == document.id) document else document.copy(id = id)
        }
        val activeId = snapshot.activeId.takeIf { active ->
            documents.any { it.id == active }
        } ?: documents.first().id
        return snapshot.copy(documents = documents, activeId = activeId)
    }

    private fun nextUniqueId(seenIds: MutableSet<String>): String {
        while (true) {
            val id = UUID.randomUUID().toString()
            if (seenIds.add(id)) return id
        }
    }

    private fun backupUnreadableSnapshot() {
        if (!file.exists()) return

        val directory = file.parentFile ?: File(".")
        val backup = File(directory, "${file.name}.corrupt-${System.currentTimeMillis()}")
        runCatching {
            file.copyTo(backup, overwrite = false)
        }
    }

    private fun persist(snapshot: DocumentSnapshot) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(snapshot))
        runCatching {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.getOrElse {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun String.normalizedTitle(): String =
        trim().ifEmpty { "untitled.txt" }
}
