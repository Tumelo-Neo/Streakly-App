package com.example.streakly.entities

import java.util.*

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val category: String = "",
    val frequency: String = "Daily",
    val selectedDays: String = "", // Store as comma-separated string (e.g., "1,3,5" for Mon,Wed,Fri)
    val reminderTime: Long? = null,
    val startDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val streakCount: Int = 0,
    val lastCompleted: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Habit {
            return Habit(
                id = map["id"] as String,
                userId = map["userId"] as String,
                title = map["title"] as String,
                category = map["category"] as String,
                frequency = map["frequency"] as String,
                selectedDays = map["selectedDays"] as String,
                reminderTime = (map["reminderTime"] as? String)?.toLongOrNull(),
                startDate = (map["startDate"] as Long),
                notes = map["notes"] as String,
                streakCount = (map["streakCount"] as Long).toInt(),
                lastCompleted = (map["lastCompleted"] as? String)?.toLongOrNull(),
                createdAt = (map["createdAt"] as Long),
                updatedAt = (map["updatedAt"] as Long)
            )
        }
    }
}