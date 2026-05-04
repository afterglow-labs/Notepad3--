package com.corey.notepad3.app

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

internal fun keyboardAccessoryMoreSurface(): MobileMenuSurface =
    MobileMenuSurface.MENU_BAR

internal fun mobileMenuSections(surface: MobileMenuSurface): List<MobileMenuSection> =
    when (surface) {
        MobileMenuSurface.TOP_QUICK -> topQuickMenuSections()
        MobileMenuSurface.MENU_BAR -> menuBarSections()
    }

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
                "Find",
                "Find and replace",
                "Go to line",
                "Insert date/time",
                "Sort lines",
                "Trim trailing spaces",
                "Duplicate current line",
                "Delete current line",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "View",
            rows = listOf(
                "Read mode",
                "Zen mode",
                "Compare documents",
                "Preview markdown",
                "Virtual trackpad",
                "Switch to classic layout",
                "Word wrap",
                "Line numbers",
                "Keyboard toolbar",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Tools",
            rows = listOf(
                "Preferences",
                "Change language",
                "Theme quick toggle",
            ).map(::MobileMenuRow),
        ),
        MobileMenuSection(
            title = "Help",
            rows = listOf("About Notepad 3++").map(::MobileMenuRow),
        ),
    )
