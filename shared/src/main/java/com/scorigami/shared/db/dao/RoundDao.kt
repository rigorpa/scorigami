package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.RoundEntity
import com.scorigami.shared.db.entity.RoundPlayerEntity
import kotlinx.coroutines.flow.Flow

/**
 * One row per (completed round, player) with that player's total throws — the flat
 * projection behind the history list. Rows are ordered round-newest-first, then by tee
 * order within a round, so they can be grouped in memory into per-round summaries.
 * `courseName` is null when the round's course has been deleted.
 */
data class RoundPlayerSummaryRow(
    val roundId: Long,
    val startedAt: Long,
    val courseName: String?,
    val playerName: String,
    val totalThrows: Int
)

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE completedAt IS NULL LIMIT 1")
    fun getActiveRound(): Flow<RoundEntity?>

    @Query("""
        SELECT r.id AS roundId,
               r.startedAt AS startedAt,
               c.name AS courseName,
               p.name AS playerName,
               COALESCE(SUM(s.throws), 0) AS totalThrows
        FROM rounds r
        LEFT JOIN courses c ON c.id = r.courseId
        INNER JOIN round_players rp ON rp.roundId = r.id
        INNER JOIN players p ON p.id = rp.playerId
        LEFT JOIN scores s ON s.roundId = r.id AND s.playerId = p.id
        WHERE r.completedAt IS NOT NULL
        GROUP BY r.id, p.id
        ORDER BY r.startedAt DESC, rp.`order` ASC
    """)
    fun getCompletedRoundSummaryRows(): Flow<List<RoundPlayerSummaryRow>>

    @Query("SELECT * FROM rounds WHERE completedAt IS NOT NULL ORDER BY startedAt DESC")
    fun getCompletedRounds(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds WHERE completedAt IS NOT NULL ORDER BY startedAt DESC")
    suspend fun getCompletedRoundsSnapshot(): List<RoundEntity>

    @Query("SELECT COUNT(*) FROM rounds WHERE startedAt = :startedAt")
    suspend fun countRoundsStartedAt(startedAt: Long): Int

    @Query("SELECT * FROM rounds WHERE id = :id")
    suspend fun getRoundById(id: Long): RoundEntity?

    @Insert
    suspend fun insertRound(round: RoundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoundPlayers(players: List<RoundPlayerEntity>)

    @Query("UPDATE rounds SET completedAt = :completedAt WHERE id = :roundId")
    suspend fun completeRound(roundId: Long, completedAt: Long)

    @Query("DELETE FROM rounds WHERE id = :roundId")
    suspend fun deleteRound(roundId: Long)

    @Query("DELETE FROM round_players WHERE roundId = :roundId AND playerId = :playerId")
    suspend fun removeRoundPlayer(roundId: Long, playerId: Long)
}
