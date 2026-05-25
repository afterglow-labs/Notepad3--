package com.corey.notepad3.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DocumentLanguage(
    val displayName: String,
    val lineCommentPrefix: String?,
) {
    @SerialName("Plain")
    PLAIN("Plain", null),

    @SerialName("Markdown")
    MARKDOWN("Markdown", null),

    @SerialName("Assembly")
    ASSEMBLY("Assembly", ";"),

    @SerialName("JavaScript")
    JAVA_SCRIPT("JavaScript", "//"),

    @SerialName("Kotlin")
    KOTLIN("Kotlin", "//"),

    @SerialName("Swift")
    SWIFT("Swift", "//"),

    @SerialName("C++")
    C_PLUS_PLUS("C++", "//"),

    @SerialName("Java")
    JAVA("Java", "//"),

    @SerialName("C#")
    C_SHARP("C#", "//"),

    @SerialName("Go")
    GO("Go", "//"),

    @SerialName("Rust")
    RUST("Rust", "//"),

    @SerialName("Dart")
    DART("Dart", "//"),

    @SerialName("PHP")
    PHP("PHP", "//"),

    @SerialName("Ruby")
    RUBY("Ruby", "#"),

    @SerialName("Shell")
    SHELL("Shell", "#"),

    @SerialName("PowerShell")
    POWERSHELL("PowerShell", "#"),

    @SerialName("SQL")
    SQL("SQL", "--"),

    @SerialName("Playlist")
    PLAYLIST("Playlist", "#"),

    @SerialName("YAML")
    YAML("YAML", "#"),

    @SerialName("TOML")
    TOML("TOML", "#"),

    @SerialName("INI")
    INI("INI", ";"),

    @SerialName("Dockerfile")
    DOCKERFILE("Dockerfile", "#"),

    @SerialName("Python")
    PYTHON("Python", "#"),

    @SerialName("HTML")
    HTML("HTML", null),

    @SerialName("CSS")
    CSS("CSS", null),

    @SerialName("XML")
    XML("XML", null),

    @SerialName("Web")
    WEB("Web", "//"),

    @SerialName("JSON")
    JSON("JSON", "//");

    companion object {
        val selectableLanguages: List<DocumentLanguage> = listOf(
            PLAIN,
            MARKDOWN,
            JSON,
            HTML,
            CSS,
            WEB,
            JAVA_SCRIPT,
            KOTLIN,
            SWIFT,
            PYTHON,
            C_PLUS_PLUS,
            JAVA,
            C_SHARP,
            GO,
            RUST,
            DART,
            PHP,
            RUBY,
            SHELL,
            POWERSHELL,
            SQL,
            PLAYLIST,
            YAML,
            TOML,
            INI,
            DOCKERFILE,
            XML,
            ASSEMBLY,
        )

        fun detect(fileName: String): DocumentLanguage {
            val lower = fileName.lowercase()
            val base = lower.substringAfterLast('/').substringAfterLast('\\')
            return when {
                base.matchesDockerfile() -> DOCKERFILE
                base.matchesName("gemfile", "rakefile") || base.matchesExtension("rb", "rake", "gemspec") -> RUBY
                base.matchesName(".bashrc", ".zshrc", ".profile") ||
                    base.matchesExtension("sh", "bash", "zsh", "fish", "ksh") -> SHELL
                base.matchesName(".editorconfig", ".gitconfig") || base.matchesExtension("ini", "cfg", "conf") -> INI
                base.matchesExtension("asm", "s", "nasm", "masm", "inc") -> ASSEMBLY
                base.matchesExtension("md", "markdown") -> MARKDOWN
                base.matchesExtension("js", "jsx", "ts", "tsx", "mjs", "cjs") -> JAVA_SCRIPT
                base.matchesExtension("kt", "kts") -> KOTLIN
                base.matchesExtension("swift") -> SWIFT
                base.matchesExtension("java") -> JAVA
                base.matchesExtension("cs", "csx") -> C_SHARP
                base.matchesExtension("go") -> GO
                base.matchesExtension("rs") -> RUST
                base.matchesExtension("dart") -> DART
                base.matchesExtension("php", "phtml", "php3", "php4", "php5") -> PHP
                base.matchesExtension("ps1", "psm1", "psd1") -> POWERSHELL
                base.matchesExtension("sql") -> SQL
                base.matchesExtension("m3u", "m3u8") -> PLAYLIST
                base.matchesExtension("yml", "yaml") -> YAML
                base.matchesExtension("toml") -> TOML
                base.matchesExtension("c", "cc", "cpp", "cxx", "h", "hh", "hpp", "hxx") -> C_PLUS_PLUS
                base.matchesExtension("py", "pyw") -> PYTHON
                base.matchesExtension("html", "htm") -> HTML
                base.matchesExtension("css") -> CSS
                base.matchesExtension("xml", "svg") -> XML
                base.matchesExtension("json", "jsonc") -> JSON
                else -> PLAIN
            }
        }

        private fun String.matchesExtension(vararg extensions: String): Boolean =
            extensions.any { endsWith(".$it") }

        private fun String.matchesName(vararg names: String): Boolean =
            names.any { this == it }

        private fun String.matchesDockerfile(): Boolean =
            this == "dockerfile" ||
                this == "containerfile" ||
                startsWith("dockerfile.") ||
                startsWith("containerfile.") ||
                endsWith(".dockerfile")
    }
}
