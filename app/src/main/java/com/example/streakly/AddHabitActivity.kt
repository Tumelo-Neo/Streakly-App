package com.example.streakly

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.streakly.data.HabitManager
import com.example.streakly.entities.Habit
import com.example.streakly.utils.ThemeManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddHabitActivity : AppCompatActivity() {

    private lateinit var etHabitTitle: EditText
    private lateinit var etCategory: EditText
    private lateinit var rgFrequency: RadioGroup
    private lateinit var daysSelectionLayout: LinearLayout
    private lateinit var daysContainer: LinearLayout
    private lateinit var btnReminderTime: Button
    private lateinit var btnStartDate: Button
    private lateinit var etNotes: EditText
    private lateinit var btnSaveHabit: Button
    private lateinit var btnCancel: Button

    private var selectedReminderTime: Calendar = Calendar.getInstance()
    private var selectedStartDate: Calendar = Calendar.getInstance()
    private val selectedDays = mutableSetOf<Int>() // Store selected day indices (0=Sun, 1=Mon, etc.)
    private val dayAbbreviations = arrayOf("S", "M", "T", "W", "T", "F", "S")

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        initializeViews()
        setupDaySelection()
        setupClickListeners()
    }

    private fun initializeViews() {
        etHabitTitle = findViewById(R.id.etHabitTitle)
        etCategory = findViewById(R.id.etCategory)
        rgFrequency = findViewById(R.id.rgFrequency)
        daysSelectionLayout = findViewById(R.id.daysSelectionLayout)
        daysContainer = findViewById(R.id.daysContainer) // Make sure to add this ID in your layout
        btnReminderTime = findViewById(R.id.btnReminderTime)
        btnStartDate = findViewById(R.id.btnStartDate)
        etNotes = findViewById(R.id.etNotes)
        btnSaveHabit = findViewById(R.id.btnSaveHabit)
        btnCancel = findViewById(R.id.btnCancel)

        updateReminderTimeButton()
        updateStartDateButton()
    }

    private fun setupDaySelection() {
        val dayNames = arrayOf("S", "M", "T", "W", "T", "F", "S")
        val fullDayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        // Create day buttons
        for (i in 0 until 7) {
            val dayButton = LayoutInflater.from(this).inflate(R.layout.item_day_chip, daysContainer, false) as MaterialButton
            dayButton.text = dayNames[i]
            dayButton.tag = i
            dayButton.contentDescription = fullDayNames[i] // For accessibility

            // Set initial unselected state
            updateDayButtonAppearance(dayButton, false)

            dayButton.setOnClickListener {
                toggleDaySelection(i, dayButton)
            }

            daysContainer.addView(dayButton)
        }
    }

    private fun toggleDaySelection(dayIndex: Int, button: MaterialButton) {
        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex)
            updateDayButtonAppearance(button, false)
        } else {
            selectedDays.add(dayIndex)
            updateDayButtonAppearance(button, true)
        }
    }

    private fun updateDayButtonAppearance(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            // Selected state - colored background, white text
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
            button.strokeWidth = 0
        } else {
            // Unselected state - transparent background, colored text with border
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            button.setTextColor(ContextCompat.getColor(this, R.color.teal_200))
            button.strokeWidth = 2
        }
    }

    private fun resetDayButtons() {
        selectedDays.clear()
        for (i in 0 until daysContainer.childCount) {
            val button = daysContainer.getChildAt(i) as MaterialButton
            updateDayButtonAppearance(button, false)
        }
    }



    private fun setupClickListeners() {
        btnReminderTime.setOnClickListener { showTimePicker() }
        btnStartDate.setOnClickListener { showDatePicker() }
        btnSaveHabit.setOnClickListener { saveHabit() }
        btnCancel.setOnClickListener { finish() }

        // Show/hide days selection based on frequency
        rgFrequency.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbCustom -> {
                    daysSelectionLayout.visibility = LinearLayout.VISIBLE
                }
                else -> {
                    daysSelectionLayout.visibility = LinearLayout.GONE
                    selectedDays.clear()
                    resetDayButtons()
                }
            }
        }
    }



    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedReminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedReminderTime.set(Calendar.MINUTE, minute)
                updateReminderTimeButton()
            },
            selectedReminderTime.get(Calendar.HOUR_OF_DAY),
            selectedReminderTime.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedStartDate.set(Calendar.YEAR, year)
                selectedStartDate.set(Calendar.MONTH, month)
                selectedStartDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateStartDateButton()
            },
            selectedStartDate.get(Calendar.YEAR),
            selectedStartDate.get(Calendar.MONTH),
            selectedStartDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateReminderTimeButton() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        btnReminderTime.text = timeFormat.format(selectedReminderTime.time)
    }

    private fun updateStartDateButton() {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        btnStartDate.text = dateFormat.format(selectedStartDate.time)
    }

    private fun saveHabit() {
        val title = etHabitTitle.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val notes = etNotes.text.toString().trim()
        val userId = HabitManager.getCurrentUserId()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a habit title", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(this, "Please login to create habits", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected frequency
        val frequency = when (rgFrequency.checkedRadioButtonId) {
            R.id.rbDaily -> "Daily"
            R.id.rbWeekly -> "Weekly"
            R.id.rbCustom -> "Custom"
            else -> "Daily"
        }

        // For custom frequency, validate days selection
        val selectedDaysString = if (frequency == "Custom") {
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDays.sorted().joinToString(",")
        } else {
            ""
        }

        // Use the actual reminder time set by the user
        val reminderTime = selectedReminderTime.timeInMillis

        // Create and save habit
        val newHabit = Habit(
            userId = userId,
            title = title,
            category = category,
            frequency = frequency,
            selectedDays = selectedDaysString,
            reminderTime = reminderTime,
            startDate = selectedStartDate.timeInMillis,
            notes = notes
        )

        // Format the reminder time for display
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val reminderTimeText = timeFormat.format(Date(reminderTime))

        // Use coroutine to save to database
        CoroutineScope(Dispatchers.Main).launch {
            HabitManager.addHabit(newHabit)
            Toast.makeText(
                this@AddHabitActivity,
                "Habit '$title' saved!\nReminder set for $reminderTimeText",
                Toast.LENGTH_LONG
            ).show()

            // Navigate back to MainActivity
            val intent = Intent(this@AddHabitActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
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