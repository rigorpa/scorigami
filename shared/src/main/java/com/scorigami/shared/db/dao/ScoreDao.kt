package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.ScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores WHERE roundId = :roundId")
    fun getScoresForRound(roundId: Long): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE roundId = :roundId")
    suspend fun getScoresForRoundSnapshot(roundId: Long): List<ScoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScore(score: ScoreEntity)

    @Query("SELECT SUM(throws) FROM scores WHERE roundId = :roundId AND playerId = :playerId")
    suspend fun getTotalThrowsForPlayer(roundId: Long, playerId: Long): Int?

    @Query("DELETE FROM scores WHERE roundId = :roundId AND playerId = :playerId")
    suspend fun deleteScoresForPlayer(roundId: Long, playerId: Long)

    @Query("DELETE FROM scores WHERE roundId = :roundId AND playerId = :playerId AND holeNumber = :holeNumber")
    suspend fun deleteScore(roundId: Long, playerId: Long, holeNumber: Int)
}
