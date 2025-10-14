package com.example.streakly.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.streakly.entities.Habit
import com.example.streakly.entities.HabitInstance
import com.example.streakly.entities.User

class SimpleDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "streakly_database.db"
        private const val DATABASE_VERSION = 2 // Incremented version

        // Table names
        const val TABLE_USERS = "users"
        const val TABLE_HABITS = "habits"
        const val TABLE_HABIT_INSTANCES = "habit_instances"

        // Common column names
        const val COLUMN_ID = "id"
        const val COLUMN_CREATED_AT = "createdAt"
        const val COLUMN_UPDATED_AT = "updatedAt"

        // Users table columns
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD_HASH = "passwordHash"

        // Habits table columns
        const val COLUMN_USER_ID = "userId"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_FREQUENCY = "frequency"
        const val COLUMN_REMINDER_TIME = "reminderTime"
        const val COLUMN_START_DATE = "startDate"
        const val COLUMN_NOTES = "notes"
        const val COLUMN_STREAK_COUNT = "streakCount"
        const val COLUMN_LAST_COMPLETED = "lastCompleted"

        // Habit instances table columns
        const val COLUMN_HABIT_ID = "habitId"
        const val COLUMN_DATE = "date"
        const val COLUMN_COMPLETED = "completed"
        const val COLUMN_COMPLETED_AT = "completedAt"

        const val COLUMN_SELECTED_DAYS = "selectedDays"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER,
                $COLUMN_UPDATED_AT INTEGER
            )
        """.trimIndent()

        // Create habits table
        val createHabitsTable = """
    CREATE TABLE $TABLE_HABITS (
        $COLUMN_ID TEXT PRIMARY KEY,
        $COLUMN_USER_ID TEXT NOT NULL,
        $COLUMN_TITLE TEXT NOT NULL,
        $COLUMN_CATEGORY TEXT,
        $COLUMN_FREQUENCY TEXT,
        $COLUMN_SELECTED_DAYS TEXT,  -- NEW COLUMN
        $COLUMN_REMINDER_TIME INTEGER,
        $COLUMN_START_DATE INTEGER,
        $COLUMN_NOTES TEXT,
        $COLUMN_STREAK_COUNT INTEGER DEFAULT 0,
        $COLUMN_LAST_COMPLETED INTEGER,
        $COLUMN_CREATED_AT INTEGER,
        $COLUMN_UPDATED_AT INTEGER,
        FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID) ON DELETE CASCADE
    )
""".trimIndent()

        // Create habit_instances table
        val createHabitInstancesTable = """
            CREATE TABLE $TABLE_HABIT_INSTANCES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_HABIT_ID TEXT NOT NULL,
                $COLUMN_DATE INTEGER NOT NULL,
                $COLUMN_COMPLETED INTEGER DEFAULT 0,
                $COLUMN_COMPLETED_AT INTEGER,
                FOREIGN KEY ($COLUMN_HABIT_ID) REFERENCES $TABLE_HABITS($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createHabitsTable)
        db.execSQL(createHabitInstancesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Upgrade from version 1 to 2 - add users table and update habits table
            db.execSQL("DROP TABLE IF EXISTS $TABLE_HABIT_INSTANCES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_HABITS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
        }
    }

    // Helper methods for converting between objects and ContentValues
    fun userToContentValues(user: User): ContentValues {
        return ContentValues().apply {
            put(COLUMN_ID, user.id)
            put(COLUMN_NAME, user.name)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD_HASH, user.passwordHash)
            put(COLUMN_CREATED_AT, user.createdAt)
            put(COLUMN_UPDATED_AT, user.updatedAt)
        }
    }

    fun cursorToUser(cursor: android.database.Cursor): User {
        return User(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
            email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
            passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
        )
    }

    fun habitToContentValues(habit: Habit): ContentValues {
        return ContentValues().apply {
            put(COLUMN_ID, habit.id)
            put(COLUMN_USER_ID, habit.userId)
            put(COLUMN_TITLE, habit.title)
            put(COLUMN_CATEGORY, habit.category)
            put(COLUMN_FREQUENCY, habit.frequency)
            put(COLUMN_SELECTED_DAYS, habit.selectedDays)  // NEW
            put(COLUMN_REMINDER_TIME, habit.reminderTime)
            put(COLUMN_START_DATE, habit.startDate)
            put(COLUMN_NOTES, habit.notes)
            put(COLUMN_STREAK_COUNT, habit.streakCount)
            put(COLUMN_LAST_COMPLETED, habit.lastCompleted)
            put(COLUMN_CREATED_AT, habit.createdAt)
            put(COLUMN_UPDATED_AT, habit.updatedAt)
        }
    }

    fun cursorToHabit(cursor: android.database.Cursor): Habit {
        return Habit(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
            frequency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY)),
            selectedDays = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_DAYS)), // NEW
            reminderTime = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)),
            startDate = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_DATE)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
            streakCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STREAK_COUNT)),
            lastCompleted = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
        )
    }

    fun habitInstanceToContentValues(instance: HabitInstance): ContentValues {
        return ContentValues().apply {
            put(COLUMN_ID, instance.id)
            put(COLUMN_HABIT_ID, instance.habitId)
            put(COLUMN_DATE, instance.date)
            put(COLUMN_COMPLETED, if (instance.completed) 1 else 0)
            put(COLUMN_COMPLETED_AT, instance.completedAt)
        }
    }

    fun cursorToHabitInstance(cursor: android.database.Cursor): HabitInstance {
        return HabitInstance(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            habitId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HABIT_ID)),
            date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
            completed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1,
            completedAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_AT))
        )
    }

    // Add this method to SimpleDatabaseHelper class
    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_HABIT_INSTANCES, null, null)
        db.delete(TABLE_HABITS, null, null)
        db.delete(TABLE_USERS, null, null)
    }
}

// Extension function for safe cursor operations
fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
    return if (isNull(columnIndex)) null else getLong(columnIndex)
}