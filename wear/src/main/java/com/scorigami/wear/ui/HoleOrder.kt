package com.scorigami.wear.ui

/**
 * Holes in the order played for a round that starts at [startHole]: startHole,
 * startHole+1, ..., holeCount, 1, 2, ..., startHole-1 (shotgun-style wraparound).
 * Reduces to natural 1..holeCount order when startHole == 1.
 * Mirrored on the phone (app/ui/round/ScoreFormat.kt + RoundViewModel.kt) — keep in sync.
 */
internal fun holePlayOrder(startHole: Int, holeCount: Int): List<Int> {
    if (holeCount <= 0) return emptyList()
    return (0 until holeCount).map { offset -> (startHole - 1 + offset) % holeCount + 1 }
}
