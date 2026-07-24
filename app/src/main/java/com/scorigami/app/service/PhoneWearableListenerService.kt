package com.scorigami.app.service

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.scorigami.app.sync.WearSyncManager
import com.scorigami.shared.db.dao.C1xDao
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.ObDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.C1xEntity
import com.scorigami.shared.db.entity.ObEntity
import com.scorigami.shared.db.entity.ScoreEntity
import com.scorigami.shared.sync.RoundStateBuilder
import com.scorigami.shared.sync.ScoreUpdateMessage
import com.scorigami.shared.sync.StatUpdateMessage
import com.scorigami.shared.sync.SyncKeys
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class PhoneWearableListenerService : WearableListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WearServiceEntryPoint {
        fun scoreDao(): ScoreDao
        fun roundDao(): RoundDao
        fun courseDao(): CourseDao
        fun playerDao(): PlayerDao
        fun obDao(): ObDao
        fun c1xDao(): C1xDao
        fun wearSyncManager(): WearSyncManager
    }

    private val ep by lazy {
        EntryPointAccessors.fromApplication(applicationContext, WearServiceEntryPoint::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            SyncKeys.SCORE_UPDATE_MSG -> {
                val msg = Json.decodeFromString<ScoreUpdateMessage>(String(messageEvent.data))
                scope.launch {
                    ep.scoreDao().upsertScore(
                        ScoreEntity(
                            roundId = msg.roundId,
                            playerId = msg.playerId,
                            holeNumber = msg.holeNumber,
                            throws = msg.throws
                        )
                    )
                    pushUpdatedState(msg.roundId, msg.viewingHole)
                }
            }
            SyncKeys.STAT_UPDATE_MSG -> {
                val msg = Json.decodeFromString<StatUpdateMessage>(String(messageEvent.data))
                scope.launch {
                    // Mirrors RoundViewModel.setOb/setC1x: count <= 0 clears the row.
                    when (msg.stat) {
                        "ob" -> if (msg.count <= 0) {
                            ep.obDao().deleteOb(msg.roundId, msg.playerId, msg.holeNumber)
                        } else {
                            ep.obDao().upsertOb(ObEntity(msg.roundId, msg.playerId, msg.holeNumber, msg.count))
                        }
                        "c1x" -> if (msg.count <= 0) {
                            ep.c1xDao().deleteC1x(msg.roundId, msg.playerId, msg.holeNumber)
                        } else {
                            ep.c1xDao().upsertC1x(C1xEntity(msg.roundId, msg.playerId, msg.holeNumber, msg.count))
                        }
                    }
                    pushUpdatedState(msg.roundId, msg.viewingHole)
                }
            }
        }
    }

    private suspend fun pushUpdatedState(roundId: Long, currentHole: Int) {
        val round = ep.roundDao().getRoundById(roundId) ?: return
        val courseWithHoles = ep.courseDao().getCourseWithHoles(round.courseId) ?: return
        val players = ep.playerDao().getPlayersForRound(roundId)
        val scores = ep.scoreDao().getScoresForRoundSnapshot(roundId)
        val scoreMap = scores.associate { Pair(it.playerId, it.holeNumber) to it.throws }
        val obMap = ep.obDao().getObForRoundSnapshot(roundId)
            .associate { Pair(it.playerId, it.holeNumber) to it.count }
        val c1xMap = ep.c1xDao().getC1xForRoundSnapshot(roundId)
            .associate { Pair(it.playerId, it.holeNumber) to it.count }

        val roundState = RoundStateBuilder.build(
            roundId = roundId,
            courseName = courseWithHoles.course.name,
            currentHole = currentHole,
            holes = courseWithHoles.holes,
            players = players,
            scores = scoreMap,
            obCounts = obMap,
            c1xCounts = c1xMap,
            startHole = round.startHole
        )
        ep.wearSyncManager().pushRoundState(roundState)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
