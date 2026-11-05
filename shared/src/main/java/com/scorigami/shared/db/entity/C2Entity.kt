package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Made circle-2 putts (C2 ✓) per player per hole. Mirrors [ObEntity]: purely informational,
 * a row exists only while count > 0 (cleared counts are deleted).
 */
@Entity(
    tableName = "c2_counts",
    primaryKeys = ["roundId", "playerId", "holeNumber"],
    foreignKeys = [ForeignKey(
        entity = RoundEntity::class,
        parentColumns = ["id"],
        childColumns = ["roundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roundId")]
)
data class C2Entity(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val count: Int
)
