package com.corey.notepad3.app

import com.corey.notepad3.models.DocumentLanguage
import com.corey.notepad3.models.TextDocument
import java.util.Locale

object DocumentExport {
    fun fileNameFor(document: TextDocument): String {
        val sanitized = document.title
            .trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .map { char ->
                if (char.isISOControl() || char in invalidFileNameChars) '_' else char
            }
            .joinToString("")
            .trim()
            .trimEnd('.')
            .trim()
        return sanitized.takeUnless { it.isEmpty() || it.all { char -> char == '_' } } ?: "untitled.txt"
    }

    fun mimeTypeFor(document: TextDocument): String {
        val title = fileNameFor(document).lowercase(Locale.ROOT)
        return when {
            title.endsWithAny(".md", ".markdown") || document.language == DocumentLanguage.MARKDOWN -> {
                "text/markdown"
            }
            title.endsWithAny(".json", ".jsonc") || document.language == DocumentLanguage.JSON -> {
                "application/json"
            }
            title.endsWithAny(".py", ".pyw") || document.language == DocumentLanguage.PYTHON -> {
                "text/x-python"
            }
            title.endsWithAny(".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs") ||
                document.language == DocumentLanguage.JAVA_SCRIPT -> {
                "application/javascript"
            }
            title.endsWithAny(".kt", ".kts") || document.language == DocumentLanguage.KOTLIN -> "text/x-kotlin"
            title.endsWith(".swift") || document.language == DocumentLanguage.SWIFT -> "text/x-swift"
            title.endsWithAny(".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".hxx") ||
                document.language == DocumentLanguage.C_PLUS_PLUS -> "text/x-c++src"
            title.endsWith(".java") || document.language == DocumentLanguage.JAVA -> "text/x-java-source"
            title.endsWithAny(".cs", ".csx") || document.language == DocumentLanguage.C_SHARP -> "text/x-csharp"
            title.endsWith(".go") || document.language == DocumentLanguage.GO -> "text/x-go"
            title.endsWith(".rs") || document.language == DocumentLanguage.RUST -> "text/rust"
            title.endsWith(".dart") || document.language == DocumentLanguage.DART -> "text/x-dart"
            title.endsWithAny(".php", ".phtml") || document.language == DocumentLanguage.PHP -> "application/x-httpd-php"
            title.endsWithAny(".rb", ".rake", ".gemspec") || title == "gemfile" ||
                document.language == DocumentLanguage.RUBY -> "text/x-ruby"
            title.endsWithAny(".sh", ".bash", ".zsh", ".fish", ".ksh") ||
                title == ".bashrc" ||
                title == ".zshrc" ||
                title == ".profile" ||
                document.language == DocumentLanguage.SHELL -> "application/x-sh"
            title.endsWithAny(".ps1", ".psm1", ".psd1") ||
                document.language == DocumentLanguage.POWERSHELL -> "text/x-powershell"
            title.endsWith(".sql") || document.language == DocumentLanguage.SQL -> "application/sql"
            title.endsWithAny(".m3u", ".m3u8") ||
                document.language == DocumentLanguage.PLAYLIST -> "application/vnd.apple.mpegurl"
            title.endsWithAny(".yml", ".yaml") || document.language == DocumentLanguage.YAML -> "application/yaml"
            title.endsWith(".toml") || document.language == DocumentLanguage.TOML -> "application/toml"
            title.endsWithAny(".ini", ".cfg", ".conf") ||
                title == ".editorconfig" ||
                title == ".gitconfig" ||
                document.language == DocumentLanguage.INI -> "text/plain"
            title == "dockerfile" ||
                title == "containerfile" ||
                title.startsWith("dockerfile.") ||
                title.startsWith("containerfile.") ||
                title.endsWith(".dockerfile") ||
                document.language == DocumentLanguage.DOCKERFILE -> "text/x-dockerfile"
            title.endsWithAny(".html", ".htm") || document.language == DocumentLanguage.HTML -> "text/html"
            title.endsWith(".css") || document.language == DocumentLanguage.CSS -> "text/css"
            title.endsWithAny(".xml", ".svg") ||
                document.language == DocumentLanguage.XML ||
                document.language == DocumentLanguage.WEB -> "text/xml"
            else -> "text/plain"
        }
    }

    private fun String.endsWithAny(vararg suffixes: String): Boolean =
        suffixes.any(::endsWith)

    private val invalidFileNameChars = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
}
