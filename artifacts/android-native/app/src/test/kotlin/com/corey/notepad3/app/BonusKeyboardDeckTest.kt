package com.corey.notepad3.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusKeyboardDeckTest {
    @Test
    fun deckHasFourIndexedPagesMatchingIosBonusDeckShape() {
        val deck = bonusKeyboardDeck()

        assertEquals(listOf("Nav", "123", "Sym", "More"), deck.pages.map { it.label })
        assertEquals(listOf("1/4", "2/4", "3/4", "4/4"), deck.pages.map { it.indexLabel })
        assertEquals(listOf(4, 4, 4, 4), deck.pages.map { it.rows.size })
        deck.pages.forEach { page ->
            assertEquals(16, page.keys.size)
            assertTrue(page.rows.all { row -> row.size == 4 })
        }
    }

    @Test
    fun firstPageContainsNavigationSelectionUndoRedoFindAndMoreActions() {
        val page = bonusKeyboardDeck().pages[0]

        assertEquals(
            listOf(
                "Word", "Pg Up", "Home", "All",
                "Line", "Pg Dn", "End", "Tab",
                "Undo", "Redo", "Up", "More",
                "Find", "Left", "Down", "Right",
            ),
            page.keys.map { it.label },
        )
        assertEquals(BonusKeyboardAction.SELECT_WORD, page.key("Word").action)
        assertEquals(BonusKeyboardAction.PAGE_UP, page.key("Pg Up").action)
        assertEquals(BonusKeyboardAction.SELECT_ALL, page.key("All").action)
        assertEquals(BonusKeyboardAction.TAB, page.key("Tab").action)
        assertEquals(BonusKeyboardAction.FIND, page.key("Find").action)
        assertEquals(BonusKeyboardAction.MORE, page.key("More").action)
    }

    @Test
    fun arrowKeysAndBackspaceAreRepeatableButRegularActionsAreNot() {
        val deck = bonusKeyboardDeck()
        val nav = deck.pages[0]

        listOf("Up", "Left", "Down", "Right").forEach { label ->
            assertTrue(nav.key(label).repeatable)
        }
        assertTrue(deck.sideKeys.key("Delete").repeatable)
        assertFalse(deck.sideKeys.key("Enter").repeatable)
        assertFalse(nav.key("Undo").repeatable)
        assertFalse(nav.key("Pg Up").repeatable)
    }

    @Test
    fun numberPageUsesTextKeysWithInsertedText() {
        val page = bonusKeyboardDeck().pages[1]

        assertEquals(
            listOf("/", "7", "8", "9", "*", "4", "5", "6", "-", "1", "2", "3", "+", "0", ".", ","),
            page.keys.map { it.label },
        )
        page.keys.forEach { key ->
            assertEquals(BonusKeyboardAction.INSERT_TEXT, key.action)
            assertEquals(key.label, key.insertText)
            assertEquals(BonusKeyboardContentMode.TEXT_ONLY, key.contentMode)
        }
    }

    @Test
    fun symbolPageUsesTextKeysWithInsertedText() {
        val page = bonusKeyboardDeck().pages[2]

        assertEquals(
            listOf("(", ")", "[", "]", "{", "}", "<", ">", "\"", "'", "=", "_", ":", ";", "\\", "|"),
            page.keys.map { it.label },
        )
        page.keys.forEach { key ->
            assertEquals(BonusKeyboardAction.INSERT_TEXT, key.action)
            assertEquals(key.label, key.insertText)
        }
    }

    @Test
    fun fourthPageContainsExtraEditorAndVisibilityActions() {
        val page = bonusKeyboardDeck().pages[3]

        assertEquals(
            listOf(
                "Duplicate", "Delete line", "Sort", "Trim",
                "Go to", "Open", "Compare", "Date",
                "Shift", "Read", "Kbd", "More",
                "Word", "Line", "All", "Find",
            ),
            page.keys.map { it.label },
        )
        assertEquals(BonusKeyboardAction.DUPLICATE_LINE, page.key("Duplicate").action)
        assertEquals(BonusKeyboardAction.DELETE_LINE, page.key("Delete line").action)
        assertEquals(BonusKeyboardAction.SORT_LINES, page.key("Sort").action)
        assertEquals(BonusKeyboardAction.TRIM_SPACES, page.key("Trim").action)
        assertEquals(BonusKeyboardAction.GOTO_LINE, page.key("Go to").action)
        assertEquals(BonusKeyboardAction.OPEN_DOCUMENTS, page.key("Open").action)
        assertEquals(BonusKeyboardAction.COMPARE, page.key("Compare").action)
        assertEquals(BonusKeyboardAction.INSERT_DATE, page.key("Date").action)
        assertEquals(BonusKeyboardAction.TOGGLE_SHIFT, page.key("Shift").action)
        assertEquals(BonusKeyboardAction.TOGGLE_READ_MODE, page.key("Read").action)
        assertEquals(BonusKeyboardAction.HIDE_BONUS_KEYBOARD, page.key("Kbd").action)
    }

    @Test
    fun actionsExposeStableIdsForUiDispatch() {
        assertEquals("select_word", BonusKeyboardAction.SELECT_WORD.id)
        assertEquals("insert_text", BonusKeyboardAction.INSERT_TEXT.id)
        assertEquals("delete_backward", BonusKeyboardAction.DELETE_BACKWARD.id)
        assertEquals("hide_bonus_keyboard", BonusKeyboardAction.HIDE_BONUS_KEYBOARD.id)
        assertEquals(
            BonusKeyboardAction.entries.size,
            BonusKeyboardAction.entries.map { it.id }.toSet().size,
        )
    }

    private fun BonusKeyboardPage.key(label: String): BonusKeyboardKey =
        keys.find { it.label == label }.also { assertNotNull("Missing key $label", it) }!!

    private fun List<BonusKeyboardKey>.key(label: String): BonusKeyboardKey =
        find { it.label == label }.also { assertNotNull("Missing key $label", it) }!!
}
