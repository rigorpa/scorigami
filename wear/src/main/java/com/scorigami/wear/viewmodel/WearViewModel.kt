package com.scorigami.wear.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.scorigami.shared.sync.RoundState
import com.scorigami.shared.sync.ScoreUpdateMessage
import com.scorigami.shared.sync.SyncKeys
import com.scorigami.wear.sync.RoundStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class WearUiState(
    val roundState: RoundState? = null,
    val currentHole: Int = 1
)

@HiltViewModel
class WearViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentHole = MutableStateFlow(1)
    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val dataItems = Wearable.getDataClient(context).getDataItems().await()
                    var found = false
                    dataItems.forEach { item ->
                        if (item.uri.path == SyncKeys.ROUND_STATE_PATH) {
                            found = true
                            val json = DataMapItem.fromDataItem(item).dataMap.getString("state")
                            if (json != null) {
                                RoundStateHolder.update(Json.decodeFromString<RoundState>(json))
                            }
                        }
                    }
                    if (!found) RoundStateHolder.update(null)
                    dataItems.release()
                } catch (e: Exception) {
                    Log.w("WearViewModel", "poll failed", e)
                }
                delay(2000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    val uiState: StateFlow<WearUiState> = combine(
        RoundStateHolder.state,
        _currentHole
    ) { roundState, currentHole ->
        val hole = roundState?.let { currentHole.coerceIn(1, it.totalHoles) } ?: currentHole
        WearUiState(roundState = roundState, currentHole = hole)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WearUiState())

    fun navigateToHole(hole: Int) {
        _currentHole.value = hole
    }

    fun sendScoreUpdate(roundId: Long, playerId: Long, holeNumber: Int, throws: Int) {
        viewModelScope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                val msg = ScoreUpdateMessage(
                    roundId = roundId,
                    playerId = playerId,
                    holeNumber = holeNumber,
                    throws = throws,
                    viewingHole = _currentHole.value
                )
                val bytes = Json.encodeToString(msg).toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, SyncKeys.SCORE_UPDATE_MSG, bytes)
                        .await()
                }
            } catch (e: Exception) {
                Log.w("WearViewModel", "Failed to send score update to phone", e)
            }
        }
    }
}
