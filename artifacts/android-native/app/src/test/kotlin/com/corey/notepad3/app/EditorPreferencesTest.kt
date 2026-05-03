package com.corey.notepad3.app

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorPreferencesTest {
    @Test
    fun controllerStartsWithTheStoredLayoutMode() {
        val preferences = InMemoryEditorPreferences(layoutMode = EditorLayoutMode.CLASSIC)
        val controller = EditorPreferenceController(preferences)

        assertEquals(EditorLayoutMode.CLASSIC, controller.layoutMode.value)
    }

    @Test
    fun controllerPersistsLayoutModeChanges() {
        val preferences = InMemoryEditorPreferences()
        val controller = EditorPreferenceController(preferences)

        controller.setLayoutMode(EditorLayoutMode.CLASSIC)

        assertEquals(EditorLayoutMode.CLASSIC, preferences.displayOptions.value.layoutMode)
        assertEquals(EditorLayoutMode.CLASSIC, controller.displayOptions.value.layoutMode)
    }

    @Test
    fun controllerTogglesLayoutMode() {
        val controller = EditorPreferenceController(InMemoryEditorPreferences())

        controller.toggleLayoutMode()
        assertEquals(EditorLayoutMode.CLASSIC, controller.displayOptions.value.layoutMode)

        controller.toggleLayoutMode()
        assertEquals(EditorLayoutMode.MOBILE, controller.displayOptions.value.layoutMode)
    }

    @Test
    fun controllerAdjustsEditorDisplayOptionsWithinUsableBounds() {
        val controller = EditorPreferenceController(InMemoryEditorPreferences())

        controller.adjustFontSize(40)
        controller.toggleWordWrap()
        controller.toggleLineNumbers()
        controller.toggleAccessoryBar()

        assertEquals(24, controller.displayOptions.value.fontSizeSp)
        assertEquals(false, controller.displayOptions.value.wordWrap)
        assertEquals(false, controller.displayOptions.value.lineNumbers)
        assertEquals(false, controller.displayOptions.value.accessoryBar)

        controller.adjustFontSize(-40)

        assertEquals(11, controller.displayOptions.value.fontSizeSp)
    }
}
