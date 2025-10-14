package com.example.streakly.data

import android.content.Context
import android.util.Log
import com.example.streakly.entities.Habit
import com.example.streakly.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService(private val context: Context) {
    private val client: OkHttpClient
    private val baseUrl = "http://192.168.18.40:3000/api" // Use 10.0.2.2 for Android emulator

    init {
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun makeRequest(request: Request): String? {
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("ApiService", "Request failed: ${response.code} - ${response.message}")
                        return@withContext null
                    }
                    response.body?.string()
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error: ${e.message}")
                null
            }
        }
    }

    // User operations
    suspend fun registerUser(name: String, email: String, password: String): User? {
        val json = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", password)
        }

        val url = "$baseUrl/register"
        Log.d("ApiService", "Attempting registration to: $url")
        Log.d("ApiService", "With data: name=$name, email=$email")

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = makeRequest(request)
        Log.d("ApiService", "Registration response: $response")
        return response?.let {
            val jsonResponse = JSONObject(it)
            if (jsonResponse.getBoolean("success")) {
                val userJson = jsonResponse.getJSONObject("user")
                User(
                    id = userJson.getString("id"),
                    name = userJson.getString("name"),
                    email = userJson.getString("email"),
                    passwordHash = "", // Not stored from API
                    createdAt = userJson.getLong("createdAt"),
                    updatedAt = userJson.getLong("updatedAt")
                )
            } else {
                null
            }
        }
    }

    suspend fun loginUser(email: String, password: String): User? {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val request = Request.Builder()
            .url("$baseUrl/login")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            if (jsonResponse.getBoolean("success")) {
                val userJson = jsonResponse.getJSONObject("user")
                User(
                    id = userJson.getString("id"),
                    name = userJson.getString("name"),
                    email = userJson.getString("email"),
                    passwordHash = "", // Not stored from API
                    createdAt = userJson.getLong("createdAt"),
                    updatedAt = userJson.getLong("updatedAt")
                )
            } else {
                null
            }
        }
    }

    suspend fun getUserById(userId: String): User? {
        val request = Request.Builder()
            .url("$baseUrl/user/$userId")
            .addHeader("user-id", userId)
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            val userJson = jsonResponse.getJSONObject("user")
            User(
                id = userJson.getString("id"),
                name = userJson.getString("name"),
                email = userJson.getString("email"),
                passwordHash = "",
                createdAt = userJson.getLong("createdAt"),
                updatedAt = userJson.getLong("updatedAt")
            )
        }
    }

    // Habit operations
    suspend fun getHabits(userId: String): List<Habit> {
        val request = Request.Builder()
            .url("$baseUrl/habits")
            .addHeader("user-id", userId)
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            val habitsArray = jsonResponse.getJSONArray("habits")
            val habits = mutableListOf<Habit>()

            for (i in 0 until habitsArray.length()) {
                val habitJson = habitsArray.getJSONObject(i)
                habits.add(Habit(
                    id = habitJson.getString("id"),
                    userId = habitJson.getString("userId"),
                    title = habitJson.getString("title"),
                    category = habitJson.getString("category"),
                    frequency = habitJson.getString("frequency"),
                    selectedDays = habitJson.getString("selectedDays"),
                    reminderTime = if (habitJson.has("reminderTime") && !habitJson.isNull("reminderTime"))
                        habitJson.getLong("reminderTime") else null,
                    startDate = habitJson.getLong("startDate"),
                    notes = habitJson.getString("notes"),
                    streakCount = habitJson.getInt("streakCount"),
                    lastCompleted = if (habitJson.has("lastCompleted") && !habitJson.isNull("lastCompleted"))
                        habitJson.getLong("lastCompleted") else null,
                    createdAt = habitJson.getLong("createdAt"),
                    updatedAt = habitJson.getLong("updatedAt")
                ))
            }
            habits
        } ?: emptyList()
    }

    suspend fun getHabitById(habitId: String, userId: String): Habit? {
        val request = Request.Builder()
            .url("$baseUrl/habits/$habitId")
            .addHeader("user-id", userId)
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            val habitJson = jsonResponse.getJSONObject("habit")
            Habit(
                id = habitJson.getString("id"),
                userId = habitJson.getString("userId"),
                title = habitJson.getString("title"),
                category = habitJson.getString("category"),
                frequency = habitJson.getString("frequency"),
                selectedDays = habitJson.getString("selectedDays"),
                reminderTime = if (habitJson.has("reminderTime") && !habitJson.isNull("reminderTime"))
                    habitJson.getLong("reminderTime") else null,
                startDate = habitJson.getLong("startDate"),
                notes = habitJson.getString("notes"),
                streakCount = habitJson.getInt("streakCount"),
                lastCompleted = if (habitJson.has("lastCompleted") && !habitJson.isNull("lastCompleted"))
                    habitJson.getLong("lastCompleted") else null,
                createdAt = habitJson.getLong("createdAt"),
                updatedAt = habitJson.getLong("updatedAt")
            )
        }
    }

    suspend fun createHabit(habit: Habit, userId: String): Habit? {
        val json = JSONObject().apply {
            put("title", habit.title)
            put("category", habit.category)
            put("frequency", habit.frequency)
            put("selectedDays", habit.selectedDays)
            put("reminderTime", habit.reminderTime)
            put("startDate", habit.startDate)
            put("notes", habit.notes)
        }

        val request = Request.Builder()
            .url("$baseUrl/habits")
            .addHeader("user-id", userId)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            val habitJson = jsonResponse.getJSONObject("habit")
            Habit(
                id = habitJson.getString("id"),
                userId = habitJson.getString("userId"),
                title = habitJson.getString("title"),
                category = habitJson.getString("category"),
                frequency = habitJson.getString("frequency"),
                selectedDays = habitJson.getString("selectedDays"),
                reminderTime = if (habitJson.has("reminderTime") && !habitJson.isNull("reminderTime"))
                    habitJson.getLong("reminderTime") else null,
                startDate = habitJson.getLong("startDate"),
                notes = habitJson.getString("notes"),
                streakCount = habitJson.getInt("streakCount"),
                lastCompleted = if (habitJson.has("lastCompleted") && !habitJson.isNull("lastCompleted"))
                    habitJson.getLong("lastCompleted") else null,
                createdAt = habitJson.getLong("createdAt"),
                updatedAt = habitJson.getLong("updatedAt")
            )
        }
    }

    suspend fun updateHabit(habit: Habit, userId: String): Habit? {
        val json = JSONObject().apply {
            put("title", habit.title)
            put("category", habit.category)
            put("frequency", habit.frequency)
            put("selectedDays", habit.selectedDays)
            put("reminderTime", habit.reminderTime)
            put("startDate", habit.startDate)
            put("notes", habit.notes)
            put("streakCount", habit.streakCount)
            put("lastCompleted", habit.lastCompleted)
        }

        val request = Request.Builder()
            .url("$baseUrl/habits/${habit.id}")
            .addHeader("user-id", userId)
            .put(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            val habitJson = jsonResponse.getJSONObject("habit")
            Habit(
                id = habitJson.getString("id"),
                userId = habitJson.getString("userId"),
                title = habitJson.getString("title"),
                category = habitJson.getString("category"),
                frequency = habitJson.getString("frequency"),
                selectedDays = habitJson.getString("selectedDays"),
                reminderTime = if (habitJson.has("reminderTime") && !habitJson.isNull("reminderTime"))
                    habitJson.getLong("reminderTime") else null,
                startDate = habitJson.getLong("startDate"),
                notes = habitJson.getString("notes"),
                streakCount = habitJson.getInt("streakCount"),
                lastCompleted = if (habitJson.has("lastCompleted") && !habitJson.isNull("lastCompleted"))
                    habitJson.getLong("lastCompleted") else null,
                createdAt = habitJson.getLong("createdAt"),
                updatedAt = habitJson.getLong("updatedAt")
            )
        }
    }

    suspend fun deleteHabit(habitId: String, userId: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/habits/$habitId")
            .addHeader("user-id", userId)
            .delete()
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            jsonResponse.getBoolean("success")
        } ?: false
    }

    suspend fun markHabitCompleted(habitId: String, userId: String): Habit? {
        val json = JSONObject().apply {
            put("date", System.currentTimeMillis())
        }

        val request = Request.Builder()
            .url("$baseUrl/habits/$habitId/complete")
            .addHeader("user-id", userId)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            val habitJson = jsonResponse.getJSONObject("habit")
            Habit(
                id = habitJson.getString("id"),
                userId = habitJson.getString("userId"),
                title = habitJson.getString("title"),
                category = habitJson.getString("category"),
                frequency = habitJson.getString("frequency"),
                selectedDays = habitJson.getString("selectedDays"),
                reminderTime = if (habitJson.has("reminderTime") && !habitJson.isNull("reminderTime"))
                    habitJson.getLong("reminderTime") else null,
                startDate = habitJson.getLong("startDate"),
                notes = habitJson.getString("notes"),
                streakCount = habitJson.getInt("streakCount"),
                lastCompleted = if (habitJson.has("lastCompleted") && !habitJson.isNull("lastCompleted"))
                    habitJson.getLong("lastCompleted") else null,
                createdAt = habitJson.getLong("createdAt"),
                updatedAt = habitJson.getLong("updatedAt")
            )
        }
    }

    // Analytics operations
    suspend fun getCompletedTodayCount(userId: String): Int {
        val request = Request.Builder()
            .url("$baseUrl/analytics/completed-count")
            .addHeader("user-id", userId)
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            jsonResponse.getInt("count")
        } ?: 0
    }

    suspend fun getHabitCompletionCount(habitId: String, userId: String): Int {
        val request = Request.Builder()
            .url("$baseUrl/habits/$habitId/completion-count")
            .addHeader("user-id", userId)
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            jsonResponse.getInt("count")
        } ?: 0
    }

    suspend fun getUserStats(userId: String): Map<String, Int> {
        val request = Request.Builder()
            .url("$baseUrl/analytics/stats")
            .addHeader("user-id", userId)
            .build()

        val response = makeRequest(request)
        return response?.let {
            val jsonResponse = JSONObject(it)
            mapOf(
                "habitCount" to jsonResponse.getInt("habitCount"),
                "totalStreaks" to jsonResponse.getInt("totalStreaks"),
                "completedToday" to jsonResponse.getInt("completedToday")
            )
        } ?: mapOf(
            "habitCount" to 0,
            "totalStreaks" to 0,
            "completedToday" to 0
        )
    }
}