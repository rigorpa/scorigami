package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rounds",
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["courseId"]
    )],
    indices = [Index("courseId")]
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    /** Hole the round begins on (shotgun-style start). Play order wraps: startHole,
     * startHole+1, ..., holeCount, 1, 2, ..., startHole-1. 1 = normal linear round. */
    val startHole: Int = 1
)
