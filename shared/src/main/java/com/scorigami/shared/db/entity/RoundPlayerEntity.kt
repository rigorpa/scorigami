package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "round_players",
    primaryKeys = ["roundId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"]
        )
    ],
    indices = [Index("playerId")]
)
data class RoundPlayerEntity(
    val roundId: Long,
    val playerId: Long,
    val order: Int,
    /** Per-round handicap, set at round setup; added directly to a player's vs-par total
     * to produce their "Hcp" adjusted score. 0 = no handicap (not displayed). */
    val handicap: Int = 0
)
