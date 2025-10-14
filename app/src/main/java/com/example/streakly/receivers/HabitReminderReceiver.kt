package com.example.streakly.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.streakly.utils.NotificationHelper

class HabitReminderReceiver : BroadcastReceiver() {
    private val TAG = "HabitReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received!")

        val habitTitle = intent.getStringExtra("HABIT_TITLE") ?: "Your habit"
        val habitId = intent.getStringExtra("HABIT_ID") ?: ""

        Log.d(TAG, "Showing notification for habit: $habitTitle")

        // Show notification
        NotificationHelper.showHabitReminder(context, habitTitle, habitId)
    }
}