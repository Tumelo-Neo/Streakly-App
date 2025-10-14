package com.example.streakly.data

import android.database.sqlite.SQLiteDatabase
import com.example.streakly.entities.Habit
import com.example.streakly.entities.HabitInstance
import com.example.streakly.entities.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HabitDao(private val dbHelper: SimpleDatabaseHelper) {

    // User operations
    suspend fun insertUser(user: User) {
        val db = dbHelper.writableDatabase
        db.insert(SimpleDatabaseHelper.TABLE_USERS, null, dbHelper.userToContentValues(user))
    }

    suspend fun getUserByEmail(email: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SimpleDatabaseHelper.TABLE_USERS,
            null,
            "${SimpleDatabaseHelper.COLUMN_EMAIL} = ?",
            arrayOf(email),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                dbHelper.cursorToUser(it)
            } else {
                null
            }
        }
    }

    suspend fun getUserById(userId: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SimpleDatabaseHelper.TABLE_USERS,
            null,
            "${SimpleDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                dbHelper.cursorToUser(it)
            } else {
                null
            }
        }
    }

    // Use callbackFlow for real-time updates - user-specific
    fun getHabitsByUserFlow(userId: String): Flow<List<Habit>> = callbackFlow {
        val initialHabits = getHabitsByUserId(userId)
        trySend(initialHabits)
        awaitClose { }
    }

    private fun getHabitsByUserId(userId: String): List<Habit> {
        val db = dbHelper.readableDatabase
        val habits = mutableListOf<Habit>()

        val cursor = db.query(
            SimpleDatabaseHelper.TABLE_HABITS,
            null,
            "${SimpleDatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId),
            null, null,
            "${SimpleDatabaseHelper.COLUMN_CREATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                habits.add(dbHelper.cursorToHabit(it))
            }
        }
        return habits
    }

    suspend fun getHabitById(habitId: String): Habit? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SimpleDatabaseHelper.TABLE_HABITS,
            null,
            "${SimpleDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(habitId),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                dbHelper.cursorToHabit(it)
            } else {
                null
            }
        }
    }

    suspend fun insertHabit(habit: Habit) {
        val db = dbHelper.writableDatabase
        db.insert(SimpleDatabaseHelper.TABLE_HABITS, null, dbHelper.habitToContentValues(habit))
    }

    suspend fun updateHabit(habit: Habit) {
        val db = dbHelper.writableDatabase
        db.update(
            SimpleDatabaseHelper.TABLE_HABITS,
            dbHelper.habitToContentValues(habit),
            "${SimpleDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(habit.id)
        )
    }

    suspend fun deleteHabitById(habitId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            SimpleDatabaseHelper.TABLE_HABITS,
            "${SimpleDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(habitId)
        )
    }

    // HabitInstance operations
    suspend fun insertHabitInstance(instance: HabitInstance) {
        val db = dbHelper.writableDatabase
        db.insert(SimpleDatabaseHelper.TABLE_HABIT_INSTANCES, null, dbHelper.habitInstanceToContentValues(instance))
    }

    suspend fun getHabitInstance(habitId: String, date: Long): HabitInstance? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SimpleDatabaseHelper.TABLE_HABIT_INSTANCES,
            null,
            "${SimpleDatabaseHelper.COLUMN_HABIT_ID} = ? AND ${SimpleDatabaseHelper.COLUMN_DATE} = ?",
            arrayOf(habitId, date.toString()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                dbHelper.cursorToHabitInstance(it)
            } else {
                null
            }
        }
    }

    suspend fun getCompletedCountForDate(date: Long): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${SimpleDatabaseHelper.TABLE_HABIT_INSTANCES} WHERE ${SimpleDatabaseHelper.COLUMN_DATE} = ? AND ${SimpleDatabaseHelper.COLUMN_COMPLETED} = 1",
            arrayOf(date.toString())
        )

        return cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }
    }

    suspend fun getCompletedCount(habitId: String): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${SimpleDatabaseHelper.TABLE_HABIT_INSTANCES} WHERE ${SimpleDatabaseHelper.COLUMN_HABIT_ID} = ? AND ${SimpleDatabaseHelper.COLUMN_COMPLETED} = 1",
            arrayOf(habitId)
        )

        return cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }
    }

    suspend fun clearAllData() {
        val db = dbHelper.writableDatabase
        db.delete(SimpleDatabaseHelper.TABLE_HABIT_INSTANCES, null, null)
        db.delete(SimpleDatabaseHelper.TABLE_HABITS, null, null)
        db.delete(SimpleDatabaseHelper.TABLE_USERS, null, null)
    }
}