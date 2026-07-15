package com.scorigami.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Persists the user's theme choice (dark is the default, matching the app's original
 * dark-only design). Backed by Preferences DataStore; injected via Hilt constructor
 * injection like the rest of the codebase.
 */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val darkKey = booleanPreferencesKey("dark_theme")

    val isDark: Flow<Boolean> = context.settingsDataStore.data.map { it[darkKey] ?: true }

    /**
     * One-time synchronous read at process start, before setContent — avoids rendering
     * the first frame in the wrong theme. The prefs file holds a single boolean; the
     * blocking read is single-digit milliseconds.
     */
    fun isDarkBlocking(): Boolean = runBlocking { isDark.first() }

    suspend fun setDark(dark: Boolean) {
        context.settingsDataStore.edit { it[darkKey] = dark }
    }
}
