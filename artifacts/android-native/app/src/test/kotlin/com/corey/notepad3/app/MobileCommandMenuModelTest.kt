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
                "Virtual mouse",
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
        val searchRows = sections.first { it.title == "Search" }.rows.map { it.title }
        assertTrue(searchRows.contains("Find/Replace"))
        assertFalse(searchRows.contains("Find and replace"))
        assertFalse(searchRows.contains("Replace"))
        assertTrue(sections.first { it.title == "View" }.rows.map { it.title }.contains("Switch to classic layout"))
        assertTrue(sections.first { it.title == "Language" }.rows.map { it.title }.contains("Markdown"))
        assertTrue(sections.first { it.title == "Settings" }.rows.map { it.title }.contains("Appearance preferences"))
        assertTrue(sections.first { it.title == "Tools" }.rows.map { it.title }.contains("Unique lines"))
    }

    @Test
    fun classicSettingsMenuUsesExpandableAppearanceSubmenu() {
        val submenu = classicSettingsSubmenus().single()

        assertEquals("Appearance", submenu.title)
        assertEquals(
            listOf(
                "Appearance preferences...",
                "Top toolbar preferences...",
                "Bottom toolbar preferences...",
                "Themes",
            ),
            submenu.rows.map { it.title },
        )
        assertTrue(submenu.expandable)
    }

    @Test
    fun preferencesHomeUsesCategoriesInsteadOfDumpingToolbarButtons() {
        val rows = preferencesHomeRows().map { it.title }

        assertEquals(listOf("Appearance", "Top Toolbar", "Bottom Toolbar", "Editor"), rows)
        assertFalse(rows.contains("Paste"))
        assertFalse(rows.contains("Move line up"))
        assertFalse(rows.contains("Hidden Toolbar Buttons"))
    }

    @Test
    fun preferencesBackReturnsFromSubpageBeforeDismissing() {
        assertEquals(PreferencesDestination.GENERAL, preferencesBackDestination(PreferencesDestination.APPEARANCE))
        assertEquals(PreferencesDestination.GENERAL, preferencesBackDestination(PreferencesDestination.TOOLBAR))
        assertEquals(PreferencesDestination.GENERAL, preferencesBackDestination(PreferencesDestination.EDITOR))
        assertEquals(null, preferencesBackDestination(PreferencesDestination.GENERAL))
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

        assertEquals("Show", toggle.label)
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

        assertEquals("Hide", toggle.label)
        assertEquals(false, toggle.active)
        assertTrue(toggle.enabled)
        assertEquals(true, shouldShowSoftKeyboardOnEditorFocus(readOnly = false, keyboardSuppressed = false))
    }

    @Test
    fun trackpadToggleOpensTrackpadWithoutChangingLayoutState() {
        assertTrue(toggledTrackpadVisibility(visible = false))
    }

    @Test
    fun trackpadToggleClosesTrackpadWithoutChangingLayoutState() {
        assertFalse(toggledTrackpadVisibility(visible = true))
    }

    @Test
    fun menusDismissOnOutsideTapWhenVirtualMouseIsOff() {
        assertTrue(shouldDismissMenusOnOutsidePointer(trackpadActive = false))
    }

    @Test
    fun menusStayOpenForVirtualMouseTouches() {
        assertFalse(shouldDismissMenusOnOutsidePointer(trackpadActive = true))
    }

    @Test
    fun staticCaretAndDeleteKeysRepeatOnHold() {
        assertTrue(accessoryStaticButtonRepeats("Left"))
        assertTrue(accessoryStaticButtonRepeats("Right"))
        assertTrue(accessoryStaticButtonRepeats("Up"))
        assertTrue(accessoryStaticButtonRepeats("Down"))
        assertTrue(accessoryStaticButtonRepeats("Delete"))
        assertTrue(accessoryStaticButtonRepeats("Backspace"))
        assertFalse(accessoryStaticButtonRepeats("Shift"))
        assertEquals(360L, accessoryRepeatPressSpec.initialDelayMillis)
        assertEquals(170L, repeatDelayForIteration(0))
        assertTrue(repeatDelayForIteration(4) < repeatDelayForIteration(1))
        assertEquals(accessoryRepeatPressSpec.minimumDelayMillis, repeatDelayForIteration(100))
    }

    @Test
    fun keyboardAccessoryStaysInNativeToolbarModel() {
        assertFalse(keyboardAccessoryUsesDeckSurface())
        assertEquals(MobileMenuSurface.MENU_BAR, keyboardAccessoryMoreSurface())
        assertTrue(accessoryStaticButtonRepeats("Left"))
        assertTrue(accessoryStaticButtonRepeats("Backspace"))
        assertFalse(accessoryStaticButtonRepeats("Deck"))
        assertFalse(accessoryStaticButtonRepeats("Home"))
        assertFalse(accessoryStaticButtonRepeats("Pg Up"))
        assertFalse(accessoryStaticButtonRepeats("Windows"))
    }

    @Test
    fun documentTabsUseCompactWidthsAndTruncateLongNames() {
        assertEquals("notes.txt", documentTabDisplayTitle("notes.txt"))
        assertEquals("very-long-filename-for-...", documentTabDisplayTitle("very-long-filename-for-notes-and-more.txt"))

        assertEquals(86, documentTabWidthDp("notes.txt"))
        assertEquals(184, documentTabWidthDp("very-long-filename-for-notes-and-more.txt"))
    }

    @Test
    fun mobileLayoutUsesTabsFlyoutInsteadOfPersistentDocumentStrip() {
        assertFalse(shouldShowPersistentDocumentStrip(EditorLayoutMode.MOBILE))
        assertTrue(shouldShowPersistentDocumentStrip(EditorLayoutMode.CLASSIC))
    }
}
