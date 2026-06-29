package com.scorigami.shared.sync

import kotlinx.serialization.Serializable

/**
 * Serializable representation of a disc golf course for sharing via .sgcourse files.
 * Version field allows forward-compatible format evolution.
 */
@Serializable
data class SgCourse(
    val version: Int = 1,
    val name: String,
    val holeCount: Int,
    val holes: List<SgHole>
)

@Serializable
data class SgHole(
    val number: Int,
    val par: Int,
    val distanceFeet: Int? = null,
    val notes: String? = null
)
