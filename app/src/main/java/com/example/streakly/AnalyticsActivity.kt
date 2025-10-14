package com.example.streakly

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.streakly.data.HabitManager
import com.example.streakly.entities.Habit
import com.example.streakly.ui.components.LineChartView
import com.example.streakly.ui.components.PieChartView
import com.example.streakly.utils.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.util.*
import java.util.Calendar

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var tvTotalHabits: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var llWeeklyProgress: LinearLayout
    private lateinit var llHabitDistribution: LinearLayout
    private lateinit var llStreakLeaderboard: LinearLayout
    private lateinit var heatmapContainer: LinearLayout
    private lateinit var tvHeatmapTitle: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var completionHistoryChart: LineChartView
    private lateinit var tvChartTitle: TextView
    private lateinit var categoryPieChart: PieChartView // NEW: Pie chart

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics_fallback)

        initializeViews()
        setupBottomNavigation()
        loadAnalyticsData()
    }

    private fun initializeViews() {
        tvTotalHabits = findViewById(R.id.tvTotalHabits)
        tvCompletionRate = findViewById(R.id.tvCompletionRate)
        llWeeklyProgress = findViewById(R.id.llWeeklyProgress)
        llHabitDistribution = findViewById(R.id.llHabitDistribution)
        llStreakLeaderboard = findViewById(R.id.llStreakLeaderboard)
        heatmapContainer = findViewById(R.id.heatmapContainer)
        tvHeatmapTitle = findViewById(R.id.tvHeatmapTitle)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Initialize line chart
        completionHistoryChart = findViewById(R.id.completionHistoryChart)
        tvChartTitle = findViewById(R.id.tvChartTitle)

        // NEW: Initialize pie chart
        categoryPieChart = findViewById(R.id.categoryPieChart)

        // Set chart colors based on theme
        val lineColor = if (ThemeManager.isDarkModeEnabled(this)) {
            Color.parseColor("#03DAC5")
        } else {
            Color.parseColor("#01333C")
        }
        val gridColor = if (ThemeManager.isDarkModeEnabled(this)) {
            Color.parseColor("#80FFFFFF")
        } else {
            Color.parseColor("#80000000")
        }
        val textColor = if (ThemeManager.isDarkModeEnabled(this)) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        completionHistoryChart.setColors(lineColor, gridColor, textColor)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.navigation_analytics
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
                    true
                }
                R.id.navigation_analytics -> {
                    true
                }
                R.id.navigation_settings -> {
                    val intent = android.content.Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadAnalyticsData() {
        lifecycleScope.launch {
            try {
                HabitManager.getUserHabitsFlow().collect { habitsList ->
                    updateAnalyticsUI(habitsList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateAnalyticsUI(habits: List<Habit>) {
        // Basic stats
        tvTotalHabits.text = habits.size.toString()

        // Calculate real completion rate based on actual weekly completions
        val completionRate = calculateWeeklyCompletionRate(habits)
        tvCompletionRate.text = "$completionRate%"

        // Load visualizations with real data
        loadWeeklyProgress(habits)
        loadHabitDistribution(habits)
        loadStreakLeaderboard(habits)
        loadCompletionHeatmap(habits)
        loadCompletionHistoryChart(habits)
        loadCategoryPieChart(habits) // NEW: Load pie chart
    }

    // NEW: Load category distribution pie chart
    private fun loadCategoryPieChart(habits: List<Habit>) {
        if (habits.isEmpty()) {
            categoryPieChart.setData(emptyList())
            return
        }

        val categoryData = generateCategoryData(habits)
        categoryPieChart.setCategoryData(categoryData)
    }

    // NEW: Generate category distribution data
    private fun generateCategoryData(habits: List<Habit>): Map<String, Int> {
        val categoryMap = mutableMapOf<String, Int>()

        habits.forEach { habit ->
            val category = if (habit.category.isNullOrEmpty()) {
                getString(R.string.uncategorized)
            } else {
                habit.category
            }

            categoryMap[category] = categoryMap.getOrDefault(category, 0) + 1
        }

        return categoryMap
    }

    // ... REST OF THE EXISTING METHODS REMAIN THE SAME (including loadCompletionHistoryChart, etc.) ...

    private fun loadCompletionHistoryChart(habits: List<Habit>) {
        if (habits.isEmpty()) {
            completionHistoryChart.setData(emptyList(), emptyList())
            return
        }

        // Generate completion data for last 7 days
        val completionData = generateCompletionHistoryData(habits, 7)
        val labels = generateDateLabels(7)

        tvChartTitle.text = getString(R.string.last_7_days)
        completionHistoryChart.setData(completionData, labels)
    }

    private fun generateCompletionHistoryData(habits: List<Habit>, days: Int): List<Float> {
        val completionRates = mutableListOf<Float>()
        val calendar = Calendar.getInstance()

        for (i in days - 1 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val targetDate = calendar.timeInMillis

            // Calculate completion rate for this day
            val dayCompletionRate = calculateDailyCompletionRate(habits, targetDate)
            completionRates.add(dayCompletionRate)
        }

        return completionRates
    }

    private fun calculateDailyCompletionRate(habits: List<Habit>, date: Long): Float {
        if (habits.isEmpty()) return 0f

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date

        var completedCount = 0

        habits.forEach { habit ->
            // Check if habit was completed on this specific day
            if (habit.lastCompleted != null) {
                val completionCalendar = Calendar.getInstance()
                completionCalendar.timeInMillis = habit.lastCompleted!!

                if (completionCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    completionCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) {
                    completedCount++
                }
            }
        }

        return (completedCount.toFloat() / habits.size.toFloat()) * 100f
    }

    private fun generateDateLabels(days: Int): List<String> {
        val labels = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("MM/dd", Locale.getDefault())

        for (i in days - 1 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            labels.add(dateFormat.format(Date(calendar.timeInMillis)))
        }

        return labels
    }

    private fun calculateWeeklyCompletionRate(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0

        val weeklyCompletions = getThisWeeksCompletions(habits)
        val totalPossibleCompletions = habits.size * 7

        return if (totalPossibleCompletions > 0) {
            (weeklyCompletions * 100) / totalPossibleCompletions
        } else {
            0
        }
    }

    private fun getThisWeeksCompletions(habits: List<Habit>): Int {
        var totalCompletions = 0

        habits.forEach { habit ->
            if (habit.lastCompleted != null) {
                val daysSinceLastCompletion = calculateDaysSince(habit.lastCompleted!!)
                if (daysSinceLastCompletion <= 7) {
                    when (habit.frequency) {
                        "Daily" -> totalCompletions += minOf(habit.streakCount, 7)
                        "Weekly" -> totalCompletions += 1
                        else -> totalCompletions += 1
                    }
                }
            }
        }

        return totalCompletions
    }

    private fun calculateDaysSince(timestamp: Long): Int {
        val currentTime = System.currentTimeMillis()
        return ((currentTime - timestamp) / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun loadHabitDistribution(habits: List<Habit>) {
        llHabitDistribution.removeAllViews()

        if (habits.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = getString(R.string.no_habits_message)
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            llHabitDistribution.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(this)
        val totalStreaks = habits.sumOf { it.streakCount }

        habits.forEach { habit ->
            val distributionView = inflater.inflate(R.layout.item_habit_distribution, llHabitDistribution, false)

            val tvHabitName = distributionView.findViewById<TextView>(R.id.tvHabitName)
            val progressBar = distributionView.findViewById<ProgressBar>(R.id.progressBar)
            val tvPercentage = distributionView.findViewById<TextView>(R.id.tvPercentage)

            tvHabitName.text = habit.title
            val percentage = if (totalStreaks > 0) {
                (habit.streakCount * 100) / totalStreaks
            } else {
                0
            }
            progressBar.progress = percentage
            tvPercentage.text = "$percentage%"

            llHabitDistribution.addView(distributionView)
        }
    }

    private fun loadStreakLeaderboard(habits: List<Habit>) {
        llStreakLeaderboard.removeAllViews()

        if (habits.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = getString(R.string.no_habits_message)
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            llStreakLeaderboard.addView(emptyView)
            return
        }

        val sortedHabits = habits.sortedByDescending { it.streakCount }
        val inflater = LayoutInflater.from(this)

        sortedHabits.forEachIndexed { index, habit ->
            val leaderboardItem = inflater.inflate(R.layout.item_streak_leaderboard, llStreakLeaderboard, false)

            val tvRank = leaderboardItem.findViewById<TextView>(R.id.tvRank)
            val tvHabitName = leaderboardItem.findViewById<TextView>(R.id.tvHabitName)
            val tvStreakCount = leaderboardItem.findViewById<TextView>(R.id.tvStreakCount)

            tvRank.text = (index + 1).toString()
            tvHabitName.text = habit.title
            tvStreakCount.text = "${habit.streakCount} ${getString(R.string.days)}"

            llStreakLeaderboard.addView(leaderboardItem)
        }
    }

    private fun loadWeeklyProgress(habits: List<Habit>) {
        llWeeklyProgress.removeAllViews()

        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val inflater = LayoutInflater.from(this)

        val weeklyCompletion = calculateThisWeeksCompletion(habits)

        days.forEachIndexed { index, day ->
            val dayProgressView = inflater.inflate(R.layout.item_day_progress, llWeeklyProgress, false)

            val tvDay = dayProgressView.findViewById<TextView>(R.id.tvDay)
            val progressBar = dayProgressView.findViewById<ProgressBar>(R.id.progressBar)
            val tvPercentage = dayProgressView.findViewById<TextView>(R.id.tvPercentage)

            tvDay.text = day

            val progress = weeklyCompletion[index]
            progressBar.progress = progress
            tvPercentage.text = "$progress%"

            llWeeklyProgress.addView(dayProgressView)
        }
    }

    private fun calculateThisWeeksCompletion(habits: List<Habit>): IntArray {
        val weeklyCompletion = IntArray(7) { 0 }

        if (habits.isEmpty()) return weeklyCompletion

        val calendar = Calendar.getInstance()

        for (dayIndex in 0 until 7) {
            calendar.timeInMillis = System.currentTimeMillis()

            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (currentDayOfWeek - 1 - dayIndex + 7) % 7
            calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

            var dayCompletions = 0
            habits.forEach { habit ->
                if (habit.lastCompleted != null) {
                    val completionCalendar = Calendar.getInstance()
                    completionCalendar.timeInMillis = habit.lastCompleted!!

                    if (completionCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                        completionCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) {
                        dayCompletions++
                    }
                }
            }

            weeklyCompletion[dayIndex] = if (habits.isNotEmpty()) {
                (dayCompletions * 100) / habits.size
            } else {
                0
            }
        }

        return weeklyCompletion
    }

    private fun loadCompletionHeatmap(habits: List<Habit>) {
        heatmapContainer.removeAllViews()

        if (habits.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No data available"
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            heatmapContainer.addView(emptyView)
            return
        }

        val heatmapData = generateHeatmapData(habits)
        val inflater = LayoutInflater.from(this)

        for (week in 0 until 5) {
            val weekRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }

            for (day in 0 until 7) {
                val cellIndex = week * 7 + day
                if (cellIndex < 35) {
                    val cellView = inflater.inflate(R.layout.item_heatmap_cell, weekRow, false) as View

                    val completionCount = heatmapData[cellIndex]
                    val color = when {
                        completionCount == 0 -> Color.parseColor("#E0E0E0")
                        completionCount == 1 -> Color.parseColor("#4CAF50")
                        completionCount == 2 -> Color.parseColor("#388E3C")
                        else -> Color.parseColor("#2E7D32")
                    }

                    cellView.setBackgroundColor(color)
                    weekRow.addView(cellView)
                }
            }

            heatmapContainer.addView(weekRow)
        }
    }

    private fun generateHeatmapData(habits: List<Habit>): List<Int> {
        val heatmapData = MutableList(35) { 0 }

        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        for (daysAgo in 0 until 35) {
            calendar.timeInMillis = today
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val targetDate = calendar.timeInMillis

            var dayCompletions = 0
            habits.forEach { habit ->
                if (habit.lastCompleted != null) {
                    val completionCalendar = Calendar.getInstance()
                    completionCalendar.timeInMillis = habit.lastCompleted!!

                    if (completionCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                        completionCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) {
                        dayCompletions++
                    }
                }
            }

            heatmapData[34 - daysAgo] = dayCompletions
        }

        return heatmapData
    }
}