package com.corey.notepad3.models

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentLanguageTest {
    @Test
    fun detectsLanguagesFromFileExtensions() {
        assertEquals(DocumentLanguage.ASSEMBLY, DocumentLanguage.detect("boot.asm"))
        assertEquals(DocumentLanguage.MARKDOWN, DocumentLanguage.detect("README.markdown"))
        assertEquals(DocumentLanguage.JAVA_SCRIPT, DocumentLanguage.detect("app.tsx"))
        assertEquals(DocumentLanguage.PYTHON, DocumentLanguage.detect("tool.pyw"))
        assertEquals(DocumentLanguage.HTML, DocumentLanguage.detect("index.HTML"))
        assertEquals(DocumentLanguage.CSS, DocumentLanguage.detect("site.css"))
        assertEquals(DocumentLanguage.KOTLIN, DocumentLanguage.detect("MainActivity.kt"))
        assertEquals(DocumentLanguage.SWIFT, DocumentLanguage.detect("NotepadApp.swift"))
        assertEquals(DocumentLanguage.C_PLUS_PLUS, DocumentLanguage.detect("editor.hpp"))
        assertEquals(DocumentLanguage.JAVA, DocumentLanguage.detect("Service.java"))
        assertEquals(DocumentLanguage.C_SHARP, DocumentLanguage.detect("Program.csx"))
        assertEquals(DocumentLanguage.GO, DocumentLanguage.detect("server.go"))
        assertEquals(DocumentLanguage.RUST, DocumentLanguage.detect("lib.rs"))
        assertEquals(DocumentLanguage.DART, DocumentLanguage.detect("main.dart"))
        assertEquals(DocumentLanguage.PHP, DocumentLanguage.detect("index.phtml"))
        assertEquals(DocumentLanguage.RUBY, DocumentLanguage.detect("Gemfile"))
        assertEquals(DocumentLanguage.SHELL, DocumentLanguage.detect(".zshrc"))
        assertEquals(DocumentLanguage.POWERSHELL, DocumentLanguage.detect("profile.ps1"))
        assertEquals(DocumentLanguage.SQL, DocumentLanguage.detect("schema.sql"))
        assertEquals(DocumentLanguage.YAML, DocumentLanguage.detect("workflow.yml"))
        assertEquals(DocumentLanguage.TOML, DocumentLanguage.detect("pyproject.toml"))
        assertEquals(DocumentLanguage.INI, DocumentLanguage.detect(".editorconfig"))
        assertEquals(DocumentLanguage.DOCKERFILE, DocumentLanguage.detect("Dockerfile"))
        assertEquals(DocumentLanguage.XML, DocumentLanguage.detect("layout.svg"))
        assertEquals(DocumentLanguage.JSON, DocumentLanguage.detect("settings.jsonc"))
        assertEquals(DocumentLanguage.PLAIN, DocumentLanguage.detect("scratchpad.txt"))
    }

    @Test
    fun exposesPrimaryLineCommentPrefixForEditorCommands() {
        assertEquals(";", DocumentLanguage.ASSEMBLY.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.JAVA_SCRIPT.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.KOTLIN.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.SWIFT.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.C_PLUS_PLUS.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.JAVA.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.C_SHARP.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.GO.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.RUST.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.DART.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.PHP.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.RUBY.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.SHELL.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.POWERSHELL.lineCommentPrefix)
        assertEquals("--", DocumentLanguage.SQL.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.YAML.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.TOML.lineCommentPrefix)
        assertEquals(";", DocumentLanguage.INI.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.DOCKERFILE.lineCommentPrefix)
        assertEquals("#", DocumentLanguage.PYTHON.lineCommentPrefix)
        assertEquals("//", DocumentLanguage.JSON.lineCommentPrefix)
        assertEquals(null, DocumentLanguage.HTML.lineCommentPrefix)
        assertEquals(null, DocumentLanguage.CSS.lineCommentPrefix)
        assertEquals(null, DocumentLanguage.PLAIN.lineCommentPrefix)
    }

    @Test
    fun exposesManualWebSyntaxMode() {
        assertEquals(true, DocumentLanguage.selectableLanguages.contains(DocumentLanguage.WEB))
    }

    @Test
    fun exposesNewSyntaxModesInPickerOrder() {
        assertEquals(true, DocumentLanguage.selectableLanguages.contains(DocumentLanguage.RUST))
        assertEquals(true, DocumentLanguage.selectableLanguages.contains(DocumentLanguage.GO))
        assertEquals(true, DocumentLanguage.selectableLanguages.contains(DocumentLanguage.SQL))
        assertEquals(true, DocumentLanguage.selectableLanguages.contains(DocumentLanguage.DOCKERFILE))
    }
}
