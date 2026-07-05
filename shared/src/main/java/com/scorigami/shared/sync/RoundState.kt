package com.scorigami.shared.sync

import kotlinx.serialization.Serializable

@Serializable
data class RoundState(
    val roundId: Long,
    val courseName: String,
    val currentHole: Int,
    val totalHoles: Int,
    val players: List<PlayerState>,
    val holePars: Map<Int, Int> = emptyMap()
)

@Serializable
data class PlayerState(
    val playerId: Long,
    val name: String,
    val holeScores: Map<Int, Int> = emptyMap(),
    val totalThrows: Int,
    val totalVsPar: Int,
    // Per-hole stat counts (hole number → count); rows exist only for holes with count > 0
    val obCounts: Map<Int, Int> = emptyMap(),
    val c1xCounts: Map<Int, Int> = emptyMap()
)

@Serializable
data class ScoreUpdateMessage(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val throws: Int,
    val viewingHole: Int
)

/** Watch→phone update for a per-hole stat counter. [stat] is "ob" or "c1x". */
@Serializable
data class StatUpdateMessage(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val stat: String,
    val count: Int,
    val viewingHole: Int
)
