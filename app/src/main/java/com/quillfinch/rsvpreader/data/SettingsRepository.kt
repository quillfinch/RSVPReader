package com.quillfinch.rsvpreader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reader_settings")

data class ReaderSettings(
    val wpm: Int = DEFAULT_WPM,
    /** ARGB colors stored as Long, e.g. 0xFFE6E1E9. */
    val textColor: Long = DEFAULT_TEXT,
    val backgroundColor: Long = DEFAULT_BACKGROUND,
    val focusColor: Long = DEFAULT_FOCUS,
) {
    companion object {
        const val DEFAULT_WPM = 300
        const val DEFAULT_TEXT = 0xFFE6E1E9
        const val DEFAULT_BACKGROUND = 0xFF000000
        const val DEFAULT_FOCUS = 0xFFFF5252
    }
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<ReaderSettings> = context.dataStore.data.map { prefs ->
        ReaderSettings(
            wpm = prefs[KEY_WPM] ?: ReaderSettings.DEFAULT_WPM,
            textColor = prefs[KEY_TEXT] ?: ReaderSettings.DEFAULT_TEXT,
            backgroundColor = prefs[KEY_BACKGROUND] ?: ReaderSettings.DEFAULT_BACKGROUND,
            focusColor = prefs[KEY_FOCUS] ?: ReaderSettings.DEFAULT_FOCUS,
        )
    }

    suspend fun setWpm(value: Int) = context.dataStore.edit { it[KEY_WPM] = value.coerceIn(MIN_WPM, MAX_WPM) }

    suspend fun setTextColor(value: Long) = context.dataStore.edit { it[KEY_TEXT] = value }

    suspend fun setBackgroundColor(value: Long) = context.dataStore.edit { it[KEY_BACKGROUND] = value }

    suspend fun setFocusColor(value: Long) = context.dataStore.edit { it[KEY_FOCUS] = value }

    suspend fun resetColors() = context.dataStore.edit {
        it.remove(KEY_TEXT)
        it.remove(KEY_BACKGROUND)
        it.remove(KEY_FOCUS)
    }

    companion object {
        const val MIN_WPM = 100
        const val MAX_WPM = 1000

        private val KEY_WPM = intPreferencesKey("wpm")
        private val KEY_TEXT = longPreferencesKey("text_color")
        private val KEY_BACKGROUND = longPreferencesKey("background_color")
        private val KEY_FOCUS = longPreferencesKey("focus_color")
    }
}
