package com.scorigami.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.*
import com.scorigami.shared.sync.PlayerState
import com.scorigami.shared.sync.RoundState
import com.scorigami.app.sync.WearSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val currentHole: Int = 1,
    val isActive: Boolean = false
)

@HiltViewModel
class RoundViewModel @Inject constructor(
    private val roundDao: RoundDao,
    private val scoreDao: ScoreDao,
    private val courseDao: CourseDao,
    private val playerDao: PlayerDao,
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
        val scores: List<ScoreEntity>
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
                            playerDao.getPlayersForRoundFlow(round.id)
                        ) { scores, players ->
                            val course = courseDao.getCourseWithHoles(round.courseId)
                                ?: return@combine null
                            RoundData(
                                round = round,
                                courseWithHoles = course,
                                players = players,
                                scores = scores
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
                if (existing != null) existing
                else {
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
            scoreDao.upsertScore(ScoreEntity(roundId = roundId, playerId = playerId, holeNumber = holeNumber, throws = throws))
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
            val player = existing ?: run {
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
            roundDao.removeRoundPlayer(roundId, playerId)
        }
    }

    private fun sortPlayersForHole(
        players: List<PlayerEntity>,
        scores: Map<Pair<Long, Int>, Int>,
        hole: Int
    ): List<PlayerEntity> {
        if (hole <= 1) return players
        val prevHole = hole - 1
        return players.sortedWith(compareBy { scores[Pair(it.id, prevHole)] ?: Int.MAX_VALUE })
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
        val roundState = RoundState(
            roundId = state.roundId,
            courseName = state.courseName,
            currentHole = state.currentHole,
            totalHoles = state.holes.size,
            players = state.players.map { player ->
                val playerHoleScores = state.scores.entries
                    .filter { it.key.first == player.id }
                    .associate { it.key.second to it.value }
                val totalThrows = playerHoleScores.values.sum()
                val parSoFar = state.holes
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
        wearSyncManager.pushRoundState(roundState)
    }
}
