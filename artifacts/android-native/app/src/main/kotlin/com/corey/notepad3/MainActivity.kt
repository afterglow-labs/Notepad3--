package com.corey.notepad3

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.corey.notepad3.app.AndroidEditorPreferences
import com.corey.notepad3.app.DocumentExport
import com.corey.notepad3.app.DocumentImport
import com.corey.notepad3.app.EditorPreferenceController
import com.corey.notepad3.app.NotepadApp
import com.corey.notepad3.models.TextDocument
import com.corey.notepad3.persistence.DocumentStore
import com.corey.notepad3.theme.AndroidThemePreferences
import com.corey.notepad3.theme.ThemeController

class MainActivity : ComponentActivity() {
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var saveDocumentLauncher: ActivityResultLauncher<Intent>
    private var pendingExportDocument: TextDocument? = null

    private val documentStore: DocumentStore by lazy {
        DocumentStore(filesDir.resolve("documents-v1.json"))
    }

    private val themeController: ThemeController by lazy {
        ThemeController(AndroidThemePreferences(this))
    }

    private val editorPreferenceController: EditorPreferenceController by lazy {
        EditorPreferenceController(AndroidEditorPreferences(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(::importDocument)
        }
        saveDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val document = pendingExportDocument
            pendingExportDocument = null
            if (result.resultCode == RESULT_OK && document != null) {
                result.data?.data?.let { uri -> exportDocument(uri, document) }
            }
        }
        setContent {
            NotepadApp(
                store = documentStore,
                themeController = themeController,
                editorPreferenceController = editorPreferenceController,
                onOpenFile = {
                    openDocumentLauncher.launch(DocumentImport.openMimeTypes)
                },
                onSaveFile = ::beginExportDocument,
                onCloseApp = ::finishAndRemoveTask,
            )
        }
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStop() {
        documentStore.flushPendingChanges()
        super.onStop()
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val streamUri = incomingIntent.sharedStreamUri()
        val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
        val canImport = DocumentImport.canImportFromIncomingIntent(
            action = incomingIntent.action,
            hasDataUri = incomingIntent.data != null,
            hasStreamUri = streamUri != null,
            hasPlainText = !sharedText.isNullOrBlank(),
        )
        if (!canImport) return

        when (incomingIntent.action) {
            Intent.ACTION_VIEW,
            Intent.ACTION_EDIT,
            -> incomingIntent.data?.let(::importDocument)
            Intent.ACTION_SEND -> {
                when {
                    streamUri != null -> importDocument(streamUri)
                    !sharedText.isNullOrBlank() -> {
                        documentStore.importDocument(title = sharedTextTitle(incomingIntent), body = sharedText)
                    }
                }
            }
        }
        setIntent(Intent(Intent.ACTION_MAIN))
    }

    private fun importDocument(uri: Uri) {
        val body = contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: return
        documentStore.importDocument(title = displayName(uri), body = body)
    }

    private fun sharedTextTitle(intent: Intent): String =
        intent.getStringExtra(Intent.EXTRA_TITLE)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: "shared.txt"

    private fun beginExportDocument(document: TextDocument) {
        documentStore.flushPendingChanges()
        val latestDocument = documentStore.state.value.documents.firstOrNull { it.id == document.id }
            ?: documentStore.activeDocument
        pendingExportDocument = latestDocument
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = DocumentExport.mimeTypeFor(latestDocument)
            putExtra(Intent.EXTRA_TITLE, DocumentExport.fileNameFor(latestDocument))
        }
        saveDocumentLauncher.launch(intent)
    }

    private fun exportDocument(uri: Uri, document: TextDocument) {
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(document.body)
        }
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) {
                return cursor.getString(column)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "untitled.txt"
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
}
