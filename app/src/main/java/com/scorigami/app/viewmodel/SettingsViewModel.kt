package com.scorigami.app.viewmodel

import androidx.lifecycle.ViewModel
import com.scorigami.app.settings.SettingsRepository
import com.scorigami.app.ui.theme.AppFontSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    val fontSize: StateFlow<AppFontSize> = repository.fontSize
    fun setFontSize(size: AppFontSize) = repository.setFontSize(size)
}
