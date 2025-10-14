package com.example.streakly.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.streakly.R

object ThemeManager {
    private const val THEME_PREFERENCE = "theme_preference"
    private const val DARK_MODE_KEY = "dark_mode_enabled"

    fun applyTheme(activity: Activity) {
        val sharedPreferences = activity.getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, true)

        if (isDarkMode) {
            activity.setTheme(R.style.Theme_Streakly_Dark)
        } else {
            activity.setTheme(R.style.Theme_Streakly_Light)
        }
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(DARK_MODE_KEY, enabled).apply()

        // Notify all activities to restart
        notifyThemeChanged(context)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(DARK_MODE_KEY, true)
    }

    private fun notifyThemeChanged(context: Context) {
        // This will force all activities to restart when they come to foreground
        // You can also use LocalBroadcastManager for more precise control
        val intent = Intent("THEME_CHANGED")
        context.sendBroadcast(intent)
    }
}