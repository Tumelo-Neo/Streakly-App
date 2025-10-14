package com.example.streakly

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.streakly.utils.ThemeManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ensure HabitManager is initialized
        if (applicationContext is StreaklyApplication) {
            // It should already be initialized in Application class
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: try to start MainActivity directly
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }, 2000)
    }
}