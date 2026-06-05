package com.scorigami.app.service

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.scorigami.app.sync.WearSyncManager
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.ScoreEntity
import com.scorigami.shared.sync.PlayerState
import com.scorigami.shared.sync.RoundState
import com.scorigami.shared.sync.ScoreUpdateMessage
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
        fun wearSyncManager(): WearSyncManager
    }

    private val ep by lazy {
        EntryPointAccessors.fromApplication(applicationContext, WearServiceEntryPoint::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != SyncKeys.SCORE_UPDATE_MSG) return
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

    private suspend fun pushUpdatedState(roundId: Long, currentHole: Int) {
        val round = ep.roundDao().getRoundById(roundId) ?: return
        val courseWithHoles = ep.courseDao().getCourseWithHoles(round.courseId) ?: return
        val players = ep.playerDao().getPlayersForRound(roundId)
        val scores = ep.scoreDao().getScoresForRoundSnapshot(roundId)
        val scoreMap = scores.associate { Pair(it.playerId, it.holeNumber) to it.throws }

        val roundState = RoundState(
            roundId = roundId,
            courseName = courseWithHoles.course.name,
            currentHole = currentHole,
            totalHoles = courseWithHoles.holes.size,
            players = players.map { player ->
                val playerHoleScores = scoreMap.entries
                    .filter { it.key.first == player.id }
                    .associate { it.key.second to it.value }
                val totalThrows = playerHoleScores.values.sum()
                val parSoFar = courseWithHoles.holes
                    .filter { hole -> playerHoleScores[hole.number] != null }
                    .sumOf { it.par }
                PlayerState(
                    playerId = player.id,
                    name = player.name,
                    holeScores = playerHoleScores,
                    totalThrows = totalThrows,
                    totalVsPar = totalThrows - parSoFar
                )
            }
        )
        ep.wearSyncManager().pushRoundState(roundState)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
