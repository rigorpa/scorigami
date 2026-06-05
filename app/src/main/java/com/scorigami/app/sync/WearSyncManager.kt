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
    private val dataClient    = Wearable.getDataClient(context)
    private val nodeClient    = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun pushRoundState(state: RoundState) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val json = Json.encodeToString(state)
                val bytes = json.toByteArray(Charsets.UTF_8)
                val request = PutDataMapRequest.create(SyncKeys.ROUND_STATE_PATH).apply {
                    dataMap.putString("state", json)
                    dataMap.putLong("ts", System.currentTimeMillis())
                }
                dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, SyncKeys.ROUND_STATE_MSG, bytes).await()
                }
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
