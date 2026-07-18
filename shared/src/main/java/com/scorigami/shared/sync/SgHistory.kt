package com.scorigami.shared.sync

import kotlinx.serialization.Serializable

/**
 * Serializable export of all completed rounds for sharing via .sghistory files.
 * Each round carries a full course snapshot (name + holes) so the file can be imported
 * on a device that has never seen the course — the importer recreates it by name.
 */
@Serializable
data class SgHistory(
    val version: Int = 1,
    val rounds: List<SgRound>
)

@Serializable
data class SgRound(
    val courseName: String,
    val startedAt: Long,
    val completedAt: Long,
    val holes: List<SgHole>,
    val players: List<SgRoundPlayer>
)

@Serializable
data class SgRoundPlayer(
    val name: String,
    val order: Int,
    /** hole number → throws (JSON object keys are strings; kotlinx handles Int keys) */
    val scores: Map<Int, Int> = emptyMap(),
    val obCounts: Map<Int, Int> = emptyMap(),
    val c1xCounts: Map<Int, Int> = emptyMap()
)
