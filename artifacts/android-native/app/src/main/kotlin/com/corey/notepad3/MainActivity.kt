package com.corey.notepad3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.corey.notepad3.app.AndroidEditorPreferences
import com.corey.notepad3.app.DocumentExport
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
                    openDocumentLauncher.launch(
                        arrayOf("text/*", "application/json", "application/xml", "application/javascript"),
                    )
                },
                onSaveFile = ::beginExportDocument,
                onCloseApp = ::finishAndRemoveTask,
            )
        }
    }

    private fun importDocument(uri: Uri) {
        val body = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return
        documentStore.importDocument(title = displayName(uri), body = body)
    }

    private fun beginExportDocument(document: TextDocument) {
        pendingExportDocument = document
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = DocumentExport.mimeTypeFor(document)
            putExtra(Intent.EXTRA_TITLE, DocumentExport.fileNameFor(document))
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
}
