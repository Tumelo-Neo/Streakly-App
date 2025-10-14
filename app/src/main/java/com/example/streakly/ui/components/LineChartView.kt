package com.example.streakly.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.streakly.R
import kotlin.math.max
import kotlin.math.min

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: List<Float> = emptyList()
    private var labels: List<String> = emptyList()
    private var lineColor: Int = Color.parseColor("#03DAC5")
    private var gridColor: Int = Color.parseColor("#80FFFFFF")
    private var textColor: Int = Color.WHITE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var chartPadding = 60f
    private var pointRadius = 8f

    init {
        setupPaints()
    }

    private fun setupPaints() {
        // Line paint
        paint.color = lineColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND

        // Text paint
        textPaint.color = textColor
        textPaint.textSize = 36f
        textPaint.textAlign = Paint.Align.CENTER

        // Grid paint
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
    }

    fun setData(dataPoints: List<Float>, labels: List<String>) {
        this.dataPoints = dataPoints
        this.labels = labels
        invalidate()
    }

    fun setColors(lineColor: Int, gridColor: Int, textColor: Int) {
        this.lineColor = lineColor
        this.gridColor = gridColor
        this.textColor = textColor
        setupPaints()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty() || dataPoints.all { it == 0f }) {
            drawNoDataMessage(canvas)
            return
        }

        drawGrid(canvas)
        drawLineChart(canvas)
        drawDataPoints(canvas)
        drawLabels(canvas)
    }

    private fun drawNoDataMessage(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            context.getString(R.string.no_completion_data),
            width / 2f,
            height / 2f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas) {
        val chartWidth = width - 2 * chartPadding
        val chartHeight = height - 2 * chartPadding

        // Draw vertical grid lines
        for (i in 0 until 5) {
            val x = chartPadding + (i * chartWidth / 4)
            canvas.drawLine(x, chartPadding, x, height - chartPadding, gridPaint)
        }

        // Draw horizontal grid lines
        for (i in 0 until 5) {
            val y = chartPadding + (i * chartHeight / 4)
            canvas.drawLine(chartPadding, y, width - chartPadding, y, gridPaint)
        }
    }

    private fun drawLineChart(canvas: Canvas) {
        if (dataPoints.size < 2) return

        val chartWidth = width - 2 * chartPadding
        val chartHeight = height - 2 * chartPadding
        val maxValue = max(100f, dataPoints.maxOrNull() ?: 100f)

        val path = Path()
        val pointWidth = chartWidth / (dataPoints.size - 1)

        for (i in dataPoints.indices) {
            val x = chartPadding + (i * pointWidth)
            val y = height - chartPadding - ((dataPoints[i] / maxValue) * chartHeight)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, paint)
    }

    private fun drawDataPoints(canvas: Canvas) {
        val chartWidth = width - 2 * chartPadding
        val chartHeight = height - 2 * chartPadding
        val maxValue = max(100f, dataPoints.maxOrNull() ?: 100f)
        val pointWidth = chartWidth / (dataPoints.size - 1)

        paint.style = Paint.Style.FILL

        for (i in dataPoints.indices) {
            val x = chartPadding + (i * pointWidth)
            val y = height - chartPadding - ((dataPoints[i] / maxValue) * chartHeight)

            // Draw point
            canvas.drawCircle(x, y, pointRadius, paint)

            // Draw value text
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "${dataPoints[i].toInt()}%",
                x,
                y - 20f,
                textPaint
            )
        }

        paint.style = Paint.Style.STROKE
    }

    private fun drawLabels(canvas: Canvas) {
        if (labels.size != dataPoints.size) return

        val chartWidth = width - 2 * chartPadding
        val pointWidth = chartWidth / (dataPoints.size - 1)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 32f

        for (i in labels.indices) {
            val x = chartPadding + (i * pointWidth)
            val y = height - chartPadding + 40f

            canvas.drawText(labels[i], x, y, textPaint)
        }
    }
}