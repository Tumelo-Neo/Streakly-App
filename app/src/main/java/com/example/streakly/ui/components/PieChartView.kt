package com.example.streakly.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.streakly.R
import com.example.streakly.utils.ThemeManager
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class PieChartData(
    val category: String,
    val value: Float,
    val color: Int
)

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pieData: List<PieChartData> = emptyList()
    private var selectedSlice: Int = -1

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var legendWidth = 0f
    private var totalWidth = 0f

    // Predefined colors for categories
    private val categoryColors = listOf(
        Color.parseColor("#FF6B6B"), // Red
        Color.parseColor("#4ECDC4"), // Teal
        Color.parseColor("#45B7D1"), // Blue
        Color.parseColor("#96CEB4"), // Green
        Color.parseColor("#FFEAA7"), // Yellow
        Color.parseColor("#DDA0DD"), // Plum
        Color.parseColor("#98D8C8"), // Mint
        Color.parseColor("#F7DC6F"), // Light Yellow
        Color.parseColor("#BB8FCE"), // Light Purple
        Color.parseColor("#85C1E9")  // Light Blue
    )

    init {
        setupPaints()
    }

    private fun setupPaints() {
        // Pie slice paint
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 2f

        // Selected slice paint
        selectedPaint.style = Paint.Style.FILL
        selectedPaint.strokeWidth = 4f

        // Text paint
        textPaint.color = Color.WHITE
        textPaint.textSize = 42f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun setData(data: List<PieChartData>) {
        this.pieData = data
        invalidate()
    }

    fun setCategoryData(categories: Map<String, Int>) {
        val total = categories.values.sum()
        if (total == 0) {
            setData(emptyList())
            return
        }

        val pieDataList = mutableListOf<PieChartData>()
        var colorIndex = 0

        categories.forEach { (category, count) ->
            val percentage = (count.toFloat() / total.toFloat()) * 100f
            val color = categoryColors[colorIndex % categoryColors.size]

            pieDataList.add(PieChartData(category, percentage, color))
            colorIndex++
        }

        setData(pieDataList)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val minDimension = min(w, h)
        radius = minDimension * 0.35f
        centerX = w * 0.35f  // Position pie chart on the left
        centerY = h * 0.5f
        legendWidth = w * 0.55f
        totalWidth = w.toFloat()

        // If we have many categories, increase the total width to accommodate legend
        if (pieData.size > 5) {
            totalWidth = w * 1.5f  // Allow more space for legend
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (totalWidth).toInt()
        val desiredHeight = MeasureSpec.getSize(heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        setMeasuredDimension(width, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (pieData.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }

        drawPieChart(canvas)
        drawLegend(canvas)
    }

    private fun drawNoDataMessage(canvas: Canvas) {
        textPaint.color = if (ThemeManager.isDarkModeEnabled(context)) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 48f

        canvas.drawText(
            context.getString(R.string.no_category_data),
            width / 2f,
            height / 2f,
            textPaint
        )
    }

    private fun drawPieChart(canvas: Canvas) {
        var startAngle = -90f // Start from top

        pieData.forEachIndexed { index, data ->
            val sweepAngle = (data.value / 100f) * 360f

            // Use selected paint if this slice is selected
            val currentPaint = if (index == selectedSlice) {
                selectedPaint.color = adjustColorBrightness(data.color, 0.8f)
                selectedPaint
            } else {
                paint.color = data.color
                paint
            }

            // Draw pie slice
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                true,
                currentPaint
            )

            // Draw percentage text in the center of each slice
            if (sweepAngle > 15f) { // Only draw text if slice is large enough
                val midAngle = startAngle + sweepAngle / 2
                val textRadius = radius * 0.6f
                val textX = centerX + textRadius * cos(Math.toRadians(midAngle.toDouble())).toFloat()
                val textY = centerY + textRadius * sin(Math.toRadians(midAngle.toDouble())).toFloat()

                textPaint.color = getContrastColor(data.color)
                textPaint.textSize = 36f

                canvas.drawText(
                    "${data.value.toInt()}%",
                    textX,
                    textY,
                    textPaint
                )
            }

            startAngle += sweepAngle
        }

        // Draw center circle for donut effect
        paint.color = if (ThemeManager.isDarkModeEnabled(context)) {
            Color.parseColor("#01333C")
        } else {
            Color.WHITE
        }
        canvas.drawCircle(centerX, centerY, radius * 0.4f, paint)
    }

    private fun drawLegend(canvas: Canvas) {
        val legendStartX = width * 0.65f  // Start legend after the pie chart
        var legendY = centerY - (pieData.size * 60f) / 2

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 36f

        pieData.forEachIndexed { index, data ->
            // Draw color box
            paint.color = data.color
            canvas.drawRect(
                legendStartX,
                legendY,
                legendStartX + 40f,
                legendY + 30f,
                paint
            )

            // Draw category name and percentage
            textPaint.color = if (ThemeManager.isDarkModeEnabled(context)) {
                Color.WHITE
            } else {
                Color.BLACK
            }

            val categoryText = if (data.category.isEmpty()) {
                context.getString(R.string.uncategorized)
            } else {
                data.category
            }

            // Truncate long category names for better display
            val displayText = if (categoryText.length > 15) {
                "${categoryText.substring(0, 12)}..."
            } else {
                categoryText
            }

            canvas.drawText(
                "$displayText (${data.value.toInt()}%)",
                legendStartX + 50f,
                legendY + 22f,
                textPaint
            )

            legendY += 60f
        }
    }

    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    private fun getContrastColor(color: Int): Int {
        // Calculate luminance
        val luminance = 0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)
        return if (luminance > 186) Color.BLACK else Color.WHITE
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                handleTouch(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouch(x: Float, y: Float) {
        // Check if touch is within pie chart bounds
        val distance = Math.sqrt(
            Math.pow((x - centerX).toDouble(), 2.0) +
                    Math.pow((y - centerY).toDouble(), 2.0)
        ).toFloat()

        if (distance <= radius) {
            // Calculate angle of touch
            var angle = Math.toDegrees(Math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())).toFloat()
            if (angle < 0) angle += 360f
            angle = (angle + 90f) % 360f // Adjust for starting angle

            // Find which slice was touched
            var currentAngle = 0f
            pieData.forEachIndexed { index, data ->
                val sweepAngle = (data.value / 100f) * 360f
                if (angle >= currentAngle && angle < currentAngle + sweepAngle) {
                    selectedSlice = index
                    invalidate()
                    return
                }
                currentAngle += sweepAngle
            }
        }

        selectedSlice = -1
        invalidate()
    }
}