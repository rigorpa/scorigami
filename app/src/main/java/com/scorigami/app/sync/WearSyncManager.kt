package com.scorigami.app.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.scorigami.shared.sync.RoundState
import com.scorigami.shared.sync.SyncKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun pushRoundState(state: RoundState) {
        scope.launch {
            try {
                val json = Json.encodeToString(state)
                val request = PutDataMapRequest.create(SyncKeys.ROUND_STATE_PATH).apply {
                    dataMap.putString("state", json)
                    dataMap.putLong("ts", System.currentTimeMillis())
                }
                dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
            } catch (e: Exception) {
                Log.w("WearSync", "pushRoundState failed", e)
            }
        }
    }

    fun clearRoundState() {
        scope.launch {
            try {
                dataClient.deleteDataItems(Uri.parse("wear://*${SyncKeys.ROUND_STATE_PATH}")).await()
            } catch (e: Exception) {
                Log.w("WearSync", "clearRoundState failed", e)
            }
        }
    }
}
