package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE name = :name LIMIT 1")
    suspend fun getPlayerByName(name: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Query("""
        SELECT p.* FROM players p
        INNER JOIN round_players rp ON p.id = rp.playerId
        WHERE rp.roundId = :roundId
        ORDER BY rp.`order` ASC
    """)
    suspend fun getPlayersForRound(roundId: Long): List<PlayerEntity>

    @Query("""
        SELECT p.* FROM players p
        INNER JOIN round_players rp ON p.id = rp.playerId
        WHERE rp.roundId = :roundId
        ORDER BY rp.`order` ASC
    """)
    fun getPlayersForRoundFlow(roundId: Long): Flow<List<PlayerEntity>>

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("UPDATE players SET isArchived = 1 WHERE id = :playerId")
    suspend fun archivePlayer(playerId: Long)
}
