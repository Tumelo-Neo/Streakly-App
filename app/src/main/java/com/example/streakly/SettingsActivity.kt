package com.example.streakly

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.streakly.data.HabitManager
import com.example.streakly.entities.Habit
import com.example.streakly.utils.ThemeManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchNotifications: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var tvAppVersion: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        setupBottomNavigation()
        setupClickListeners()
        loadSettings()
        setupGoogleSignIn()
        updateCloudSyncUI()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initializeViews() {
        switchNotifications = findViewById(R.id.switchNotifications)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        tvAppVersion = findViewById(R.id.tvAppVersion)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        tvAppVersion.text = "1.0.0"

        // Set initial dark mode switch state
        switchDarkMode.isChecked = ThemeManager.isDarkModeEnabled(this)

        // Display current user info
        displayUserInfo()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.navigation_settings
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
                    true
                }
                R.id.navigation_analytics -> {
                    val intent = Intent(this, AnalyticsActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_settings -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting(isChecked)
            if (isChecked) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            saveDarkModeSetting(isChecked)
            ThemeManager.setDarkModeEnabled(this, isChecked)

            // Restart activity to apply theme change
            recreate()

            if (isChecked) {
                Toast.makeText(this, "Dark mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Light mode enabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Cloud Sync option
        findViewById<android.view.View>(R.id.btnCloudSync)?.setOnClickListener {
            handleCloudSync()
        }

        // Google Sign-In option
        findViewById<android.view.View>(R.id.btnGoogleSignIn)?.setOnClickListener {
            handleGoogleSignIn()
        }

        findViewById<android.view.View>(R.id.btnExportData).setOnClickListener {
            exportDataToDocument()
        }

        findViewById<android.view.View>(R.id.btnClearData).setOnClickListener {
            showClearDataConfirmation()
        }

        findViewById<android.view.View>(R.id.btnRateApp).setOnClickListener {
            showAppStats()
        }

        findViewById<android.view.View>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun handleCloudSync() {
        if (HabitManager.isCloudSyncEnabled()) {
            // Already signed in - show sync options
            showCloudSyncOptions()
        } else {
            // Not signed in - prompt for Google Sign-In
            showSignInPrompt()
        }
    }

    private fun handleGoogleSignIn() {
        if (HabitManager.isCloudSyncEnabled()) {
            // Already signed in - show account info
            showAccountInfo()
        } else {
            // Start Google Sign-In
            val intent = Intent(this, GoogleSignInActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showCloudSyncOptions() {
        val options = arrayOf("Sync to Cloud", "Sync from Cloud", "Sign Out")

        AlertDialog.Builder(this)
            .setTitle("Cloud Sync")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> syncToCloud()
                    1 -> syncFromCloud()
                    2 -> signOutFromCloud()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun syncToCloud() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                HabitManager.syncToCloud()
                Toast.makeText(this@SettingsActivity, "Data synced to cloud!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncFromCloud() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = HabitManager.syncFromCloud()
                if (success) {
                    Toast.makeText(this@SettingsActivity, "Data synced from cloud!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Sync failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOutFromCloud() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out from cloud sync?")
            .setPositiveButton("Sign Out") { _, _ ->
                // Use the public method - FIXED LINE
                HabitManager.signOutFromFirebase()
                googleSignInClient.signOut()
                updateCloudSyncUI()
                Toast.makeText(this@SettingsActivity, "Signed out from cloud", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSignInPrompt() {
        AlertDialog.Builder(this)
            .setTitle("Enable Cloud Sync")
            .setMessage("Sign in with Google to enable cloud synchronization of your habits across devices.")
            .setPositiveButton("Sign In") { _, _ ->
                val intent = Intent(this, GoogleSignInActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showAccountInfo() {
        // Use the public method - FIXED LINE
        val user = HabitManager.getFirebaseUser()
        val message = if (user != null) {
            "Signed in as: ${user.email}\n\nCloud sync is enabled. Your habits will be automatically synchronized across devices."
        } else {
            "Not signed in"
        }

        AlertDialog.Builder(this)
            .setTitle("Account Info")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Sign Out") { _, _ ->
                signOutFromCloud()
            }
            .show()
    }

    private fun updateCloudSyncUI() {
        // Update the cloud sync button text based on sign-in status
        val cloudSyncButton = findViewById<android.view.View>(R.id.btnCloudSync)
        val googleSignInButton = findViewById<android.view.View>(R.id.btnGoogleSignIn)

        val isSignedIn = HabitManager.isCloudSyncEnabled()

        // Update cloud sync button
        cloudSyncButton?.findViewById<TextView>(R.id.tvCloudSyncTitle)?.text =
            if (isSignedIn) "Cloud Sync (Enabled)" else "Cloud Sync"

        // Update Google Sign-In button
        googleSignInButton?.findViewById<TextView>(R.id.tvGoogleSignInTitle)?.text =
            if (isSignedIn) "Google Account" else "Sign in with Google"
    }

    override fun onResume() {
        super.onResume()
        updateCloudSyncUI()
        displayUserInfo()
    }

    private fun loadSettings() {
        // Load settings from SharedPreferences
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        switchNotifications.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
    }

    private fun saveNotificationSetting(enabled: Boolean) {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    private fun saveDarkModeSetting(enabled: Boolean) {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    private fun exportDataToDocument() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Use getUserHabitsFlow() instead of getAllHabitsFlow()
                val habits = HabitManager.getUserHabitsFlow().first()

                if (habits.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "No habits to export!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create CSV content with explicit string building
                val csvContent = StringBuilder().apply {
                    appendLine("Streakly Data Export")
                    appendLine("User: ${HabitManager.getCurrentUserName() ?: "Unknown"}")
                    appendLine("Email: ${HabitManager.getCurrentUserEmail() ?: "Unknown"}")
                    appendLine("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine()
                    appendLine("HABITS")
                    appendLine("Title,Category,Frequency,Streak Count,Start Date,Last Completed,Notes")

                    habits.forEach { habit ->
                        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(habit.startDate))
                        val lastCompleted = habit.lastCompleted?.let { date ->
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
                        } ?: "Never"

                        // Use explicit string building to avoid type inference issues
                        appendLine(
                            "\"${habit.title}\"," +
                                    "\"${habit.category}\"," +
                                    "\"${habit.frequency}\"," +
                                    "${habit.streakCount}," +
                                    "\"$startDate\"," +
                                    "\"$lastCompleted\"," +
                                    "\"${habit.notes}\""
                        )
                    }

                    appendLine()
                    appendLine("SUMMARY")
                    appendLine("Total Habits: ${habits.size}")
                    appendLine("Total Streaks: ${habits.sumOf { it.streakCount }}")
                    val avgStreak = if (habits.isNotEmpty()) habits.sumOf { it.streakCount } / habits.size else 0
                    appendLine("Average Streak: $avgStreak")
                }.toString()

                // Save to file
                val fileName = "streakly_export_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                FileWriter(file).use { writer ->
                    writer.write(csvContent)
                }

                // Share the file
                val fileUri = FileProvider.getUriForFile(
                    this@SettingsActivity,
                    "${packageName}.provider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Streakly Data Export")
                    putExtra(Intent.EXTRA_TEXT, "My Streakly habits data export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Export Streakly Data"))
                Toast.makeText(this@SettingsActivity, "Data exported successfully!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showClearDataConfirmation() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val habitCount = HabitManager.getHabitCount()

                if (habitCount > 0) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Clear All Data")
                        .setMessage("Are you sure you want to clear $habitCount habits? This cannot be undone.")
                        .setPositiveButton("Clear") { _: DialogInterface, _: Int ->
                            clearAllData(habitCount)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    Toast.makeText(this@SettingsActivity, "No data to clear!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error checking data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllData(habitCount: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Clear all data from database
                HabitManager.clearAllData()

                // Also clear settings
                val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()

                // Reset switches to default
                switchNotifications.isChecked = true
                switchDarkMode.isChecked = ThemeManager.isDarkModeEnabled(this@SettingsActivity)

                // Show confirmation
                Toast.makeText(
                    this@SettingsActivity,
                    "Cleared $habitCount habits and all data!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Clear data failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Sign out from Firebase if signed in
        if (HabitManager.isCloudSyncEnabled()) {
            HabitManager.signOutFromFirebase() // Use public method
            googleSignInClient.signOut()
        }

        HabitManager.logoutUser()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showAppStats() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val habitCount = HabitManager.getHabitCount()
                val totalStreaks = HabitManager.getTotalStreaks()
                val userName = HabitManager.getCurrentUserName() ?: "User"

                val message = if (habitCount > 0) {
                    "Hello, $userName! 🌟\n\n" +
                            "• You've created $habitCount ${if (habitCount == 1) "habit" else "habits"}\n" +
                            "• Maintained $totalStreaks total ${if (totalStreaks == 1) "streak" else "streaks"}\n\n" +
                            "Keep building those habits! 💪"
                } else {
                    "Hello, $userName! 🌟\n\n" +
                            "Start building your first habit to see your progress here!\n\n" +
                            "Tap the + button on the home screen to begin."
                }

                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Your Progress")
                    .setMessage(message)
                    .setPositiveButton("Awesome!") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Thank you for using Streakly!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayUserInfo() {
        val tvCurrentUser = findViewById<TextView>(R.id.tvCurrentUser)
        val userName = HabitManager.getCurrentUserName()
        val userEmail = HabitManager.getCurrentUserEmail()

        if (userName != null && userEmail != null) {
            tvCurrentUser.text = "$userName\n$userEmail"
        } else {
            tvCurrentUser.text = "Not logged in"
        }
    }
}