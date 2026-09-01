package com.steplauncher.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Interactive RGB / HSV Color Picker Dialog with live preview, SeekBars, hex text input, and quick swatches.
 */
object ColorPickerDialogHelper {

    fun show(
        context: Context,
        title: String,
        initialColorHex: String,
        onColorSelected: (String) -> Unit
    ) {
        var currentColorInt: Int = try {
            Color.parseColor(initialColorHex)
        } catch (e: Exception) {
            Color.WHITE
        }

        val hsv = FloatArray(3)
        Color.colorToHSV(currentColorInt, hsv)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 25, 40, 25)
        }

        // Live Color Preview Box
        val previewBox = View(context).apply {
            val gd = GradientDrawable().apply {
                setColor(currentColorInt)
                setStroke(2, Color.WHITE)
                cornerRadius = 16f
            }
            background = gd
        }
        val previewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (48 * context.resources.displayMetrics.density).toInt()
        ).apply {
            setMargins(0, 0, 0, 16)
        }
        layout.addView(previewBox, previewParams)

        // Hex Code Input
        val etHex = EditText(context).apply {
            hint = "#RRGGBB"
            setText(String.format("#%06X", 0xFFFFFF and currentColorInt))
        }
        layout.addView(etHex)

        // Helper to refresh UI preview & sliders without triggering loops
        var isSelfUpdating = false

        fun updatePreview(newColorInt: Int, updateHexField: Boolean = true) {
            currentColorInt = newColorInt
            Color.colorToHSV(currentColorInt, hsv)
            val gd = GradientDrawable().apply {
                setColor(currentColorInt)
                setStroke(2, Color.WHITE)
                cornerRadius = 16f
            }
            previewBox.background = gd

            if (updateHexField && !isSelfUpdating) {
                isSelfUpdating = true
                etHex.setText(String.format("#%06X", 0xFFFFFF and currentColorInt))
                isSelfUpdating = false
            }
        }

        // HSV Controls
        val tvHsvLabel = TextView(context).apply {
            text = "HSV (Hue / Saturation / Value) Sliders:"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 5)
        }
        layout.addView(tvHsvLabel)

        val sbHue = SeekBar(context).apply { max = 360; progress = hsv[0].toInt() }
        val sbSat = SeekBar(context).apply { max = 100; progress = (hsv[1] * 100).toInt() }
        val sbVal = SeekBar(context).apply { max = 100; progress = (hsv[2] * 100).toInt() }

        val tvHue = TextView(context).apply { text = "Hue: ${sbHue.progress}°"; textSize = 11f }
        val tvSat = TextView(context).apply { text = "Saturation: ${sbSat.progress}%"; textSize = 11f }
        val tvVal = TextView(context).apply { text = "Brightness: ${sbVal.progress}%"; textSize = 11f }

        fun syncFromHsv() {
            if (isSelfUpdating) return
            isSelfUpdating = true
            hsv[0] = sbHue.progress.toFloat()
            hsv[1] = sbSat.progress / 100f
            hsv[2] = sbVal.progress / 100f
            val c = Color.HSVToColor(hsv)
            updatePreview(c, updateHexField = true)
            tvHue.text = "Hue: ${sbHue.progress}°"
            tvSat.text = "Saturation: ${sbSat.progress}%"
            tvVal.text = "Brightness: ${sbVal.progress}%"
            isSelfUpdating = false
        }

        val hsvListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) syncFromHsv()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }

        sbHue.setOnSeekBarChangeListener(hsvListener)
        sbSat.setOnSeekBarChangeListener(hsvListener)
        sbVal.setOnSeekBarChangeListener(hsvListener)

        layout.addView(tvHue)
        layout.addView(sbHue)
        layout.addView(tvSat)
        layout.addView(sbSat)
        layout.addView(tvVal)
        layout.addView(sbVal)

        // Swatches Palette
        val tvSwatchesLabel = TextView(context).apply {
            text = "Preset Swatches:"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 5)
        }
        layout.addView(tvSwatchesLabel)

        val swatches = listOf(
            "#FFFFFF", "#00E5FF", "#AB47BC", "#FFAB00",
            "#FF5252", "#FFD700", "#00E676", "#FF1744",
            "#FF9100", "#38BDF8", "#F43F5E", "#10B981"
        )

        val gridLayout = GridLayout(context).apply {
            columnCount = 6
            setPadding(0, 5, 0, 5)
        }

        swatches.forEach { hex ->
            val colorInt = try { Color.parseColor(hex) } catch (e: Exception) { Color.WHITE }
            val btnSwatch = Button(context).apply {
                val gd = GradientDrawable().apply {
                    setColor(colorInt)
                    cornerRadius = 8f
                    setStroke(1, Color.GRAY)
                }
                background = gd
                setOnClickListener {
                    updatePreview(colorInt, updateHexField = true)
                    Color.colorToHSV(colorInt, hsv)
                    sbHue.progress = hsv[0].toInt()
                    sbSat.progress = (hsv[1] * 100).toInt()
                    sbVal.progress = (hsv[2] * 100).toInt()
                    tvHue.text = "Hue: ${sbHue.progress}°"
                    tvSat.text = "Saturation: ${sbSat.progress}%"
                    tvVal.text = "Brightness: ${sbVal.progress}%"
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = (40 * context.resources.displayMetrics.density).toInt()
                height = (40 * context.resources.displayMetrics.density).toInt()
                setMargins(4, 4, 4, 4)
            }
            gridLayout.addView(btnSwatch, params)
        }
        layout.addView(gridLayout)

        etHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isSelfUpdating) return
                val str = s?.toString()?.trim() ?: ""
                if (str.length == 7 || str.length == 9) {
                    try {
                        val parsed = Color.parseColor(str)
                        isSelfUpdating = true
                        updatePreview(parsed, updateHexField = false)
                        Color.colorToHSV(parsed, hsv)
                        sbHue.progress = hsv[0].toInt()
                        sbSat.progress = (hsv[1] * 100).toInt()
                        sbVal.progress = (hsv[2] * 100).toInt()
                        tvHue.text = "Hue: ${sbHue.progress}°"
                        tvSat.text = "Saturation: ${sbSat.progress}%"
                        tvVal.text = "Brightness: ${sbVal.progress}%"
                        isSelfUpdating = false
                    } catch (e: Exception) {}
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val scrollView = ScrollView(context).apply {
            addView(layout)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Apply Color") { _, _ ->
                val finalHex = String.format("#%06X", 0xFFFFFF and currentColorInt)
                onColorSelected(finalHex)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
