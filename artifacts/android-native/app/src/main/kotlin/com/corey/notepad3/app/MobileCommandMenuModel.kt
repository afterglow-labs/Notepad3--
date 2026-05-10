package com.corey.notepad3.app

import com.corey.notepad3.models.DocumentLanguage

internal enum class MobileMenuSurface {
    TOP_QUICK,
    MENU_BAR,
}

internal data class MobileMenuSection(
    val title: String,
    val rows: List<MobileMenuRow>,
)

internal data class MobileMenuRow(
    val title: String,
)

internal data class ClassicSubmenuSpec(
    val title: String,
    val rows: List<MobileMenuRow>,
    val expandable: Boolean = rows.isNotEmpty(),
)

internal enum class PreferencesDestination(
    val title: String,
) {
    GENERAL("Preferences"),
    APPEARANCE("Appearance"),
    TOOLBAR("Bottom Toolbar"),
    EDITOR("Editor"),
}

internal data class KeyboardAccessoryToggleState(
    val label: String,
    val active: Boolean,
    val enabled: Boolean,
)

internal data class RepeatPressSpec(
    val initialDelayMillis: Long = 360L,
    val firstRepeatDelayMillis: Long = 170L,
    val minimumDelayMillis: Long = 32L,
    val accelerationNumerator: Long = 5L,
    val accelerationDenominator: Long = 6L,
)

internal val accessoryRepeatPressSpec = RepeatPressSpec()

internal fun keyboardAccessoryMoreSurface(): MobileMenuSurface =
    MobileMenuSurface.MENU_BAR

internal fun keyboardAccessoryUsesDeckSurface(): Boolean =
    false

internal fun accessoryStaticButtonRepeats(label: String): Boolean =
    label in setOf("Up", "Down", "Left", "Right", "Delete", "Backspace")

internal fun repeatDelayForIteration(
    iteration: Int,
    spec: RepeatPressSpec = accessoryRepeatPressSpec,
): Long {
    var delay = spec.firstRepeatDelayMillis
    repeat(iteration.coerceAtLeast(0)) {
        delay = maxOf(
            spec.minimumDelayMillis,
            delay * spec.accelerationNumerator / spec.accelerationDenominator,
        )
    }
    return delay
}

internal fun keyboardAccessoryToggleState(
    keyboardSuppressed: Boolean,
    readOnly: Boolean,
): KeyboardAccessoryToggleState =
    KeyboardAccessoryToggleState(
        label = if (keyboardSuppressed) "Show" else "Hide",
        active = keyboardSuppressed,
        enabled = !readOnly,
    )

internal fun shouldShowPersistentDocumentStrip(layoutMode: EditorLayoutMode): Boolean =
    layoutMode == EditorLayoutMode.CLASSIC

private const val DOCUMENT_TAB_MAX_TITLE_CHARS = 26
private const val DOCUMENT_TAB_MIN_WIDTH_DP = 86
private const val DOCUMENT_TAB_MAX_WIDTH_DP = 184

internal fun documentTabDisplayTitle(title: String): String {
    val cleaned = title.trim().ifBlank { "untitled.txt" }
    return if (cleaned.length <= DOCUMENT_TAB_MAX_TITLE_CHARS) {
        cleaned
    } else {
        cleaned.take(DOCUMENT_TAB_MAX_TITLE_CHARS - 3) + "..."
    }
}

internal fun documentTabWidthDp(title: String): Int {
    val visibleChars = minOf(documentTabDisplayTitle(title).length, DOCUMENT_TAB_MAX_TITLE_CHARS)
    return (23 + visibleChars * 7).coerceIn(DOCUMENT_TAB_MIN_WIDTH_DP, DOCUMENT_TAB_MAX_WIDTH_DP)
}

internal fun shouldShowSoftKeyboardOnEditorFocus(
    readOnly: Boolean,
    keyboardSuppressed: Boolean,
): Boolean =
    !readOnly && !keyboardSuppressed

internal fun mobileMenuSections(surface: MobileMenuSurface): List<MobileMenuSection> =
    when (surface) {
        MobileMenuSurface.TOP_QUICK -> topQuickMenuSections()
        MobileMenuSurface.MENU_BAR -> menuBarSections()
    }

internal fun classicSettingsSubmenus(): List<ClassicSubmenuSpec> =
    listOf(
        ClassicSubmenuSpec(
            title = "Appearance",
            rows = listOf(
                "Bottom toolbar preferences...",
                "Themes",
            ).map(::MobileMenuRow),
        ),
    )

internal fun preferencesHomeRows(): List<MobileMenuRow> =
    listOf(
        MobileMenuRow("Appearance"),
        MobileMenuRow("Bottom Toolbar"),
        MobileMenuRow("Editor"),
    )

internal fun preferencesBackDestination(destination: PreferencesDestination): PreferencesDestination? =
    if (destination == PreferencesDestination.GENERAL) null else PreferencesDestination.GENERAL

private fun topQuickMenuSections(): List<MobileMenuSection> =
    listOf(
        MobileMenuSection(
            title = "Quick actions",
            rows = listOf(
                "Preferences",
                "Compare documents",
                "Change language",
                "Go to line",
                "Virtual trackpad",
                "Preview markdown",
                "Read mode",
                "Zen mode",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Line tools",
            rows = listOf(
                "Sort lines",
                "Trim trailing spaces",
                "Duplicate current line",
                "Delete current line",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Document",
            rows = listOf(
                "Insert date/time",
                "Duplicate current doc",
                "Rename current doc",
                "Close current doc",
            ).map(::MobileMenuRow),
        ),
    )

private fun menuBarSections(): List<MobileMenuSection> =
    listOf(
        MobileMenuSection(
            title = "File",
            rows = listOf(
                "New blank",
                "Open documents",
                "Open from Files",
                "Save",
                "Duplicate current",
                "Rename current",
                "Close current",
                "Close others",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Edit",
            rows = listOf(
                "Undo",
                "Redo",
                "Cut",
                "Copy",
                "Paste",
                "Select all",
                "Select word",
                "Select line",
                "Select paragraph",
                "Insert date/time",
                "Uppercase selection",
                "Lowercase selection",
                "Indent",
                "Unindent",
                "Toggle comment",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Search",
            rows = listOf(
                "Find/Replace",
                "Go to line",
                "Compare documents",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "View",
            rows = listOf(
                "Read mode",
                "Zen mode",
                "Preview markdown",
                "Virtual trackpad",
                "Switch to classic layout",
                "Word wrap",
                "Line numbers",
                "Bottom toolbar",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Language",
            rows = DocumentLanguage.selectableLanguages.map { MobileMenuRow(it.displayName) },
        ),
        MobileMenuSection(
            title = "Settings",
            rows = listOf(
                "Preferences",
                "Appearance preferences",
                "Bottom toolbar preferences",
                "Cycle theme",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Tools",
            rows = listOf(
                "Duplicate current line",
                "Delete current line",
                "Move line up",
                "Move line down",
                "Sort lines",
                "Trim trailing spaces",
                "Trim leading spaces",
                "Join selected lines",
                "Reverse lines",
                "Unique lines",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Help",
            rows = listOf("About Notepad 3++").map(::MobileMenuRow),
        ),
    )
