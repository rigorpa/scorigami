package com.scorigami.wear.sync

import com.scorigami.shared.sync.RoundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object RoundStateHolder {
    private val _state = MutableStateFlow<RoundState?>(null)
    val state: StateFlow<RoundState?> = _state

    fun update(state: RoundState?) {
        _state.value = state
    }
}
