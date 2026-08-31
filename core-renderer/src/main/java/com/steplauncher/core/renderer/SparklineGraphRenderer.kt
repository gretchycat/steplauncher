package com.steplauncher.core.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

object SparklineGraphRenderer {

    private fun subsampleHistory(history: List<Int>, maxPoints: Int = 30): List<Int> {
        val list = synchronized(history) { history.toList() }
        if (list.isEmpty()) return emptyList()
        if (list.size <= maxPoints) return list

        val result = mutableListOf<Int>()
        val step = list.size.toFloat() / maxPoints
        for (i in 0 until maxPoints) {
            val idx = (i * step).toInt().coerceIn(0, list.size - 1)
            result.add(list[idx])
        }
        return result
    }

    /**
     * Draws a high-tech glowing CPU line sparkline graph.
     */
    fun drawCpuLineGraph(width: Int, height: Int, history: List<Int>): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark translucent background card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30000000")
            style = Paint.Style.FILL
        }
        val rect = RectF(4f, 4f, w - 4f, h - 4f)
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

        val dataRaw = subsampleHistory(history, 30)
        if (dataRaw.isEmpty()) return bitmap

        val data = if (dataRaw.size < 2) listOf(dataRaw.first(), dataRaw.first()) else dataRaw
        val stepX = (w - 24f) / (data.size - 1).coerceAtLeast(1)
        val paddingY = 16f
        val usableH = h - (paddingY * 2)

        val path = Path()
        val fillPath = Path()

        val startX = 12f
        val startY = h - paddingY - (data[0].coerceIn(0, 100) / 100f * usableH)
        path.moveTo(startX, startY)
        fillPath.moveTo(startX, h - paddingY)
        fillPath.lineTo(startX, startY)

        for (i in 1 until data.size) {
            val x = startX + (i * stepX)
            val y = h - paddingY - (data[i].coerceIn(0, 100) / 100f * usableH)
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        val lastX = startX + ((data.size - 1) * stepX)
        fillPath.lineTo(lastX, h - paddingY)
        fillPath.close()

        // Draw translucent gradient under line
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, paddingY, 0f, h - paddingY, Color.parseColor("#6000E5FF"), Color.parseColor("#0500E5FF"), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawPath(fillPath, fillPaint)

        // Draw glowing line graph
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, linePaint)

        // Draw active latest data point node
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val lastY = h - paddingY - (data.last().coerceIn(0, 100) / 100f * usableH)
        canvas.drawCircle(lastX, lastY, 6f, dotPaint)

        return bitmap
    }

    /**
     * Draws a segmented vertical RAM bar graph.
     */
    fun drawMemoryBarGraph(width: Int, height: Int, percent: Int): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(4f, 4f, w - 4f, h - 4f), 16f, 16f, bgPaint)

        val numBars = 6
        val barGap = 6f
        val marginX = 14f
        val marginY = 16f
        val barW = (w - (marginX * 2) - (barGap * (numBars - 1))) / numBars
        val usableH = h - (marginY * 2)

        val activeBars = (percent.coerceIn(0, 100) * numBars / 100f).toInt().coerceIn(1, numBars)

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h - marginY, 0f, marginY, Color.parseColor("#00E676"), Color.parseColor("#B2FF59"), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }

        val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30FFFFFF")
            style = Paint.Style.FILL
        }

        for (i in 0 until numBars) {
            val left = marginX + i * (barW + barGap)
            val right = left + barW
            val barH = usableH * ((i + 1).toFloat() / numBars)
            val top = h - marginY - barH

            val paint = if (i < activeBars) activePaint else inactivePaint
            canvas.drawRoundRect(RectF(left, top, right, h - marginY), 4f, 4f, paint)
        }

        return bitmap
    }

    /**
     * Draws a sleek circular arc storage gauge graph.
     */
    fun drawStorageGaugeGraph(width: Int, height: Int, percent: Int): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(4f, 4f, w - 4f, h - 4f), 16f, 16f, bgPaint)

        val margin = 16f
        val strokeW = 10f
        val arcRect = RectF(margin, margin, w - margin, h - margin)

        // Track paint
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        // Gauge arc paint
        val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD700") // Amber Gold
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }

        val sweepAngle = 270f * (percent.coerceIn(0, 100) / 100f)
        canvas.drawArc(arcRect, 135f, sweepAngle, false, gaugePaint)

        // Percentage text in center
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = h * 0.26f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("${percent}%", w / 2f, (h / 2f) + (textPaint.textSize * 0.35f), textPaint)

        return bitmap
    }

    /**
     * Draws a dual Rx/Tx network pulse waveform graph.
     */
    fun drawNetworkWaveGraph(width: Int, height: Int, rxHistory: List<Int>, txHistory: List<Int>): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(4f, 4f, w - 4f, h - 4f), 16f, 16f, bgPaint)

        val paddingY = 14f
        val usableH = (h - (paddingY * 2)) / 2f

        fun drawSeries(dataRaw: List<Int>, colorHex: String, isUpper: Boolean) {
            val data = subsampleHistory(dataRaw, 30)
            if (data.isEmpty()) return
            val listData = if (data.size < 2) listOf(data.first(), data.first()) else data
            val stepX = (w - 24f) / (listData.size - 1).coerceAtLeast(1)
            val startX = 12f
            val baselineY = if (isUpper) h / 2f - 2f else h / 2f + 2f

            val path = Path()
            val startY = if (isUpper) baselineY - (listData[0].coerceIn(0, 100) / 100f * usableH)
                         else baselineY + (listData[0].coerceIn(0, 100) / 100f * usableH)

            path.moveTo(startX, startY)
            for (i in 1 until listData.size) {
                val x = startX + (i * stepX)
                val y = if (isUpper) baselineY - (listData[i].coerceIn(0, 100) / 100f * usableH)
                        else baselineY + (listData[i].coerceIn(0, 100) / 100f * usableH)
                path.lineTo(x, y)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(colorHex)
                strokeWidth = 4.5f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, paint)
        }

        // Rx = Downstream (Cyan upper wave), Tx = Upstream (Purple lower wave)
        drawSeries(rxHistory, "#00E5FF", true)
        drawSeries(txHistory, "#D500F9", false)

        return bitmap
    }
}
