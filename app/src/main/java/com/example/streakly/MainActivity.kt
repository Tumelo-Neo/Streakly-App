package com.example.streakly

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.streakly.data.HabitManager
import com.example.streakly.entities.Habit
import com.example.streakly.utils.NetworkMonitor
import com.example.streakly.utils.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var adapter: HabitAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var networkMonitor: NetworkMonitor
    private var offlineStatusView: TextView? = null


    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "THEME_CHANGED") {
                recreate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (!HabitManager.isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        // Register theme change receiver
        val filter = IntentFilter("THEME_CHANGED")
        registerReceiver(themeChangeReceiver, filter)

        setupViews()
        setupToolbar()
        setupOfflineIndicator()
        setupRecyclerView()
        setupSwipeToDelete()
        setupBottomNavigation()
        setupFab()
        observeHabits()

        // Start network monitoring
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
        unregisterReceiver(themeChangeReceiver)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.rvHabits)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabAddHabit = findViewById(R.id.fabAddHabit)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val titleView = TextView(this).apply {
            text = "Streakly"
            setTextColor(getColor(android.R.color.white))
            textSize = 24f
            setTypeface(Typeface.create("cursive", Typeface.BOLD))
            setPadding(16, 0, 0, 0)
        }

        toolbar.addView(titleView)
    }

    private fun setupOfflineIndicator() {
        // Create offline status view
        offlineStatusView = TextView(this).apply {
            text = "🔴 Offline - Changes will sync when online"
            setTextColor(Color.RED)
            textSize = 12f
            setPadding(16, 8, 16, 8)
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.offline_background)
            visibility = if (HabitManager.isOnline()) View.GONE else View.VISIBLE
        }

        // Add to toolbar
        toolbar.addView(offlineStatusView)
    }

    private fun updateOfflineStatus() {
        offlineStatusView?.visibility = if (HabitManager.isOnline()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                manualSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun manualSync() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                HabitManager.syncOfflineActions()
                val pendingCount = HabitManager.getPendingActionsCount()
                updateOfflineStatus()
                if (pendingCount == 0) {
                    Toast.makeText(this@MainActivity, "All changes synced!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Syncing $pendingCount changes...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeHabits() {
        lifecycleScope.launch {
            HabitManager.getUserHabitsFlow().collectLatest { habits ->
                updateHabitList(habits)
                updateOfflineStatus()
            }
        }
    }

    private fun updateHabitList(habits: List<Habit>) {
        adapter.updateHabits(habits)

        if (habits.isEmpty()) {
            recyclerView.visibility = android.view.View.GONE
            findViewById<android.widget.TextView>(R.id.tvEmptyState)?.visibility = android.view.View.VISIBLE
        } else {
            recyclerView.visibility = android.view.View.VISIBLE
            findViewById<android.widget.TextView>(R.id.tvEmptyState)?.visibility = android.view.View.GONE
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val habit = adapter.habits[position]

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Habit")
                    .setMessage("Are you sure you want to delete '${habit.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        HabitManager.deleteHabit(habit.id)
                        Toast.makeText(this@MainActivity, "Habit deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        observeHabits()
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onResume() {
        super.onResume()
        refreshHabits()
        updateOfflineStatus()
    }



    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HabitAdapter(emptyList(),
            onItemClick = { habit ->
                // Navigate to habit detail
                val intent = Intent(this@MainActivity, HabitDetailActivity::class.java)
                intent.putExtra("HABIT_ID", habit.id)
                startActivity(intent)
            },
            onCompleteClick = { habit, isCompleted ->
                // Handle quick completion - use postDelayed to avoid RecyclerView conflicts
                recyclerView.postDelayed({
                    handleQuickCompletion(habit, isCompleted)
                }, 100)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun handleQuickCompletion(habit: Habit, isCompleted: Boolean) {
        if (isCompleted) {
            // Mark habit as completed
            HabitManager.markHabitCompleted(habit.id)
            showCompletionToast(habit.title)

            // Refresh the list after a short delay to allow state to update
            lifecycleScope.launch {
                delay(300) // Small delay to ensure database is updated
                refreshHabits()
            }
        } else {
            // For unchecking, we'll just refresh to show correct state
            // In a future version, we might implement "undo completion"
            Toast.makeText(this, "Use habit detail to modify completion", Toast.LENGTH_SHORT).show()
            refreshHabits()
        }
    }

    private fun showCompletionToast(habitTitle: String) {
        Toast.makeText(
            this,
            "Completed: $habitTitle! 🔥",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun refreshHabits() {
        lifecycleScope.launch {
            try {
                // Use a small delay to ensure any pending operations complete
                delay(100)
                observeHabits()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        fabAddHabit.setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
        }
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


}

class HabitAdapter(
    internal var habits: List<Habit>,
    private val onItemClick: (Habit) -> Unit,
    private val onCompleteClick: (Habit, Boolean) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvHabitTitle: android.widget.TextView = itemView.findViewById(R.id.tvHabitTitle)
        val tvHabitFrequency: android.widget.TextView = itemView.findViewById(R.id.tvHabitFrequency)
        val tvStreak: android.widget.TextView = itemView.findViewById(R.id.tvStreak)
        val cbComplete: CheckBox = itemView.findViewById(R.id.cbComplete)
        val ivArrow: android.widget.ImageView = itemView.findViewById(R.id.ivArrow)
        val progressRing: android.widget.ProgressBar = itemView.findViewById(R.id.progressRing)
    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HabitViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        holder.tvHabitTitle.text = habit.title
        holder.tvHabitFrequency.text = habit.frequency
        holder.tvStreak.text = habit.streakCount.toString()

        // Calculate and set progress
        val progress = calculateHabitProgress(habit)
        holder.progressRing.progress = progress

        // Remove any existing listeners to prevent duplicates
        holder.cbComplete.setOnCheckedChangeListener(null)

        // Check if habit is already completed today
        val isCompletedToday = isHabitCompletedToday(habit)
        holder.cbComplete.isChecked = isCompletedToday

        // Set completion checkbox listener
        holder.cbComplete.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isCompletedToday) {
                onCompleteClick(habit, isChecked)
            }
        }

        // Set click listener for the entire item (for detail view)
        holder.itemView.setOnClickListener { onItemClick(habit) }

        // Also make arrow clickable for detail view
        holder.ivArrow.setOnClickListener { onItemClick(habit) }
    }

    private fun calculateHabitProgress(habit: Habit): Int {
        return when (habit.frequency) {
            "Daily" -> calculateDailyProgress(habit)
            "Weekly" -> calculateWeeklyProgress(habit)
            "Custom" -> calculateCustomProgress(habit)
            else -> calculateDailyProgress(habit)
        }
    }

    private fun calculateDailyProgress(habit: Habit): Int {
        // For daily habits, progress is 100% if completed today, 0% otherwise
        return if (isHabitCompletedToday(habit)) 100 else 0
    }

    private fun calculateWeeklyProgress(habit: Habit): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)

        // For weekly habits, calculate progress based on days passed in week
        // Sunday = 1, Monday = 2, ..., Saturday = 7
        val daysPassed = today - 1 // Start from Monday as day 1
        val weeklyProgress = (daysPassed * 100) / 7

        // If completed this week, show current progress, otherwise 0
        return if (isHabitCompletedThisWeek(habit)) weeklyProgress else 0
    }

    private fun calculateCustomProgress(habit: Habit): Int {
        // For custom habits, calculate based on selected days in the week
        if (habit.selectedDays.isEmpty()) return 0

        val selectedDays = habit.selectedDays.split(",").map { it.toInt() }
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0-6 (Sun-Sat)

        // Count how many selected days have passed this week (including today)
        val daysPassed = selectedDays.count { day ->
            day <= today || isHabitCompletedOnDay(habit, day)
        }

        return (daysPassed * 100) / selectedDays.size
    }

    private fun isHabitCompletedThisWeek(habit: Habit): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        return habit.lastCompleted?.let { lastCompleted ->
            val completedCalendar = Calendar.getInstance().apply {
                timeInMillis = lastCompleted
            }
            val completedWeek = completedCalendar.get(Calendar.WEEK_OF_YEAR)
            val completedYear = completedCalendar.get(Calendar.YEAR)

            completedWeek == currentWeek && completedYear == currentYear
        } ?: false
    }

    private fun isHabitCompletedOnDay(habit: Habit, dayOfWeek: Int): Boolean {
        // Check if habit was completed on a specific day of week (0-6, Sun-Sat)
        return habit.lastCompleted?.let { lastCompleted ->
            val completedCalendar = Calendar.getInstance().apply {
                timeInMillis = lastCompleted
            }
            val completedDay = completedCalendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0-6

            completedDay == dayOfWeek
        } ?: false
    }

    private fun isHabitCompletedToday(habit: Habit): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return habit.lastCompleted?.let { lastCompleted ->
            val lastCompletedDate = Calendar.getInstance().apply {
                timeInMillis = lastCompleted
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            lastCompletedDate == today
        } ?: false
    }


    override fun getItemCount(): Int = habits.size



    fun updateHabitsSafely(newHabits: List<Habit>) {
        // Check if we're on main thread and RecyclerView is not computing layout
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // If on main thread, check if RecyclerView is in layout
            val recyclerView = this.recyclerView
            if (recyclerView != null && !recyclerView.isComputingLayout) {
                this.habits = newHabits
                notifyDataSetChanged()
            } else {
                // Post to the next frame
                recyclerView?.post {
                    this.habits = newHabits
                    notifyDataSetChanged()
                }
            }
        } else {
            // If on background thread, post to main thread
            Handler(Looper.getMainLooper()).post {
                updateHabitsSafely(newHabits)
            }
        }
    }

    // Keep the original updateHabits for compatibility
    fun updateHabits(newHabits: List<Habit>) {
        updateHabitsSafely(newHabits)
    }



}