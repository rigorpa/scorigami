package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "scores",
    primaryKeys = ["roundId", "playerId", "holeNumber"],
    foreignKeys = [ForeignKey(
        entity = RoundEntity::class,
        parentColumns = ["id"],
        childColumns = ["roundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roundId")]
)
data class ScoreEntity(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val throws: Int
)
