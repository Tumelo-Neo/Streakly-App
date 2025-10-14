package com.example.streakly.entities

import java.util.UUID

// Remove all Room annotations - we're using direct SQLite now
data class HabitInstance(
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long,
    val completed: Boolean = false,
    val completedAt: Long? = null
)