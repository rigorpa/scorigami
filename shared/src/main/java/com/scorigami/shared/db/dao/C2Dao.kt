package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.C2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface C2Dao {
    @Query("SELECT * FROM c2_counts WHERE roundId = :roundId")
    fun getC2ForRound(roundId: Long): Flow<List<C2Entity>>

    @Query("SELECT * FROM c2_counts WHERE roundId = :roundId")
    suspend fun getC2ForRoundSnapshot(roundId: Long): List<C2Entity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertC2(c2: C2Entity)

    @Query("DELETE FROM c2_counts WHERE roundId = :roundId AND playerId = :playerId AND holeNumber = :holeNumber")
    suspend fun deleteC2(roundId: Long, playerId: Long, holeNumber: Int)

    @Query("DELETE FROM c2_counts WHERE roundId = :roundId AND playerId = :playerId")
    suspend fun deleteC2ForPlayer(roundId: Long, playerId: Long)
}
