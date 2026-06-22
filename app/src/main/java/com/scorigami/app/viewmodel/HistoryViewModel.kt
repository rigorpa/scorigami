package com.scorigami.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val scores: Map<Pair<Long, Int>, Int> = emptyMap()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val roundDao: RoundDao,
    private val courseDao: CourseDao,
    private val playerDao: PlayerDao,
    private val scoreDao: ScoreDao,
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
            playerDao.getPlayersForRoundFlow(detailRoundId)
        ) { scores, players ->
            val round = roundDao.getRoundById(detailRoundId)
                ?: return@combine RoundDetailState()
            val courseWithHoles = courseDao.getCourseWithHoles(round.courseId)
                ?: return@combine RoundDetailState()
            RoundDetailState(
                courseName = courseWithHoles.course.name,
                date = formatOrdinalDate(round.startedAt),
                holes = courseWithHoles.holes.sortedBy { it.number },
                players = players,
                scores = scores.associate { Pair(it.playerId, it.holeNumber) to it.throws }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoundDetailState())
    }

}
