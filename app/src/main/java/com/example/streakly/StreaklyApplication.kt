package com.example.streakly

import android.app.Application
import com.example.streakly.data.HabitManager
import com.example.streakly.utils.NotificationHelper

class StreaklyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HabitManager.initialize(this)
        // Initialize HabitManager - it will handle database setup internally
        HabitManager.initialize(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
    }
}