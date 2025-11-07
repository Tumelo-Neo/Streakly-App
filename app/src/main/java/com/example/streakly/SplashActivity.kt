package com.example.streakly

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.streakly.data.HabitManager
import com.example.streakly.utils.ThemeManager
import java.util.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ensure HabitManager is initialized
        if (applicationContext is StreaklyApplication) {
            // It should already be initialized in Application class
        }

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToAppropriateScreen()
        }, 2000)
    }

    private fun applySavedLanguage() {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("app_language", "en") ?: "en"

        val locale = Locale(savedLanguage)
        Locale.setDefault(locale)

        val resources = resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            applicationContext.createConfigurationContext(configuration)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun navigateToAppropriateScreen() {
        try {
            when {
                HabitManager.isUserLoggedIn() -> {
                    // User is logged in, go to main activity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                HabitManager.isCloudSyncEnabled() -> {
                    // User has Firebase auth but no local session, sync and go to main
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                else -> {
                    // User not logged in, go to SSO login
                    val intent = Intent(this, SsoLoginActivity::class.java)
                    startActivity(intent)
                }
            }
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to SSO login
            try {
                val intent = Intent(this, SsoLoginActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }


}