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
    /** Hole the round begins on (shotgun-style wraparound); see RoundEntity.startHole. */
    val startHole: Int = 1,
    /** Per-player handicap (playerId → value); see RoundPlayerEntity.handicap. */
    val handicaps: Map<Long, Int> = emptyMap(),
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
        val c1xCounts: List<C1xEntity>,
        val roundPlayers: List<RoundPlayerEntity>
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
                            c1xDao.getC1xForRound(round.id),
                            roundDao.getRoundPlayersFlow(round.id)
                        ) { scores, players, obCounts, c1xCounts, roundPlayers ->
                            val course = courseDao.getCourseWithHoles(round.courseId)
                                ?: return@combine null
                            RoundData(
                                round = round,
                                courseWithHoles = course,
                                players = players,
                                scores = scores,
                                obCounts = obCounts,
                                c1xCounts = c1xCounts,
                                roundPlayers = roundPlayers
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
                    val holeCount = data.courseWithHoles.holes.size
                    _uiState.update { current ->
                        // Seed currentHole from the round's start hole only on first load of a
                        // (newly started or freshly resumed) round — never on later re-emissions,
                        // which would reset the user's in-progress navigation back to the start.
                        val isNewRound = current.roundId != data.round.id
                        val hole = if (isNewRound) data.round.startHole else current.currentHole
                        current.copy(
                            roundId = data.round.id,
                            courseName = data.courseWithHoles.course.name,
                            holes = data.courseWithHoles.holes.sortedBy { it.number },
                            basePlayers = data.players,
                            players = sortPlayersForHole(data.players, scoreMap, hole, data.round.startHole, holeCount),
                            scores = scoreMap,
                            obCounts = data.obCounts.associate { Pair(it.playerId, it.holeNumber) to it.count },
                            c1xCounts = data.c1xCounts.associate { Pair(it.playerId, it.holeNumber) to it.count },
                            currentHole = hole,
                            startHole = data.round.startHole,
                            handicaps = data.roundPlayers.associate { it.playerId to it.handicap },
                            isActive = true
                        )
                    }
                    pushStateToWatch()
                }
        }
    }

    fun startRound(
        courseId: Long,
        playerNames: List<String>,
        startHole: Int = 1,
        handicaps: Map<String, Int> = emptyMap()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedPlayers = playerNames.map { name ->
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
            val roundId = roundDao.insertRound(RoundEntity(courseId = courseId, startHole = startHole))
            // Zip against the original (untrimmed) names so handicap lookups match exactly
            // what the setup screen used as map keys, regardless of incidental whitespace.
            roundDao.insertRoundPlayers(
                playerNames.zip(resolvedPlayers).mapIndexed { i, (rawName, player) ->
                    RoundPlayerEntity(
                        roundId = roundId,
                        playerId = player.id,
                        order = i,
                        handicap = handicaps[rawName] ?: 0
                    )
                }
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
                players = sortPlayersForHole(state.basePlayers, state.scores, hole, state.startHole, state.holes.size)
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


    /**
     * Holes in the order played for a round that starts at [startHole] — mirrors
     * app/ui/round/ScoreFormat.kt's holePlayOrder (this layer can't import ui.round)
     * and wear/ui/HoleOrder.kt; keep all three in sync.
     */
    private fun holePlayOrder(startHole: Int, holeCount: Int): List<Int> {
        if (holeCount <= 0) return emptyList()
        return (0 until holeCount).map { offset -> (startHole - 1 + offset) % holeCount + 1 }
    }

    /**
     * Honor-system sort for [hole]: primary key is the score on the previous hole IN PLAY
     * ORDER (follows [startHole] with shotgun-style wraparound, not raw hole number − 1),
     * ties broken cascading back through earlier played holes, then DB registration order.
     * The first hole of the round (hole == startHole) always uses base order.
     * Mirrored on the watch (WearScorecardScreen / WearNavigation) — keep both in sync.
     */
    private fun sortPlayersForHole(
        players: List<PlayerEntity>,
        scores: Map<Pair<Long, Int>, Int>,
        hole: Int,
        startHole: Int,
        holeCount: Int
    ): List<PlayerEntity> {
        val order = holePlayOrder(startHole, holeCount)
        val idx = order.indexOf(hole)
        if (idx <= 0) return players
        return players.sortedWith(
            Comparator { a, b ->
                for (i in idx - 1 downTo 0) {
                    val h = order[i]
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
            c1xCounts = state.c1xCounts,
            startHole = state.startHole
        )
        wearSyncManager.pushRoundState(roundState)
    }
}
