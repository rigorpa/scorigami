package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.C1xEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface C1xDao {
    @Query("SELECT * FROM c1x_counts WHERE roundId = :roundId")
    fun getC1xForRound(roundId: Long): Flow<List<C1xEntity>>

    @Query("SELECT * FROM c1x_counts WHERE roundId = :roundId")
    suspend fun getC1xForRoundSnapshot(roundId: Long): List<C1xEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertC1x(c1x: C1xEntity)

    @Query("DELETE FROM c1x_counts WHERE roundId = :roundId AND playerId = :playerId AND holeNumber = :holeNumber")
    suspend fun deleteC1x(roundId: Long, playerId: Long, holeNumber: Int)

    @Query("DELETE FROM c1x_counts WHERE roundId = :roundId AND playerId = :playerId")
    suspend fun deleteC1xForPlayer(roundId: Long, playerId: Long)
}
