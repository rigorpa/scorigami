package com.scorigami.app.settings

import android.content.Context
import com.scorigami.app.ui.theme.AppFontSize
import com.scorigami.app.ui.theme.CurrentFontSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App settings persisted in SharedPreferences (deliberately not DataStore — one enum
 * value doesn't justify a new dependency). The singleton's [fontSize] StateFlow is the
 * live source of truth: MainActivity collects it into ScorigamiTheme, and the Home
 * screen's Font Size dialog writes it, so changes re-theme the whole app instantly.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _fontSize = MutableStateFlow(
        prefs.getString(KEY_FONT_SIZE, null)
            ?.let { saved -> AppFontSize.entries.firstOrNull { it.name == saved } }
            ?: CurrentFontSize
    )
    val fontSize: StateFlow<AppFontSize> = _fontSize.asStateFlow()

    fun setFontSize(size: AppFontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, size.name).apply()
        _fontSize.value = size
    }

    private companion object {
        const val KEY_FONT_SIZE = "font_size"
    }
}
