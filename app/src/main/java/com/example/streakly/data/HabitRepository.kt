package com.example.streakly.data

import android.content.Context
import com.example.streakly.entities.Habit
import com.example.streakly.entities.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HabitRepository(private val context: Context) {
    private val apiService = ApiService(context)

    fun getContext(): Context {
        return context
    }

    // Authentication methods
    suspend fun registerUser(name: String, email: String, password: String): Boolean {
        return apiService.registerUser(name, email, password) != null
    }

    suspend fun loginUser(email: String, password: String): User? {
        return apiService.loginUser(email, password)
    }

    suspend fun getUserById(userId: String): User? {
        return apiService.getUserById(userId)
    }

    // User-specific habit methods
    fun getUserHabitsFlow(userId: String): Flow<List<Habit>> = flow {
        val habits = apiService.getHabits(userId)
        emit(habits)
    }

    suspend fun insertHabit(habit: Habit, userId: String) {
        apiService.createHabit(habit, userId)
    }

    suspend fun updateHabit(habit: Habit, userId: String) {
        apiService.updateHabit(habit, userId)
    }

    suspend fun deleteHabitById(habitId: String, userId: String) {
        apiService.deleteHabit(habitId, userId)
    }

    suspend fun getHabitById(habitId: String, userId: String): Habit? {
        return apiService.getHabitById(habitId, userId)
    }

    // HabitInstance operations
    suspend fun markHabitCompleted(habitId: String, userId: String) {
        apiService.markHabitCompleted(habitId, userId)
    }

    // Analytics
    suspend fun getCompletedTodayCount(userId: String): Int {
        return apiService.getCompletedTodayCount(userId)
    }

    suspend fun getCompletedCount(habitId: String, userId: String): Int {
        return apiService.getHabitCompletionCount(habitId, userId)
    }

    suspend fun getUserStats(userId: String): Map<String, Int> {
        return apiService.getUserStats(userId)
    }
}