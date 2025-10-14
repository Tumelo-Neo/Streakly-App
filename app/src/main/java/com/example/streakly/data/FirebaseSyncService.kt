package com.example.streakly.data

import android.util.Log
import com.example.streakly.entities.Habit
import com.example.streakly.entities.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseSyncService"

    // Authentication
    suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser

    // Data Sync
    suspend fun syncHabitsToCloud(habits: List<Habit>) {
        val userId = auth.currentUser?.uid ?: return

        try {
            habits.forEach { habit ->
                db.collection("users")
                    .document(userId)
                    .collection("habits")
                    .document(habit.id)
                    .set(habit.toMap())
                    .await()
            }
            Log.d(TAG, "Habits synced to cloud: ${habits.size} habits")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habits to cloud", e)
        }
    }

    suspend fun syncHabitsFromCloud(): List<Habit> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("habits")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                document.data?.let { data ->
                    Habit.fromMap(data)
                }
            }.also { habits ->
                Log.d(TAG, "Habits synced from cloud: ${habits.size} habits")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habits from cloud", e)
            emptyList()
        }
    }
}

// Extension functions to convert between Habit and Map
private fun Habit.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "category" to category,
        "frequency" to frequency,
        "selectedDays" to selectedDays,
        "reminderTime" to (reminderTime ?: ""),
        "startDate" to startDate,
        "notes" to notes,
        "streakCount" to streakCount,
        "lastCompleted" to (lastCompleted ?: ""),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

// Make this a companion object function in Habit class
// Add this to your Habit.kt entity file:
fun Habit.Companion.fromMap(map: Map<String, Any>): Habit {
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