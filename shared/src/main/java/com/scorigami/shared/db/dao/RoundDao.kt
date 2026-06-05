package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.RoundEntity
import com.scorigami.shared.db.entity.RoundPlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE completedAt IS NULL LIMIT 1")
    fun getActiveRound(): Flow<RoundEntity?>

    @Query("SELECT * FROM rounds WHERE completedAt IS NOT NULL ORDER BY startedAt DESC")
    fun getCompletedRounds(): Flow<List<RoundEntity>>

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
