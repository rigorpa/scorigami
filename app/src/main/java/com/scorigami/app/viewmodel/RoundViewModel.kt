package com.scorigami.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scorigami.shared.db.dao.C1xDao
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.ObDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.*
import com.scorigami.shared.sync.RoundStateBuilder
import com.scorigami.app.sync.WearSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoundUiState(
    val roundId: Long = -1L,
    val courseName: String = "",
    val holes: List<HoleEntity> = emptyList(),
    val basePlayers: List<PlayerEntity> = emptyList(),
    val players: List<PlayerEntity> = emptyList(),
    val scores: Map<Pair<Long, Int>, Int> = emptyMap(),
    val obCounts: Map<Pair<Long, Int>, Int> = emptyMap(),
    val c1xCounts: Map<Pair<Long, Int>, Int> = emptyMap(),
    val currentHole: Int = 1,
    val isActive: Boolean = false
)

@HiltViewModel
class RoundViewModel @Inject constructor(
    private val roundDao: RoundDao,
    private val scoreDao: ScoreDao,
    private val courseDao: CourseDao,
    private val playerDao: PlayerDao,
    private val obDao: ObDao,
    private val c1xDao: C1xDao,
    private val wearSyncManager: WearSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoundUiState())
    val uiState: StateFlow<RoundUiState> = _uiState.asStateFlow()

    private var pushJob: Job? = null

    val allPlayers: StateFlow<List<PlayerEntity>> = playerDao.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPlayedCourseId: StateFlow<Long?> = roundDao.getCompletedRounds()
        .map { it.firstOrNull()?.courseId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private data class RoundData(
        val round: RoundEntity,
        val courseWithHoles: com.scorigami.shared.db.dao.CourseWithHoles,
        val players: List<PlayerEntity>,
        val scores: List<ScoreEntity>,
        val obCounts: List<ObEntity>,
        val c1xCounts: List<C1xEntity>
    )

    init {
        viewModelScope.launch {
            roundDao.getActiveRound()
                .flatMapLatest { round ->
                    if (round == null) {
                        flowOf(null)
                    } else {
                        combine(
                            scoreDao.getScoresForRound(round.id),
                            playerDao.getPlayersForRoundFlow(round.id),
                            obDao.getObForRound(round.id),
                            c1xDao.getC1xForRound(round.id)
                        ) { scores, players, obCounts, c1xCounts ->
                            val course = courseDao.getCourseWithHoles(round.courseId)
                                ?: return@combine null
                            RoundData(
                                round = round,
                                courseWithHoles = course,
                                players = players,
                                scores = scores,
                                obCounts = obCounts,
                                c1xCounts = c1xCounts
                            )
                        }
                    }
                }
                .collect { data ->
                    if (data == null) {
                        _uiState.value = RoundUiState()
                        return@collect
                    }
                    val scoreMap = data.scores.associate { Pair(it.playerId, it.holeNumber) to it.throws }
                    _uiState.update { current ->
                        current.copy(
                            roundId = data.round.id,
                            courseName = data.courseWithHoles.course.name,
                            holes = data.courseWithHoles.holes.sortedBy { it.number },
                            basePlayers = data.players,
                            players = sortPlayersForHole(data.players, scoreMap, current.currentHole),
                            scores = scoreMap,
                            obCounts = data.obCounts.associate { Pair(it.playerId, it.holeNumber) to it.count },
                            c1xCounts = data.c1xCounts.associate { Pair(it.playerId, it.holeNumber) to it.count },
                            isActive = true
                        )
                    }
                    pushStateToWatch()
                }
        }
    }

    fun startRound(courseId: Long, playerNames: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val players = playerNames.map { name ->
                val existing = playerDao.getPlayerByName(name.trim())
                if (existing != null) {
                    if (existing.isArchived) {
                        playerDao.updatePlayer(existing.copy(isArchived = false))
                    }
                    existing
                } else {
                    val id = playerDao.insertPlayer(PlayerEntity(name = name.trim()))
                    PlayerEntity(id = id, name = name.trim())
                }
            }
            val roundId = roundDao.insertRound(RoundEntity(courseId = courseId))
            roundDao.insertRoundPlayers(
                players.mapIndexed { i, p -> RoundPlayerEntity(roundId = roundId, playerId = p.id, order = i) }
            )
        }
    }

    fun updateScore(playerId: Long, holeNumber: Int, throws: Int) {
        val roundId = _uiState.value.roundId
        if (roundId == -1L) return
        viewModelScope.launch(Dispatchers.IO) {
            if (throws <= 0) {
                scoreDao.deleteScore(roundId, playerId, holeNumber)
            } else {
                scoreDao.upsertScore(ScoreEntity(roundId = roundId, playerId = playerId, holeNumber = holeNumber, throws = throws))
            }
        }
    }

    /** Sets a player's OB count for a hole; count <= 0 clears the row (no stored zeros). */
    fun setOb(playerId: Long, holeNumber: Int, count: Int) {
        val roundId = _uiState.value.roundId
        if (roundId == -1L) return
        viewModelScope.launch(Dispatchers.IO) {
            if (count <= 0) {
                obDao.deleteOb(roundId, playerId, holeNumber)
            } else {
                obDao.upsertOb(ObEntity(roundId = roundId, playerId = playerId, holeNumber = holeNumber, count = count))
            }
        }
    }

    /** Sets a player's missed-C1-putt count for a hole; count <= 0 clears the row. */
    fun setC1x(playerId: Long, holeNumber: Int, count: Int) {
        val roundId = _uiState.value.roundId
        if (roundId == -1L) return
        viewModelScope.launch(Dispatchers.IO) {
            if (count <= 0) {
                c1xDao.deleteC1x(roundId, playerId, holeNumber)
            } else {
                c1xDao.upsertC1x(C1xEntity(roundId = roundId, playerId = playerId, holeNumber = holeNumber, count = count))
            }
        }
    }

    fun navigateToHole(hole: Int) {
        _uiState.update { state ->
            state.copy(
                currentHole = hole,
                players = sortPlayersForHole(state.basePlayers, state.scores, hole)
            )
        }
        pushStateToWatch()
    }

    fun completeRound() {
        val roundId = _uiState.value.roundId
        if (roundId == -1L) return
        viewModelScope.launch(Dispatchers.IO) {
            roundDao.completeRound(roundId, System.currentTimeMillis())
            wearSyncManager.clearRoundState()
        }
    }

    fun cancelRound() {
        val roundId = _uiState.value.roundId
        if (roundId == -1L) return
        viewModelScope.launch(Dispatchers.IO) {
            roundDao.deleteRound(roundId)
            wearSyncManager.clearRoundState()
        }
    }

    fun addPlayerToRound(name: String) {
        val roundId = _uiState.value.roundId
        if (roundId == -1L) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = playerDao.getPlayerByName(name.trim())
            val player = if (existing != null) {
                if (existing.isArchived) {
                    playerDao.updatePlayer(existing.copy(isArchived = false))
                }
                existing
            } else {
                val id = playerDao.insertPlayer(PlayerEntity(name = name.trim()))
                PlayerEntity(id = id, name = name.trim())
            }
            val order = playerDao.getPlayersForRound(roundId).size
            roundDao.insertRoundPlayers(listOf(
                RoundPlayerEntity(roundId = roundId, playerId = player.id, order = order)
            ))
        }
    }

    fun removePlayerFromRound(playerId: Long) {
        val roundId = _uiState.value.roundId
        if (roundId == -1L || _uiState.value.players.size <= 1) return
        viewModelScope.launch(Dispatchers.IO) {
            scoreDao.deleteScoresForPlayer(roundId, playerId)
            obDao.deleteObForPlayer(roundId, playerId)
            c1xDao.deleteC1xForPlayer(roundId, playerId)
            roundDao.removeRoundPlayer(roundId, playerId)
        }
    }

    fun archivePlayer(playerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            playerDao.archivePlayer(playerId)
        }
    }


    private fun sortPlayersForHole(
        players: List<PlayerEntity>,
        scores: Map<Pair<Long, Int>, Int>,
        hole: Int
    ): List<PlayerEntity> {
        if (hole <= 1) return players
        return players.sortedWith(
            Comparator { a, b ->
                for (h in hole - 1 downTo 1) {
                    val sa = scores[Pair(a.id, h)] ?: Int.MAX_VALUE
                    val sb = scores[Pair(b.id, h)] ?: Int.MAX_VALUE
                    if (sa != sb) return@Comparator sa - sb
                }
                0
            }
        )
    }

    private fun pushStateToWatch() {
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(150)
            doPushStateToWatch()
        }
    }

    private fun doPushStateToWatch() {
        val state = _uiState.value
        if (!state.isActive || state.holes.isEmpty()) return
        val roundState = RoundStateBuilder.build(
            roundId = state.roundId,
            courseName = state.courseName,
            currentHole = state.currentHole,
            holes = state.holes,
            players = state.basePlayers,
            scores = state.scores,
            obCounts = state.obCounts,
            c1xCounts = state.c1xCounts
        )
        wearSyncManager.pushRoundState(roundState)
    }
}
