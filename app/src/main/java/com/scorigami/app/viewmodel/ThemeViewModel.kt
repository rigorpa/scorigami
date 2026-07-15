package com.scorigami.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.scorigami.app.data.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exposes the persisted theme choice to the HomeScreen toggle. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemeRepository
) : ViewModel() {

    val isDark: StateFlow<Boolean> = repo.isDark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggle() {
        viewModelScope.launch { repo.setDark(!isDark.value) }
    }
}
