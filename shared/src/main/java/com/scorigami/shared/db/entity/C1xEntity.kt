package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Missed circle-1 putts (C1x) per player per hole. Mirrors [ObEntity]: purely informational,
 * a row exists only while count > 0 (cleared counts are deleted).
 */
@Entity(
    tableName = "c1x_counts",
    primaryKeys = ["roundId", "playerId", "holeNumber"],
    foreignKeys = [ForeignKey(
        entity = RoundEntity::class,
        parentColumns = ["id"],
        childColumns = ["roundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roundId")]
)
data class C1xEntity(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val count: Int
)
