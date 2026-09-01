package com.steplauncher.core.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

object SparklineGraphRenderer {

    /**
     * Subsamples telemetry history using bucket averaging (moving mean) to eliminate erratic point-sampling
     * jitter and ensure smooth, consistent trend lines across hundreds of data points over time.
     */
    private fun subsampleHistory(history: List<Int>, maxPoints: Int = 30): List<Int> {
        val list = synchronized(history) { history.toList() }
        if (list.isEmpty()) return emptyList()
        if (list.size <= maxPoints) return list

        val result = mutableListOf<Int>()
        val chunkSize = list.size.toFloat() / maxPoints
        for (i in 0 until maxPoints) {
            val startIdx = (i * chunkSize).toInt().coerceIn(0, list.size - 1)
            val endIdx = ((i + 1) * chunkSize).toInt().coerceIn(startIdx + 1, list.size)
            val subList = list.subList(startIdx, endIdx)
            val avg = if (subList.isNotEmpty()) subList.average().toInt() else list[startIdx]
            result.add(avg)
        }
        return result
    }

    /**
     * Draws a high-resolution detailed telemetry graph with labelled X & Y axes for the dialog view.
     */
    fun drawDetailedTelemetryGraphWithAxes(
        width: Int,
        height: Int,
        history: List<Int>,
        title: String,
        yUnit: String = "%",
        graphicColorHex: String = "#00E5FF",
        colorHighHex: String = "#FF5252",
        colorMedHex: String = "#FFD700",
        colorLowHex: String = "#00E676"
    ): Bitmap {
        val w = width.coerceAtLeast(300)
        val h = height.coerceAtLeast(180)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Translucent background card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#20000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 20f, 20f, bgPaint)

        val marginLeft = 65f
        val marginRight = 25f
        val marginTop = 40f
        val marginBottom = 45f

        val graphW = w - marginLeft - marginRight
        val graphH = h - marginTop - marginBottom

        // Draw Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText(title, marginLeft, 28f, titlePaint)

        // Dotted grid lines & Y-Axis Labels
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }

        val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0FFFFFF")
            textSize = 15f
            textAlign = Paint.Align.RIGHT
        }

        val yLevels = arrayOf(100, 75, 50, 25, 0)
        yLevels.forEach { lvl ->
            val ratio = lvl / 100f
            val y = marginTop + graphH * (1f - ratio)
            
            // Grid line
            val gridPath = Path().apply {
                moveTo(marginLeft, y)
                lineTo(w - marginRight, y)
            }
            canvas.drawPath(gridPath, gridPaint)

            // Y-Axis label
            canvas.drawText("$lvl$yUnit", marginLeft - 10f, y + 5f, axisLabelPaint)
        }

        // X-Axis Time Labels (-10m, -7.5m, -5m, -2.5m, NOW)
        val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0FFFFFF")
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }
        val xLabels = arrayOf("-10m", "-7.5m", "-5m", "-2.5m", "NOW")
        xLabels.forEachIndexed { idx, label ->
            val ratio = idx / (xLabels.size - 1).toFloat()
            val x = marginLeft + graphW * ratio
            canvas.drawText(label, x, h - 12f, xLabelPaint)
        }

        val dataRaw = subsampleHistory(history, 60)
        if (dataRaw.isEmpty()) return bitmap

        val data = if (dataRaw.size < 2) listOf(dataRaw.first(), dataRaw.first()) else dataRaw
        val stepX = graphW / (data.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        val startX = marginLeft
        val startY = marginTop + graphH * (1f - (data[0].coerceIn(0, 100) / 100f))

        linePath.moveTo(startX, startY)
        fillPath.moveTo(startX, marginTop + graphH)
        fillPath.lineTo(startX, startY)

        for (i in 1 until data.size) {
            val x = startX + (i * stepX)
            val y = marginTop + graphH * (1f - (data[i].coerceIn(0, 100) / 100f))
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        val lastX = startX + ((data.size - 1) * stepX)
        fillPath.lineTo(lastX, marginTop + graphH)
        fillPath.close()

        // Fill area under graph
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, marginTop, 0f, marginTop + graphH, Color.parseColor("#5000E5FF"), Color.parseColor("#0200E5FF"), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawPath(fillPath, fillPaint)

        // Line graph paint
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(graphicColorHex)
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(linePath, linePaint)

        // Highlight latest point
        val latestVal = data.last()
        val latestColor = when {
            latestVal >= 75 -> Color.parseColor(colorHighHex)
            latestVal >= 40 -> Color.parseColor(colorMedHex)
            else -> Color.parseColor(colorLowHex)
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = latestColor
            style = Paint.Style.FILL
        }
        val lastY = marginTop + graphH * (1f - (latestVal.coerceIn(0, 100) / 100f))
        canvas.drawCircle(lastX, lastY, 7f, dotPaint)

        return bitmap
    }

    /**
     * Draws a high-tech glowing CPU line sparkline graph.
     */
    fun drawCpuLineGraph(
        width: Int,
        height: Int,
        history: List<Int>,
        graphicColorHex: String = "#00E5FF",
        colorHighHex: String = "#FF5252",
        colorMedHex: String = "#FFD700",
        colorLowHex: String = "#00E676"
    ): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

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

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, paddingY, 0f, h - paddingY, Color.parseColor("#6000E5FF"), Color.parseColor("#0500E5FF"), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawPath(fillPath, fillPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(graphicColorHex)
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, linePaint)

        val latestVal = data.last()
        val latestColor = when {
            latestVal >= 75 -> Color.parseColor(colorHighHex)
            latestVal >= 40 -> Color.parseColor(colorMedHex)
            else -> Color.parseColor(colorLowHex)
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = latestColor
            style = Paint.Style.FILL
        }
        val lastY = h - paddingY - (latestVal.coerceIn(0, 100) / 100f * usableH)
        canvas.drawCircle(lastX, lastY, 6f, dotPaint)

        return bitmap
    }

    /**
     * Draws a segmented vertical RAM bar graph.
     */
    fun drawMemoryBarGraph(
        width: Int,
        height: Int,
        percent: Int,
        colorHighHex: String = "#FF5252",
        colorMedHex: String = "#FFD700",
        colorLowHex: String = "#00E676"
    ): Bitmap {
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

        val activeColor = when {
            percent >= 80 -> colorHighHex
            percent >= 50 -> colorMedHex
            else -> colorLowHex
        }

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h - marginY, 0f, marginY, Color.parseColor(activeColor), Color.parseColor("#B2FF59"), Shader.TileMode.CLAMP)
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
    fun drawStorageGaugeGraph(
        width: Int,
        height: Int,
        percent: Int,
        colorHighHex: String = "#FF5252",
        colorMedHex: String = "#FFD700",
        colorLowHex: String = "#00E676"
    ): Bitmap {
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

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        val gaugeColor = when {
            percent >= 85 -> colorHighHex
            percent >= 60 -> colorMedHex
            else -> colorLowHex
        }

        val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(gaugeColor)
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }

        val sweepAngle = 270f * (percent.coerceIn(0, 100) / 100f)
        canvas.drawArc(arcRect, 135f, sweepAngle, false, gaugePaint)

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
    fun drawNetworkWaveGraph(
        width: Int,
        height: Int,
        rxHistory: List<Int>,
        txHistory: List<Int>,
        graphicColorHex: String = "#00E5FF"
    ): Bitmap {
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

        drawSeries(rxHistory, graphicColorHex, true)
        drawSeries(txHistory, "#D500F9", false)

        return bitmap
    }
}
