package com.example.streakly.data

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Notification settings
    var notificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean("notifications_enabled", true)
        set(value) = sharedPreferences.edit().putBoolean("notifications_enabled", value).apply()

    // Dark mode settings
    var darkModeEnabled: Boolean
        get() = sharedPreferences.getBoolean("dark_mode_enabled", true)
        set(value) = sharedPreferences.edit().putBoolean("dark_mode_enabled", value).apply()

    // Other settings can be added here
    var firstLaunch: Boolean
        get() = sharedPreferences.getBoolean("first_launch", true)
        set(value) = sharedPreferences.edit().putBoolean("first_launch", value).apply()

    // Clear all settings
    fun clearAllSettings() {
        sharedPreferences.edit().clear().apply()
    }
}