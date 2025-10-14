package com.example.streakly

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.streakly.data.HabitManager
import com.example.streakly.entities.Habit
import com.example.streakly.ui.components.CalendarView
import com.example.streakly.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class HabitDetailActivity : AppCompatActivity() {

    private lateinit var habit: Habit
    private lateinit var calendarView: CalendarView
    private lateinit var btnPrevMonth: Button
    private lateinit var btnNextMonth: Button
    private lateinit var tvMonthYear: TextView
    private lateinit var legendContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_detail)

        // Initialize calendar views FIRST
        initializeCalendarViews()

        // Get habit ID from intent
        val habitId = intent.getStringExtra("HABIT_ID")
        if (habitId == null) {
            showErrorAndFinish("Habit not found")
            return
        }

        loadHabitData(habitId)
        setupClickListeners()
        setupCalendarColors()
    }

    private fun showErrorAndFinish(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadHabitData(habitId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val loadedHabit = HabitManager.getHabitById(habitId)
            if (loadedHabit != null) {
                habit = loadedHabit
                updateUI()
                loadCompletionHistory()
            } else {
                showErrorAndFinish("Habit not found")
            }
        }
    }

    private fun setupClickListeners() {
        // Complete button
        findViewById<Button>(R.id.btnComplete).setOnClickListener {
            markHabitComplete()
        }

        // Calendar navigation buttons - these are now initialized in initializeCalendarViews()
        btnPrevMonth.setOnClickListener {
            calendarView.previousMonth()
            updateCalendarHeader()
        }

        btnNextMonth.setOnClickListener {
            calendarView.nextMonth()
            updateCalendarHeader()
        }
    }

    private fun setupCalendarColors() {
        val isDarkMode = ThemeManager.isDarkModeEnabled(this)

        val completedColor = Color.parseColor("#4CAF50")
        val missedColor = Color.parseColor("#FF6B6B")
        val todayColor = Color.parseColor("#2196F3")
        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val headerTextColor = Color.WHITE
        val gridColor = if (isDarkMode) Color.parseColor("#666666") else Color.parseColor("#E0E0E0")

        calendarView.setColors(
            completedColor,
            missedColor,
            todayColor,
            textColor,
            headerTextColor,
            gridColor
        )
    }

    private fun updateUI() {
        // Set basic info
        findViewById<TextView>(R.id.tvHabitTitle).text = habit.title
        findViewById<TextView>(R.id.tvCurrentStreak).text = habit.streakCount.toString()
        findViewById<TextView>(R.id.tvFrequency).text = habit.frequency
        findViewById<TextView>(R.id.tvCategory).text = habit.category.ifEmpty { "Uncategorized" }

        // Format start date
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        findViewById<TextView>(R.id.tvStartDate).text = dateFormat.format(Date(habit.startDate))

        // Set notes
        val notes = habit.notes.ifEmpty { "No notes" }
        findViewById<TextView>(R.id.tvNotes).text = notes

        val frequencyText = if (habit.frequency == "Custom" && habit.selectedDays.isNotEmpty()) {
            val days = habit.selectedDays.split(",").map { dayIndex ->
                when (dayIndex.toInt()) {
                    0 -> "Sun"
                    1 -> "Mon"
                    2 -> "Tue"
                    3 -> "Wed"
                    4 -> "Thu"
                    5 -> "Fri"
                    6 -> "Sat"
                    else -> ""
                }
            }
            "Custom (${days.joinToString(", ")})"
        } else {
            habit.frequency
        }

        findViewById<TextView>(R.id.tvFrequency).text = frequencyText

        // Update complete button text based on today's completion status
        updateCompleteButton()
    }

    private fun initializeCalendarViews() {
        calendarView = findViewById(R.id.calendarView)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        legendContainer = findViewById(R.id.legendContainer)

        updateCalendarHeader()
        setupLegend()
    }

    private fun updateCalendarHeader() {
        tvMonthYear.text = calendarView.getCurrentMonthYear()
    }

    private fun setupLegend() {
        legendContainer.removeAllViews()

        val legendItems = listOf(
            LegendItem("Completed", Color.parseColor("#4CAF50")),
            LegendItem("Missed", Color.parseColor("#FF6B6B")),
            LegendItem("Today", Color.parseColor("#2196F3")),
            LegendItem("Future", Color.parseColor("#E0E0E0"))
        )

        val inflater = LayoutInflater.from(this)

        legendItems.forEach { item ->
            val legendView = inflater.inflate(R.layout.item_calendar_legend, legendContainer, false)

            val colorView = legendView.findViewById<View>(R.id.colorView)
            val textView = legendView.findViewById<TextView>(R.id.legendText)

            colorView.setBackgroundColor(item.color)
            textView.text = item.label
            textView.setTextColor(if (ThemeManager.isDarkModeEnabled(this)) Color.WHITE else Color.BLACK)

            legendContainer.addView(legendView)
        }
    }

    private fun loadCompletionHistory() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get completion dates for this habit
                val completionDates = getCompletionDatesForHabit()
                calendarView.setCompletionDates(completionDates)

                // Update recent activity text
                updateRecentActivityText(completionDates)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getCompletionDatesForHabit(): Set<Long> {
        val completionDates = mutableSetOf<Long>()

        // Get habit instances for this habit
        // Note: This is a simplified implementation. In a real app, you'd query the HabitInstance table
        // For now, we'll use the lastCompleted field and streak count to simulate completion history

        if (habit.lastCompleted != null) {
            // Add the last completed date
            completionDates.add(getStartOfDay(habit.lastCompleted!!))

            // Simulate previous completions based on streak count
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = habit.lastCompleted!!

            for (i in 1 until min(habit.streakCount, 30)) { // Show up to 30 days of history
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                completionDates.add(getStartOfDay(calendar.timeInMillis))
            }
        }

        return completionDates
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun updateRecentActivityText(completionDates: Set<Long>) {
        val recentActivityView = findViewById<TextView>(R.id.tvRecentActivity)

        if (completionDates.isEmpty()) {
            recentActivityView.text = getString(R.string.no_completion_history)
            return
        }

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val sortedDates = completionDates.sortedDescending().take(5) // Show last 5 completions

        val activityText = StringBuilder()
        activityText.append("Recent Completions:\n")

        sortedDates.forEach { date ->
            activityText.append("• ${dateFormat.format(Date(date))}\n")
        }

        recentActivityView.text = activityText.toString()
    }

    private fun updateCompleteButton() {
        val completeButton = findViewById<Button>(R.id.btnComplete)
        // For now, simple implementation - always show "Mark as Complete"
        // Later we can check if already completed today
        completeButton.text = "Mark as Complete"
    }

    private fun markHabitComplete() {
        CoroutineScope(Dispatchers.Main).launch {
            HabitManager.markHabitCompleted(habit.id)
            android.widget.Toast.makeText(
                this@HabitDetailActivity,
                "Completed: ${habit.title}",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // Refresh the streak count
            val updatedHabit = HabitManager.getHabitById(habit.id)
            if (updatedHabit != null) {
                habit = updatedHabit
                findViewById<TextView>(R.id.tvCurrentStreak).text = habit.streakCount.toString()
            }

            updateCompleteButton()
            loadCompletionHistory() // Refresh the calendar
        }
    }

    data class LegendItem(val label: String, val color: Int)
}