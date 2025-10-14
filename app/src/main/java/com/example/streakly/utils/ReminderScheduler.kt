package com.example.streakly.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.streakly.entities.Habit
import com.example.streakly.receivers.HabitReminderReceiver
import java.text.SimpleDateFormat
import java.util.*

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun scheduleReminder(context: Context, habit: Habit) {
        Log.d(TAG, "Attempting to schedule reminder for habit: ${habit.title}")

        // If no reminder time is set, don't schedule
        if (habit.reminderTime == null) {
            Log.d(TAG, "No reminder time set for habit: ${habit.title}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_TITLE", habit.title)
            putExtra("HABIT_ID", habit.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set reminder for today at the specified time
        val calendar = Calendar.getInstance().apply {
            timeInMillis = habit.reminderTime!!

            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            Log.d(TAG, "Original reminder time: ${dateFormat.format(Date(habit.reminderTime!!))}")

            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
                Log.d(TAG, "Time passed, scheduling for tomorrow")
            }

            Log.d(TAG, "Scheduled reminder time: ${dateFormat.format(Date(timeInMillis))}")
        }

        // Use set() instead of setExact() to avoid permission issues
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "Reminder scheduled successfully for: ${habit.title}")
    }

    fun cancelReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        NotificationHelper.cancelReminder(context, habitId)
        Log.d(TAG, "Reminder cancelled for habit ID: $habitId")
    }
}