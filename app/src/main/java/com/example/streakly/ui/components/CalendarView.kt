package com.example.streakly.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.streakly.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

class CalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var completionDates: Set<Long> = emptySet()
    private var currentMonth: Calendar = Calendar.getInstance()
    private var today: Long = System.currentTimeMillis()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var cellSize = 0f
    private var headerHeight = 0f
    private var dayHeaderHeight = 0f

    private val dayHeaders = arrayOf("S", "M", "T", "W", "T", "F", "S")
    private val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Colors
    private var completedColor = Color.parseColor("#4CAF50")
    private var missedColor = Color.parseColor("#FF6B6B")
    private var todayColor = Color.parseColor("#2196F3")
    private var textColor = Color.BLACK
    private var headerTextColor = Color.WHITE
    private var gridColor = Color.parseColor("#E0E0E0")

    init {
        setupPaints()
        currentMonth.set(Calendar.DAY_OF_MONTH, 1)
    }

    private fun setupPaints() {
        // Cell paint
        paint.style = Paint.Style.FILL

        // Text paint
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 42f

        // Header paint
        headerPaint.style = Paint.Style.FILL
        headerPaint.color = Color.parseColor("#01333C")
        headerPaint.textAlign = Paint.Align.CENTER
        headerPaint.textSize = 48f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun setCompletionDates(dates: Set<Long>) {
        this.completionDates = dates
        invalidate()
    }

    fun setColors(
        completedColor: Int,
        missedColor: Int,
        todayColor: Int,
        textColor: Int,
        headerTextColor: Int,
        gridColor: Int
    ) {
        this.completedColor = completedColor
        this.missedColor = missedColor
        this.todayColor = todayColor
        this.textColor = textColor
        this.headerTextColor = headerTextColor
        this.gridColor = gridColor
        setupPaints()
        invalidate()
    }

    fun previousMonth() {
        currentMonth.add(Calendar.MONTH, -1)
        invalidate()
    }

    fun nextMonth() {
        currentMonth.add(Calendar.MONTH, 1)
        invalidate()
    }

    fun getCurrentMonthYear(): String {
        val month = monthNames[currentMonth.get(Calendar.MONTH)]
        val year = currentMonth.get(Calendar.YEAR)
        return String.format(context.getString(R.string.month_year_format), month, year)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        headerHeight = h * 0.12f
        dayHeaderHeight = h * 0.08f
        val availableHeight = h - headerHeight - dayHeaderHeight
        cellSize = min(w / 7f, availableHeight / 6f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawHeader(canvas)
        drawDayHeaders(canvas)
        drawCalendarGrid(canvas)
    }

    private fun drawHeader(canvas: Canvas) {
        // Draw header background
        headerPaint.color = Color.parseColor("#01333C")
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

        // Draw month and year
        headerPaint.color = headerTextColor
        headerPaint.textSize = 52f
        canvas.drawText(
            getCurrentMonthYear(),
            width / 2f,
            headerHeight * 0.6f,
            headerPaint
        )
    }

    private fun drawDayHeaders(canvas: Canvas) {
        val startY = headerHeight

        // Draw day headers background
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(0f, startY, width.toFloat(), startY + dayHeaderHeight, paint)

        // Draw day headers
        textPaint.color = textColor
        textPaint.textSize = 40f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        for (i in 0 until 7) {
            val x = (i * cellSize) + (cellSize / 2)
            val y = startY + (dayHeaderHeight * 0.7f)
            canvas.drawText(dayHeaders[i], x, y, textPaint)
        }
    }

    private fun drawCalendarGrid(canvas: Canvas) {
        val startY = headerHeight + dayHeaderHeight

        // Get first day of month and number of days
        val calendar = currentMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Draw calendar cells
        for (day in 1..daysInMonth) {
            val position = firstDayOfWeek + (day - 1)
            val row = position / 7
            val col = position % 7

            val x = col * cellSize
            val y = startY + (row * cellSize)

            drawCalendarCell(canvas, x, y, day, calendar)
        }

        // Draw grid lines
        drawGridLines(canvas, startY, daysInMonth, firstDayOfWeek)
    }

    private fun drawCalendarCell(canvas: Canvas, x: Float, y: Float, day: Int, monthCalendar: Calendar) {
        val cellCalendar = monthCalendar.clone() as Calendar
        cellCalendar.set(Calendar.DAY_OF_MONTH, day)

        val isToday = isSameDay(cellCalendar.timeInMillis, today)
        val isCompleted = completionDates.any { isSameDay(it, cellCalendar.timeInMillis) }
        val isFuture = cellCalendar.timeInMillis > today

        // Draw cell background
        when {
            isToday -> {
                paint.color = todayColor
                canvas.drawCircle(x + cellSize/2, y + cellSize/2, cellSize/2 - 4, paint)
            }
            isCompleted -> {
                paint.color = completedColor
                canvas.drawCircle(x + cellSize/2, y + cellSize/2, cellSize/2 - 4, paint)
            }
            isFuture -> {
                // Future dates are slightly faded
                paint.color = Color.parseColor("#E0E0E0")
                canvas.drawCircle(x + cellSize/2, y + cellSize/2, cellSize/2 - 4, paint)
            }
            else -> {
                // Past dates that weren't completed
                paint.color = missedColor
                canvas.drawCircle(x + cellSize/2, y + cellSize/2, cellSize/2 - 4, paint)
            }
        }

        // Draw day number
        textPaint.color = when {
            isToday -> Color.WHITE
            isCompleted -> Color.WHITE
            isFuture -> Color.parseColor("#B0B0B0")
            else -> Color.WHITE
        }
        textPaint.textSize = 36f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText(
            day.toString(),
            x + cellSize / 2,
            y + (cellSize / 2) + 12,
            textPaint
        )
    }

    private fun drawGridLines(canvas: Canvas, startY: Float, daysInMonth: Int, firstDayOfWeek: Int) {
        paint.color = gridColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        val rows = ceil((firstDayOfWeek + daysInMonth) / 7.0).toInt()

        // Draw vertical lines
        for (i in 0..7) {
            val x = i * cellSize
            canvas.drawLine(x, startY, x, startY + (rows * cellSize), paint)
        }

        // Draw horizontal lines
        for (i in 0..rows) {
            val y = startY + (i * cellSize)
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }

        paint.style = Paint.Style.FILL
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 1.2f).toInt() // 1.2:1 aspect ratio
        setMeasuredDimension(width, height)
    }
}