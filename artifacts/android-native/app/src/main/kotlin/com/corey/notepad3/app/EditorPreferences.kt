package com.corey.notepad3.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorDisplayOptions(
    val layoutMode: EditorLayoutMode = EditorLayoutMode.MOBILE,
    val fontSizeSp: Int = DEFAULT_FONT_SIZE_SP,
    val wordWrap: Boolean = true,
    val lineNumbers: Boolean = true,
    val accessoryBar: Boolean = true,
) {
    fun withFontDelta(delta: Int): EditorDisplayOptions =
        copy(fontSizeSp = (fontSizeSp + delta).coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP))

    companion object {
        const val MIN_FONT_SIZE_SP = 11
        const val DEFAULT_FONT_SIZE_SP = 15
        const val MAX_FONT_SIZE_SP = 24
    }
}

interface EditorPreferences {
    val displayOptions: StateFlow<EditorDisplayOptions>
    val layoutMode: StateFlow<EditorLayoutMode>
    fun setDisplayOptions(options: EditorDisplayOptions)
    fun setLayoutMode(mode: EditorLayoutMode)
}

class EditorPreferenceController(private val preferences: EditorPreferences) {
    val displayOptions: StateFlow<EditorDisplayOptions> = preferences.displayOptions
    val layoutMode: StateFlow<EditorLayoutMode> = preferences.layoutMode

    fun setDisplayOptions(options: EditorDisplayOptions) {
        preferences.setDisplayOptions(options)
    }

    fun setLayoutMode(mode: EditorLayoutMode) {
        preferences.setLayoutMode(mode)
    }

    fun toggleLayoutMode() {
        setLayoutMode(layoutMode.value.toggled())
    }

    fun adjustFontSize(delta: Int) {
        setDisplayOptions(displayOptions.value.withFontDelta(delta))
    }

    fun toggleWordWrap() {
        setDisplayOptions(displayOptions.value.copy(wordWrap = !displayOptions.value.wordWrap))
    }

    fun toggleLineNumbers() {
        setDisplayOptions(displayOptions.value.copy(lineNumbers = !displayOptions.value.lineNumbers))
    }

    fun toggleAccessoryBar() {
        setDisplayOptions(displayOptions.value.copy(accessoryBar = !displayOptions.value.accessoryBar))
    }
}

class InMemoryEditorPreferences(
    layoutMode: EditorLayoutMode = EditorLayoutMode.MOBILE,
) : EditorPreferences {
    private val _displayOptions = MutableStateFlow(EditorDisplayOptions(layoutMode = layoutMode))
    override val displayOptions: StateFlow<EditorDisplayOptions> = _displayOptions.asStateFlow()

    private val _layoutMode = MutableStateFlow(layoutMode)
    override val layoutMode: StateFlow<EditorLayoutMode> = _layoutMode.asStateFlow()

    override fun setDisplayOptions(options: EditorDisplayOptions) {
        _displayOptions.value = options
        _layoutMode.value = options.layoutMode
    }

    override fun setLayoutMode(mode: EditorLayoutMode) {
        setDisplayOptions(displayOptions.value.copy(layoutMode = mode))
    }
}

class AndroidEditorPreferences(context: Context) : EditorPreferences {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notepad3pp", Context.MODE_PRIVATE)

    private val _displayOptions = MutableStateFlow(decodeDisplayOptions())
    override val displayOptions: StateFlow<EditorDisplayOptions> = _displayOptions.asStateFlow()

    private val _layoutMode = MutableStateFlow(_displayOptions.value.layoutMode)
    override val layoutMode: StateFlow<EditorLayoutMode> = _layoutMode.asStateFlow()

    override fun setDisplayOptions(options: EditorDisplayOptions) {
        prefs.edit()
            .putString(KEY_LAYOUT_MODE, options.layoutMode.storageName)
            .putInt(KEY_FONT_SIZE_SP, options.fontSizeSp)
            .putBoolean(KEY_WORD_WRAP, options.wordWrap)
            .putBoolean(KEY_LINE_NUMBERS, options.lineNumbers)
            .putBoolean(KEY_ACCESSORY_BAR, options.accessoryBar)
            .apply()
        _displayOptions.value = options
        _layoutMode.value = options.layoutMode
    }

    override fun setLayoutMode(mode: EditorLayoutMode) {
        setDisplayOptions(displayOptions.value.copy(layoutMode = mode))
    }

    private fun decodeDisplayOptions(): EditorDisplayOptions =
        EditorDisplayOptions(
            layoutMode = decodeLayoutMode(),
            fontSizeSp = prefs.getInt(KEY_FONT_SIZE_SP, EditorDisplayOptions.DEFAULT_FONT_SIZE_SP)
                .coerceIn(EditorDisplayOptions.MIN_FONT_SIZE_SP, EditorDisplayOptions.MAX_FONT_SIZE_SP),
            wordWrap = prefs.getBoolean(KEY_WORD_WRAP, true),
            lineNumbers = prefs.getBoolean(KEY_LINE_NUMBERS, true),
            accessoryBar = prefs.getBoolean(KEY_ACCESSORY_BAR, true),
        )

    private fun decodeLayoutMode(): EditorLayoutMode =
        prefs.getString(KEY_LAYOUT_MODE, null)
            ?.let(EditorLayoutMode::fromStorageName)
            ?: EditorLayoutMode.MOBILE

    companion object {
        private const val KEY_LAYOUT_MODE = "notepad3pp.layoutMode"
        private const val KEY_FONT_SIZE_SP = "notepad3pp.fontSizeSp"
        private const val KEY_WORD_WRAP = "notepad3pp.wordWrap"
        private const val KEY_LINE_NUMBERS = "notepad3pp.lineNumbers"
        private const val KEY_ACCESSORY_BAR = "notepad3pp.accessoryBar"
    }
}
