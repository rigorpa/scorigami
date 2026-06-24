package com.scorigami.shared.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
