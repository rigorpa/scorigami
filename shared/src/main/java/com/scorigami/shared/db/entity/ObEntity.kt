package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Out-of-bounds penalty count per player per hole. Purely informational — OB throws are
 * already part of the entered score; this just tracks how many a player had. A row exists
 * only while count > 0 (cleared counts are deleted, mirroring how zero scores are handled).
 */
@Entity(
    tableName = "ob_counts",
    primaryKeys = ["roundId", "playerId", "holeNumber"],
    foreignKeys = [ForeignKey(
        entity = RoundEntity::class,
        parentColumns = ["id"],
        childColumns = ["roundId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roundId")]
)
data class ObEntity(
    val roundId: Long,
    val playerId: Long,
    val holeNumber: Int,
    val count: Int
)
