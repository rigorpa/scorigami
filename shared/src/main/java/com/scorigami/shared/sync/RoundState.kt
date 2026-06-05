package com.scorigami.shared.sync

import kotlinx.serialization.Serializable

@Serializable
data class RoundState(
    val roundId: Long,
    val courseName: String,
    val currentHole: Int,
    val totalHoles: Int,
    val players: List<PlayerState>
)

@Serializable
data class PlayerState(
    val playerId: Long,
    val name: String,
    val holeScores: Map<Int, Int> = emptyMap(),
    val totalThrows: Int,
    val totalVsPar: Int
)

@Serializable
data class ScoreUpdateMessage(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val throws: Int,
    val viewingHole: Int
)
