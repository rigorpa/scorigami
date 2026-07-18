package com.scorigami.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scorigami.shared.db.dao.C1xDao
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.ObDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.*
import com.scorigami.shared.sync.SgHistory
import com.scorigami.shared.sync.SgHole
import com.scorigami.shared.sync.SgRound
import com.scorigami.shared.sync.SgRoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class RoundSummary(
    val roundId: Long,
    val courseName: String,
    val date: String,
    val playerResults: List<Pair<String, String>>,
    val winner: String?
)

data class RoundDetailState(
    val courseName: String = "",
    val date: String = "",
    val holes: List<HoleEntity> = emptyList(),
    val players: List<PlayerEntity> = emptyList(),
    val scores: Map<Pair<Long, Int>, Int> = emptyMap(),
    val obCounts: Map<Pair<Long, Int>, Int> = emptyMap(),
    val c1xCounts: Map<Pair<Long, Int>, Int> = emptyMap()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val roundDao: RoundDao,
    private val courseDao: CourseDao,
    private val playerDao: PlayerDao,
    private val scoreDao: ScoreDao,
    private val obDao: ObDao,
    private val c1xDao: C1xDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    private fun formatOrdinalDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(timestamp))
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(timestamp))
        return "$month $day$suffix, $year"
    }

    val rounds: StateFlow<List<RoundSummary>> = roundDao.getCompletedRoundSummaryRows()
        .map { rows ->
            // Rows arrive newest-round-first, then in tee order within each round, so
            // groupBy preserves both the round order and the per-round player order.
            rows.groupBy { it.roundId }.map { (roundId, roundRows) ->
                val first = roundRows.first()
                val playerResults = roundRows.map { it.playerName to it.totalThrows.toString() }
                val winner = playerResults.minByOrNull { it.second.toIntOrNull() ?: Int.MAX_VALUE }?.first
                RoundSummary(
                    roundId = roundId,
                    courseName = first.courseName ?: "Unknown",
                    date = dateFormat.format(Date(first.startedAt)),
                    playerResults = playerResults,
                    winner = winner
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val detailRoundId: Long = savedStateHandle.get<Long>("roundId") ?: -1L

    val detail: StateFlow<RoundDetailState> = if (detailRoundId == -1L) {
        MutableStateFlow(RoundDetailState())
    } else {
        combine(
            scoreDao.getScoresForRound(detailRoundId),
            playerDao.getPlayersForRoundFlow(detailRoundId),
            obDao.getObForRound(detailRoundId),
            c1xDao.getC1xForRound(detailRoundId)
        ) { scores, players, obEntries, c1xEntries ->
            val round = roundDao.getRoundById(detailRoundId)
                ?: return@combine RoundDetailState()
            val courseWithHoles = courseDao.getCourseWithHoles(round.courseId)
                ?: return@combine RoundDetailState()
            val holes = courseWithHoles.holes.sortedBy { it.number }
            val scoreMap = scores.associate { Pair(it.playerId, it.holeNumber) to it.throws }
            // Best round first: lowest vs-par, then fewest throws; players with no
            // scores sort last. Stable sort keeps tee order among exact ties.
            val sortedPlayers = players.sortedWith(
                compareBy(
                    { player -> if (scores.none { it.playerId == player.id }) 1 else 0 },
                    { player ->
                        val throws = scores.filter { it.playerId == player.id }.sumOf { it.throws }
                        val par = holes.filter { scoreMap[Pair(player.id, it.number)] != null }.sumOf { it.par }
                        throws - par
                    },
                    { player -> scores.filter { it.playerId == player.id }.sumOf { it.throws } }
                )
            )
            RoundDetailState(
                courseName = courseWithHoles.course.name,
                date = formatOrdinalDate(round.startedAt),
                holes = holes,
                players = sortedPlayers,
                scores = scoreMap,
                obCounts = obEntries.associate { Pair(it.playerId, it.holeNumber) to it.count },
                c1xCounts = c1xEntries.associate { Pair(it.playerId, it.holeNumber) to it.count }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoundDetailState())
    }

    /** Emits (imported, skipped) after a .sghistory import completes. */
    private val _importedHistory = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1)
    val importedHistory: SharedFlow<Pair<Int, Int>> = _importedHistory.asSharedFlow()

    /**
     * Snapshot of every completed round as an [SgHistory] for .sghistory export.
     * Rounds whose course was deleted are skipped (no course snapshot to carry).
     */
    suspend fun buildExport(): SgHistory = withContext(Dispatchers.IO) {
        val sgRounds = roundDao.getCompletedRoundsSnapshot().mapNotNull { round ->
            val course = courseDao.getCourseWithHoles(round.courseId) ?: return@mapNotNull null
            val players = playerDao.getPlayersForRound(round.id)
            val scores = scoreDao.getScoresForRoundSnapshot(round.id)
            val obs = obDao.getObForRoundSnapshot(round.id)
            val c1xs = c1xDao.getC1xForRoundSnapshot(round.id)
            SgRound(
                courseName = course.course.name,
                startedAt = round.startedAt,
                completedAt = round.completedAt ?: round.startedAt,
                holes = course.holes.sortedBy { it.number }
                    .map { SgHole(it.number, it.par, it.distanceFeet, it.notes) },
                players = players.mapIndexed { i, p ->
                    SgRoundPlayer(
                        name = p.name,
                        order = i,
                        scores = scores.filter { it.playerId == p.id }
                            .associate { it.holeNumber to it.throws },
                        obCounts = obs.filter { it.playerId == p.id }
                            .associate { it.holeNumber to it.count },
                        c1xCounts = c1xs.filter { it.playerId == p.id }
                            .associate { it.holeNumber to it.count }
                    )
                }
            )
        }
        SgHistory(rounds = sgRounds)
    }

    /**
     * Imports rounds from a .sghistory file. A round already present (same `startedAt`
     * timestamp) is skipped; courses are matched by name or recreated from the file's
     * snapshot; players are matched by name or created (same reuse rule as rounds).
     */
    fun importHistory(history: SgHistory) {
        viewModelScope.launch(Dispatchers.IO) {
            var imported = 0
            var skipped = 0
            history.rounds.forEach { r ->
                if (roundDao.countRoundsStartedAt(r.startedAt) > 0) {
                    skipped++
                    return@forEach
                }
                val course = courseDao.getCourseByName(r.courseName) ?: run {
                    val id = courseDao.insertCourse(
                        CourseEntity(name = r.courseName, holeCount = r.holes.size)
                    )
                    courseDao.insertHoles(r.holes.map {
                        HoleEntity(
                            courseId = id,
                            number = it.number,
                            par = maxOf(2, it.par),
                            distanceFeet = it.distanceFeet,
                            notes = it.notes
                        )
                    })
                    CourseEntity(id = id, name = r.courseName, holeCount = r.holes.size)
                }
                // completedAt is always non-null in the file; a null here would make the
                // round look "active" and hijack the scorecard.
                val roundId = roundDao.insertRound(
                    RoundEntity(courseId = course.id, startedAt = r.startedAt, completedAt = r.completedAt)
                )
                val roundPlayers = r.players.sortedBy { it.order }.mapIndexed { i, sp ->
                    val name = sp.name.trim()
                    val player = playerDao.getPlayerByName(name)
                        ?: PlayerEntity(id = playerDao.insertPlayer(PlayerEntity(name = name)), name = name)
                    sp.scores.forEach { (hole, throws) ->
                        if (throws > 0) scoreDao.upsertScore(ScoreEntity(roundId, player.id, hole, throws))
                    }
                    sp.obCounts.forEach { (hole, count) ->
                        if (count > 0) obDao.upsertOb(ObEntity(roundId, player.id, hole, count))
                    }
                    sp.c1xCounts.forEach { (hole, count) ->
                        if (count > 0) c1xDao.upsertC1x(C1xEntity(roundId, player.id, hole, count))
                    }
                    RoundPlayerEntity(roundId = roundId, playerId = player.id, order = i)
                }
                roundDao.insertRoundPlayers(roundPlayers)
                imported++
            }
            _importedHistory.tryEmit(Pair(imported, skipped))
        }
    }
}
