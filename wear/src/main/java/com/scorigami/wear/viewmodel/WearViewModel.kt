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
    val currentHole: Int = 1,
    // False until the Data Layer has been read at least once. Lets the UI tell
    // "no active round" apart from "haven't checked yet" (avoids a NoRoundScreen
    // flash on cold start when a round is actually active).
    val loaded: Boolean = false
)

@HiltViewModel
class WearViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentHole = MutableStateFlow(1)
    private val _loaded = MutableStateFlow(false)
    private var pollingJob: Job? = null
    private var lastKnownRoundId: Long? = null

    init {
        viewModelScope.launch {
            RoundStateHolder.state.collect { roundState ->
                val incomingId = roundState?.roundId
                if (incomingId != null && incomingId != lastKnownRoundId) {
                    _currentHole.value = 1
                }
                lastKnownRoundId = incomingId
            }
        }
        // Resolve the active round once up front (before polling starts in onResume) so
        // the UI can distinguish "no round" from "not loaded yet" on cold start.
        viewModelScope.launch { refreshFromDataLayer() }
    }

    /** Reads the current round snapshot from the Data Layer and publishes it (or null). */
    private suspend fun refreshFromDataLayer() {
        try {
            val dataItems = Wearable.getDataClient(context).getDataItems().await()
            var round: RoundState? = null
            dataItems.forEach { item ->
                if (item.uri.path == SyncKeys.ROUND_STATE_PATH) {
                    val json = DataMapItem.fromDataItem(item).dataMap.getString("state")
                    if (json != null) round = Json.decodeFromString<RoundState>(json)
                }
            }
            dataItems.release()
            RoundStateHolder.update(round)
        } catch (e: Exception) {
            Log.w("WearViewModel", "data layer read failed", e)
        } finally {
            _loaded.value = true
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                refreshFromDataLayer()
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
        _currentHole,
        _loaded
    ) { roundState, currentHole, loaded ->
        val hole = roundState?.let { currentHole.coerceIn(1, it.totalHoles) } ?: currentHole
        WearUiState(roundState = roundState, currentHole = hole, loaded = loaded)
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
