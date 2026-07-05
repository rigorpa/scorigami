package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.ObEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObDao {
    @Query("SELECT * FROM ob_counts WHERE roundId = :roundId")
    fun getObForRound(roundId: Long): Flow<List<ObEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOb(ob: ObEntity)

    @Query("DELETE FROM ob_counts WHERE roundId = :roundId AND playerId = :playerId AND holeNumber = :holeNumber")
    suspend fun deleteOb(roundId: Long, playerId: Long, holeNumber: Int)

    @Query("DELETE FROM ob_counts WHERE roundId = :roundId AND playerId = :playerId")
    suspend fun deleteObForPlayer(roundId: Long, playerId: Long)
}
