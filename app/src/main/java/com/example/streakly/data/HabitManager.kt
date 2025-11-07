package com.example.streakly.data

import android.content.Context
import com.example.streakly.entities.Habit
import com.example.streakly.entities.User
import com.example.streakly.utils.ReminderScheduler
import com.example.streakly.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object HabitManager {
    private var repository: HabitRepository? = null
    private var sessionManager: SessionManager? = null
    private var firebaseSyncService: FirebaseSyncService? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var offlineManager: OfflineManager? = null

    fun initialize(context: Context) {
        repository = HabitRepository(context)
        sessionManager = SessionManager(context)
        firebaseSyncService = FirebaseSyncService()
    }

    // Helper method to get context safely
    private fun getContext(): Context? {
        return repository?.getContext()
    }

    // Authentication methods
    suspend fun registerUser(name: String, email: String, password: String): Boolean {
        return repository?.registerUser(name, email, password) ?: false
    }

    suspend fun loginUser(email: String, password: String): User? {
        val user = repository?.loginUser(email, password)
        user?.let { setUserSession(it) }
        return user
    }

    fun setUserSession(user: User) {
        sessionManager?.createLoginSession(user)
    }

    fun logoutUser() {
        sessionManager?.logoutUser()
    }

    fun isUserLoggedIn(): Boolean {
        return sessionManager?.isLoggedIn() ?: false
    }

    fun getCurrentUserId(): String? {
        return sessionManager?.getCurrentUserId()
    }

    fun getCurrentUserEmail(): String? {
        return sessionManager?.getCurrentUserEmail()
    }

    fun getCurrentUserName(): String? {
        return sessionManager?.getCurrentUserName()
    }

    // User-specific habit methods
    fun getUserHabitsFlow(): Flow<List<Habit>> {
        val userId = getCurrentUserId()
        return if (userId != null) {
            repository?.getUserHabitsFlow(userId) ?: flowOf(emptyList())
        } else {
            flowOf(emptyList())
        }
    }

    fun initializeOfflineManager(context: Context) {
        offlineManager = OfflineManager(context)
    }

    fun deleteHabit(habitId: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val isOnline = offlineManager?.isOnline() ?: true

            if (isOnline) {
                // Online: Delete directly
                ioScope.launch {
                    repository?.deleteHabitById(habitId, userId)
                    cancelHabitReminder(habitId)
                    if (isCloudSyncEnabled()) {
                        syncToCloud()
                    }
                }
            } else {
                // Offline: Add to queue
                val actionData = mapOf(
                    "habitId" to habitId,
                    "userId" to userId
                )
                offlineManager?.addOfflineAction("DELETE_HABIT", actionData)

                // Also delete from local database for immediate UI update
                ioScope.launch {
                    repository?.deleteHabitById(habitId, userId)
                    cancelHabitReminder(habitId)
                }
            }
        }
    }

    suspend fun getHabitById(habitId: String): Habit? {
        val userId = getCurrentUserId()
        return if (userId != null) {
            repository?.getHabitById(habitId, userId)
        } else {
            null
        }
    }

    fun markHabitCompleted(habitId: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val isOnline = offlineManager?.isOnline() ?: true

            if (isOnline) {
                // Online: Mark complete directly
                ioScope.launch {
                    repository?.markHabitCompleted(habitId, userId)
                }
            } else {
                // Offline: Add to queue
                val actionData = mapOf(
                    "habitId" to habitId,
                    "userId" to userId
                )
                offlineManager?.addOfflineAction("COMPLETE_HABIT", actionData)

                // Also mark complete in local database for immediate UI update
                ioScope.launch {
                    repository?.markHabitCompleted(habitId, userId)
                }
            }
        }
    }

    suspend fun getCompletedTodayCount(): Int {
        val userId = getCurrentUserId()
        return if (userId != null) {
            repository?.getCompletedTodayCount(userId) ?: 0
        } else {
            0
        }
    }

    suspend fun getHabitCount(): Int {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val stats = repository?.getUserStats(userId)
            stats?.get("habitCount") ?: 0
        } else {
            0
        }
    }

    suspend fun getTotalStreaks(): Int {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val stats = repository?.getUserStats(userId)
            stats?.get("totalStreaks") ?: 0
        } else {
            0
        }
    }

    fun clearAllData() {
        // Note: With API, we don't clear data on client side
        // This would need to be implemented as an API endpoint
    }

    fun scheduleHabitReminder(habit: Habit) {
        val context = getContext()
        if (context != null) {
            ReminderScheduler.scheduleReminder(context, habit)
        }
    }

    suspend fun syncOfflineActions() {
        offlineManager?.processOfflineQueue()
    }

    fun setOnlineStatus(isOnline: Boolean) {
        offlineManager?.setOnlineStatus(isOnline)
    }


    fun isOnline(): Boolean {
        return offlineManager?.isOnline() ?: true
    }

    fun getPendingActionsCount(): Int {
        return offlineManager?.getPendingActionsCount() ?: 0
    }

    fun addHabit(habit: Habit) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val isOnline = offlineManager?.isOnline() ?: true

            if (isOnline) {
                // Online: Add directly
                ioScope.launch {
                    repository?.insertHabit(habit, userId)
                    scheduleHabitReminder(habit)
                    if (isCloudSyncEnabled()) {
                        syncToCloud()
                    }
                }
            } else {
                // Offline: Add to queue
                val actionData = mapOf(
                    "id" to habit.id,
                    "userId" to userId,
                    "title" to habit.title,
                    "category" to habit.category,
                    "frequency" to habit.frequency,
                    "selectedDays" to habit.selectedDays,
                    "reminderTime" to (habit.reminderTime ?: ""),
                    "startDate" to habit.startDate,
                    "notes" to habit.notes,
                    "streakCount" to habit.streakCount,
                    "lastCompleted" to (habit.lastCompleted ?: ""),
                    "createdAt" to habit.createdAt,
                    "updatedAt" to habit.updatedAt
                )
                offlineManager?.addOfflineAction("CREATE_HABIT", actionData)

                // Also add to local database for immediate UI update
                ioScope.launch {
                    repository?.insertHabit(habit, userId)
                    scheduleHabitReminder(habit)
                }
            }
        }
    }

    fun updateHabit(updatedHabit: Habit) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val isOnline = offlineManager?.isOnline() ?: true

            if (isOnline) {
                // Online: Update directly
                ioScope.launch {
                    repository?.updateHabit(updatedHabit.copy(updatedAt = System.currentTimeMillis()), userId)
                    scheduleHabitReminder(updatedHabit)
                    if (isCloudSyncEnabled()) {
                        syncToCloud()
                    }
                }
            } else {
                // Offline: Add to queue
                val actionData = mapOf(
                    "id" to updatedHabit.id,
                    "userId" to userId,
                    "title" to updatedHabit.title,
                    "category" to updatedHabit.category,
                    "frequency" to updatedHabit.frequency,
                    "selectedDays" to updatedHabit.selectedDays,
                    "reminderTime" to (updatedHabit.reminderTime ?: ""),
                    "startDate" to updatedHabit.startDate,
                    "notes" to updatedHabit.notes,
                    "streakCount" to updatedHabit.streakCount,
                    "lastCompleted" to (updatedHabit.lastCompleted ?: ""),
                    "createdAt" to updatedHabit.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                offlineManager?.addOfflineAction("UPDATE_HABIT", actionData)

                // Also update local database for immediate UI update
                ioScope.launch {
                    repository?.updateHabit(updatedHabit.copy(updatedAt = System.currentTimeMillis()), userId)
                    scheduleHabitReminder(updatedHabit)
                }
            }
        }
    }


    fun cancelHabitReminder(habitId: String) {
        val context = getContext()
        if (context != null) {
            ReminderScheduler.cancelReminder(context, habitId)
        }
    }

    fun isCloudSyncEnabled(): Boolean {
        return firebaseSyncService?.getCurrentUser() != null
    }



    suspend fun syncToCloud() {
        if (!isCloudSyncEnabled()) return
        // Implementation for cloud sync would go here
    }

    suspend fun syncFromCloud(): Boolean {
        if (!isCloudSyncEnabled()) return false
        // Implementation for cloud sync would go here
        return false
    }

    // Google Sign-In method
    suspend fun signInWithGoogle(idToken: String): Boolean {
        return firebaseSyncService?.signInWithGoogle(idToken) ?: false
    }

    fun getFirebaseUser() = firebaseSyncService?.getCurrentUser()

    fun signOutFromFirebase() {
        firebaseSyncService?.signOut()
    }
}