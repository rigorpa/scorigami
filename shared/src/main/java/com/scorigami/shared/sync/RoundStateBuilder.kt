package com.scorigami.shared.sync

import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

/**
 * Builds the [RoundState] snapshot pushed to the watch from raw round ingredients.
 *
 * Shared by `RoundViewModel` (which builds from its in-memory `RoundUiState`) and
 * `PhoneWearableListenerService` (which builds from fresh DB queries) so the per-player
 * `PlayerState` math — `holeScores`, `totalThrows`, `parSoFar`/`totalVsPar` — lives in one
 * place. The result is independent of [holes] ordering (all map/size/filter operations).
 */
object RoundStateBuilder {
    fun build(
        roundId: Long,
        courseName: String,
        currentHole: Int,
        holes: List<HoleEntity>,
        players: List<PlayerEntity>,
        scores: Map<Pair<Long, Int>, Int>
    ): RoundState = RoundState(
        roundId = roundId,
        courseName = courseName,
        currentHole = currentHole,
        totalHoles = holes.size,
        holePars = holes.associate { it.number to it.par },
        players = players.map { player ->
            val playerHoleScores = scores.entries
                .filter { it.key.first == player.id }
                .associate { it.key.second to it.value }
            val totalThrows = playerHoleScores.values.sum()
            val parSoFar = holes
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
}
