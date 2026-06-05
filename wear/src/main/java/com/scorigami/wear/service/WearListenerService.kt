package com.scorigami.wear.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.scorigami.shared.sync.RoundState
import com.scorigami.shared.sync.SyncKeys
import com.scorigami.wear.sync.RoundStateHolder
import kotlinx.serialization.json.Json

class WearListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("WearListener", "onDataChanged fired with ${dataEvents.count} event(s)")
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: return@forEach
            Log.d("WearListener", "event type=${event.type} path=$path")
            when {
                path == SyncKeys.ROUND_STATE_PATH && event.type == DataEvent.TYPE_CHANGED -> {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val json = dataMap.getString("state") ?: return@forEach
                    val state = Json.decodeFromString<RoundState>(json)
                    Log.d("WearListener", "RoundState received: roundId=${state.roundId} hole=${state.currentHole} players=${state.players.size}")
                    RoundStateHolder.update(state)
                }
                path == SyncKeys.ROUND_STATE_PATH && event.type == DataEvent.TYPE_DELETED -> {
                    Log.d("WearListener", "RoundState cleared")
                    RoundStateHolder.update(null)
                }
            }
        }
    }
}
