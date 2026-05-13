package com.corey.notepad3.app

enum class BonusKeyboardAction(val id: String) {
    SELECT_WORD("select_word"),
    SELECT_LINE("select_line"),
    SELECT_ALL("select_all"),
    PAGE_UP("page_up"),
    PAGE_DOWN("page_down"),
    HOME("home"),
    END("end"),
    TAB("tab"),
    UNDO("undo"),
    REDO("redo"),
    ARROW_UP("arrow_up"),
    ARROW_LEFT("arrow_left"),
    ARROW_DOWN("arrow_down"),
    ARROW_RIGHT("arrow_right"),
    FIND("find"),
    MORE("more"),
    INSERT_TEXT("insert_text"),
    DUPLICATE_LINE("duplicate_line"),
    DELETE_LINE("delete_line"),
    SORT_LINES("sort_lines"),
    TRIM_SPACES("trim_spaces"),
    GOTO_LINE("goto_line"),
    OPEN_DOCUMENTS("open_documents"),
    COMPARE("compare"),
    INSERT_DATE("insert_date"),
    TOGGLE_SHIFT("toggle_shift"),
    TOGGLE_READ_MODE("toggle_read_mode"),
    HIDE_BONUS_KEYBOARD("hide_bonus_keyboard"),
    DELETE_BACKWARD("delete_backward"),
    ENTER("enter"),
}

enum class BonusKeyboardContentMode {
    ICON_AND_TEXT,
    TEXT_ONLY,
}

data class BonusKeyboardKey(
    val action: BonusKeyboardAction,
    val label: String,
    val symbol: String,
    val contentMode: BonusKeyboardContentMode = BonusKeyboardContentMode.ICON_AND_TEXT,
    val repeatable: Boolean = false,
    val insertText: String? = null,
)

data class BonusKeyboardPage(
    val label: String,
    val indexLabel: String,
    val keys: List<BonusKeyboardKey>,
) {
    val rows: List<List<BonusKeyboardKey>> = keys.chunked(BONUS_KEYBOARD_KEYS_PER_ROW)
}

data class BonusKeyboardDeck(
    val pages: List<BonusKeyboardPage>,
    val sideKeys: List<BonusKeyboardKey>,
)

fun bonusKeyboardDeck(): BonusKeyboardDeck =
    BonusKeyboardDeck(
        pages = listOf(
            BonusKeyboardPage(
                label = "Nav",
                indexLabel = "1/4",
                keys = listOf(
                    key(BonusKeyboardAction.SELECT_WORD, "textformat.abc", "Word"),
                    key(BonusKeyboardAction.PAGE_UP, "arrow.up.to.line", "Pg Up"),
                    key(BonusKeyboardAction.HOME, "house", "Home"),
                    key(BonusKeyboardAction.SELECT_ALL, "character.textbox", "All"),
                    key(BonusKeyboardAction.SELECT_LINE, "text.line.first.and.arrowtriangle.forward", "Line"),
                    key(BonusKeyboardAction.PAGE_DOWN, "arrow.down.to.line", "Pg Dn"),
                    key(BonusKeyboardAction.END, "line.3.horizontal", "End"),
                    key(BonusKeyboardAction.TAB, "arrow.right.to.line", "Tab"),
                    key(BonusKeyboardAction.UNDO, "arrow.uturn.backward", "Undo"),
                    key(BonusKeyboardAction.REDO, "arrow.uturn.forward", "Redo"),
                    key(BonusKeyboardAction.ARROW_UP, "arrow.up", "Up", repeatable = true),
                    key(BonusKeyboardAction.MORE, "ellipsis.circle", "More"),
                    key(BonusKeyboardAction.FIND, "magnifyingglass", "Find"),
                    key(BonusKeyboardAction.ARROW_LEFT, "arrow.left", "Left", repeatable = true),
                    key(BonusKeyboardAction.ARROW_DOWN, "arrow.down", "Down", repeatable = true),
                    key(BonusKeyboardAction.ARROW_RIGHT, "arrow.right", "Right", repeatable = true),
                ),
            ),
            BonusKeyboardPage(
                label = "123",
                indexLabel = "2/4",
                keys = listOf("/", "7", "8", "9", "*", "4", "5", "6", "-", "1", "2", "3", "+", "0", ".", ",")
                    .map(::textKey),
            ),
            BonusKeyboardPage(
                label = "Sym",
                indexLabel = "3/4",
                keys = listOf("(", ")", "[", "]", "{", "}", "<", ">", "\"", "'", "=", "_", ":", ";", "\\", "|")
                    .map(::textKey),
            ),
            BonusKeyboardPage(
                label = "More",
                indexLabel = "4/4",
                keys = listOf(
                    key(BonusKeyboardAction.DUPLICATE_LINE, "plus.square.on.square", "Duplicate"),
                    key(BonusKeyboardAction.DELETE_LINE, "minus.square", "Delete line"),
                    key(BonusKeyboardAction.SORT_LINES, "arrow.up.arrow.down", "Sort"),
                    key(BonusKeyboardAction.TRIM_SPACES, "scissors.circle", "Trim"),
                    key(BonusKeyboardAction.GOTO_LINE, "arrow.down.to.line", "Go to"),
                    key(BonusKeyboardAction.OPEN_DOCUMENTS, "folder", "Open"),
                    key(BonusKeyboardAction.COMPARE, "rectangle.split.1x2", "Compare"),
                    key(BonusKeyboardAction.INSERT_DATE, "clock", "Date"),
                    key(BonusKeyboardAction.TOGGLE_SHIFT, "shift", "Shift"),
                    key(BonusKeyboardAction.TOGGLE_READ_MODE, "eye.slash", "Read"),
                    key(BonusKeyboardAction.HIDE_BONUS_KEYBOARD, "keyboard", "Kbd"),
                    key(BonusKeyboardAction.MORE, "ellipsis.circle", "More"),
                    key(BonusKeyboardAction.SELECT_WORD, "textformat.abc", "Word"),
                    key(BonusKeyboardAction.SELECT_LINE, "text.line.first.and.arrowtriangle.forward", "Line"),
                    key(BonusKeyboardAction.SELECT_ALL, "character.textbox", "All"),
                    key(BonusKeyboardAction.FIND, "magnifyingglass", "Find"),
                ),
            ),
        ),
        sideKeys = listOf(
            key(BonusKeyboardAction.DELETE_BACKWARD, "delete.left", "Delete", repeatable = true),
            key(BonusKeyboardAction.ENTER, "return", "Enter"),
        ),
    )

private const val BONUS_KEYBOARD_KEYS_PER_ROW = 4

private fun key(
    action: BonusKeyboardAction,
    symbol: String,
    label: String,
    repeatable: Boolean = false,
): BonusKeyboardKey =
    BonusKeyboardKey(
        action = action,
        label = label,
        symbol = symbol,
        repeatable = repeatable,
    )

private fun textKey(text: String): BonusKeyboardKey =
    BonusKeyboardKey(
        action = BonusKeyboardAction.INSERT_TEXT,
        label = text,
        symbol = text,
        contentMode = BonusKeyboardContentMode.TEXT_ONLY,
        insertText = text,
    )
