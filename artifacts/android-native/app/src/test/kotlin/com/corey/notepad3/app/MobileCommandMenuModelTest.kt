package com.corey.notepad3.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        assertEquals(listOf("File", "Edit", "Search", "View", "Language", "Settings", "Tools", "Help"), sections.map { it.title })
        assertTrue(sections.first { it.title == "File" }.rows.map { it.title }.contains("Open from Files"))
        assertTrue(sections.first { it.title == "Edit" }.rows.map { it.title }.contains("Toggle comment"))
        assertTrue(sections.first { it.title == "Search" }.rows.map { it.title }.contains("Find and replace"))
        assertTrue(sections.first { it.title == "View" }.rows.map { it.title }.contains("Switch to classic layout"))
        assertTrue(sections.first { it.title == "Language" }.rows.map { it.title }.contains("Markdown"))
        assertTrue(sections.first { it.title == "Settings" }.rows.map { it.title }.contains("Appearance preferences"))
        assertTrue(sections.first { it.title == "Tools" }.rows.map { it.title }.contains("Unique lines"))
    }

    @Test
    fun classicSettingsMenuUsesExpandableAppearanceSubmenu() {
        val submenu = classicSettingsSubmenus().single()

        assertEquals("Appearance", submenu.title)
        assertEquals(listOf("Toolbar preferences...", "Themes"), submenu.rows.map { it.title })
        assertTrue(submenu.expandable)
    }

    @Test
    fun preferencesHomeUsesCategoriesInsteadOfDumpingToolbarButtons() {
        val rows = preferencesHomeRows().map { it.title }

        assertEquals(listOf("Appearance", "Toolbar", "Editor"), rows)
        assertFalse(rows.contains("Paste"))
        assertFalse(rows.contains("Move line up"))
        assertFalse(rows.contains("Hidden Toolbar Buttons"))
    }

    @Test
    fun keyboardAccessoryMoreUsesMenuBarSheet() {
        assertEquals(MobileMenuSurface.MENU_BAR, keyboardAccessoryMoreSurface())
    }

    @Test
    fun hideKeyboardButtonShowsActiveStateWhenToolbarEditModeSuppressesIme() {
        val toggle = keyboardAccessoryToggleState(
            keyboardSuppressed = true,
            readOnly = false,
        )

        assertEquals("Hide Keyboard", toggle.label)
        assertTrue(toggle.active)
        assertTrue(toggle.enabled)
        assertEquals(false, shouldShowSoftKeyboardOnEditorFocus(readOnly = false, keyboardSuppressed = true))
    }

    @Test
    fun hideKeyboardButtonIsInactiveWhenKeyboardMayAppearNormally() {
        val toggle = keyboardAccessoryToggleState(
            keyboardSuppressed = false,
            readOnly = false,
        )

        assertEquals("Hide Keyboard", toggle.label)
        assertEquals(false, toggle.active)
        assertTrue(toggle.enabled)
        assertEquals(true, shouldShowSoftKeyboardOnEditorFocus(readOnly = false, keyboardSuppressed = false))
    }

    @Test
    fun staticCaretAndDeleteKeysRepeatOnHold() {
        assertTrue(accessoryStaticButtonRepeats("Left"))
        assertTrue(accessoryStaticButtonRepeats("Right"))
        assertTrue(accessoryStaticButtonRepeats("Up"))
        assertTrue(accessoryStaticButtonRepeats("Down"))
        assertTrue(accessoryStaticButtonRepeats("Delete"))
        assertFalse(accessoryStaticButtonRepeats("Shift"))
        assertEquals(360L, accessoryRepeatPressSpec.initialDelayMillis)
        assertEquals(170L, repeatDelayForIteration(0))
        assertTrue(repeatDelayForIteration(4) < repeatDelayForIteration(1))
        assertEquals(accessoryRepeatPressSpec.minimumDelayMillis, repeatDelayForIteration(100))
    }
}
