package com.example.streakly.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.streakly.entities.Habit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class OfflineManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("offline_queue", Context.MODE_PRIVATE)
    private val TAG = "OfflineManager"

    companion object {
        private const val KEY_OFFLINE_QUEUE = "offline_actions_queue"
        private const val KEY_IS_ONLINE = "is_online"
    }

    fun setOnlineStatus(isOnline: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_ONLINE, isOnline).apply()
        Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")
    }

    fun isOnline(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_ONLINE, true)
    }

    // Add action to offline queue
    fun addOfflineAction(action: String, data: Map<String, Any>) {
        val queue = getOfflineQueue().toMutableList()
        val actionData = JSONObject().apply {
            put("action", action)
            put("data", JSONObject(data))
            put("timestamp", System.currentTimeMillis())
        }
        queue.add(actionData.toString())

        sharedPreferences.edit().putStringSet(KEY_OFFLINE_QUEUE, queue.toSet()).apply()
        Log.d(TAG, "Added offline action: $action, Queue size: ${queue.size}")
    }

    // Get all pending offline actions
    private fun getOfflineQueue(): List<String> {
        return sharedPreferences.getStringSet(KEY_OFFLINE_QUEUE, setOf())?.toList() ?: emptyList()
    }

    // Clear offline queue
    private fun clearOfflineQueue() {
        sharedPreferences.edit().remove(KEY_OFFLINE_QUEUE).apply()
        Log.d(TAG, "Cleared offline queue")
    }

    // Process all pending offline actions
    suspend fun processOfflineQueue() {
        if (!isOnline()) return

        val queue = getOfflineQueue()
        if (queue.isEmpty()) return

        Log.d(TAG, "Processing offline queue with ${queue.size} actions")

        val successfulActions = mutableListOf<String>()

        for (actionJson in queue) {
            try {
                val actionObj = JSONObject(actionJson)
                val action = actionObj.getString("action")
                val data = actionObj.getJSONObject("data")

                when (action) {
                    "CREATE_HABIT" -> {
                        val habit = parseHabitFromJson(data)
                        HabitManager.addHabit(habit)
                        successfulActions.add(actionJson)
                    }
                    "UPDATE_HABIT" -> {
                        val habit = parseHabitFromJson(data)
                        HabitManager.updateHabit(habit)
                        successfulActions.add(actionJson)
                    }
                    "DELETE_HABIT" -> {
                        val habitId = data.getString("habitId")
                        HabitManager.deleteHabit(habitId)
                        successfulActions.add(actionJson)
                    }
                    "COMPLETE_HABIT" -> {
                        val habitId = data.getString("habitId")
                        HabitManager.markHabitCompleted(habitId)
                        successfulActions.add(actionJson)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process offline action: ${e.message}")
            }
        }

        // Remove successfully processed actions
        if (successfulActions.isNotEmpty()) {
            val remainingQueue = queue.toMutableList().apply {
                removeAll(successfulActions)
            }
            sharedPreferences.edit().putStringSet(KEY_OFFLINE_QUEUE, remainingQueue.toSet()).apply()
            Log.d(TAG, "Processed ${successfulActions.size} actions, ${remainingQueue.size} remaining")
        }
    }

    private fun parseHabitFromJson(data: JSONObject): Habit {
        return Habit(
            id = data.getString("id"),
            userId = data.getString("userId"),
            title = data.getString("title"),
            category = data.getString("category"),
            frequency = data.getString("frequency"),
            selectedDays = data.getString("selectedDays"),
            reminderTime = if (data.has("reminderTime") && !data.isNull("reminderTime"))
                data.getLong("reminderTime") else null,
            startDate = data.getLong("startDate"),
            notes = data.getString("notes"),
            streakCount = data.getInt("streakCount"),
            lastCompleted = if (data.has("lastCompleted") && !data.isNull("lastCompleted"))
                data.getLong("lastCompleted") else null,
            createdAt = data.getLong("createdAt"),
            updatedAt = data.getLong("updatedAt")
        )
    }

    fun getPendingActionsCount(): Int {
        return getOfflineQueue().size
    }
}