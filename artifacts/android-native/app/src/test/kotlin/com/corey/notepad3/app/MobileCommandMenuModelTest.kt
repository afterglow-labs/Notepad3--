package com.corey.notepad3.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileCommandMenuModelTest {
    @Test
    fun topOverflowKeepsIosQuickActionShape() {
        val sections = mobileMenuSections(MobileMenuSurface.TOP_QUICK)

        assertEquals(listOf("Quick actions", "Line tools", "Document"), sections.map { it.title })
        assertEquals(
            listOf(
                "Preferences",
                "Compare documents",
                "Change language",
                "Go to line",
                "Virtual trackpad",
                "Preview markdown",
                "Read mode",
                "Zen mode",
            ),
            sections.first().rows.map { it.title },
        )
    }

    @Test
    fun bottomMoreMirrorsClassicMenuBarSections() {
        val sections = mobileMenuSections(MobileMenuSurface.MENU_BAR)

        assertEquals(listOf("File", "Edit", "View", "Tools", "Help"), sections.map { it.title })
        assertTrue(sections.first { it.title == "File" }.rows.map { it.title }.contains("Open from Files"))
        assertTrue(sections.first { it.title == "Edit" }.rows.map { it.title }.contains("Find and replace"))
        assertTrue(sections.first { it.title == "View" }.rows.map { it.title }.contains("Switch to classic layout"))
    }

    @Test
    fun keyboardAccessoryMoreUsesMenuBarSheet() {
        assertEquals(MobileMenuSurface.MENU_BAR, keyboardAccessoryMoreSurface())
    }
}
