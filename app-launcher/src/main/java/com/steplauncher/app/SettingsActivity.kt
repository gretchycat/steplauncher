package com.steplauncher.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.steplauncher.app.databinding.ActivitySettingsBinding
import com.steplauncher.core.renderer.DockManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateIconSizeLabel()
        updateColorLabels()

        binding.btnCloseSettings.setOnClickListener {
            finish()
        }

        binding.btnSize28.setOnClickListener { setIconSize(28) }
        binding.btnSize40.setOnClickListener { setIconSize(38) }
        binding.btnSize56.setOnClickListener { setIconSize(48) }
        binding.btnSize72.setOnClickListener { setIconSize(64) }

        // 1. Tile Background Accent Color
        binding.btnPickAccentColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🎨 Choose Tile Background Accent Color",
                initialColorHex = DockManager.accentColorHex
            ) { hex ->
                DockManager.updateThemeColors(accentHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Tile Background Accent set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Text Accent Color
        binding.btnPickTextColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "✍️ Choose Text Accent Color",
                initialColorHex = DockManager.textColorHex
            ) { hex ->
                DockManager.updateThemeColors(textHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Text Accent set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Image Accent Color
        binding.btnPickGraphicColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🖼️ Choose Image Accent Color",
                initialColorHex = DockManager.graphicColorHex
            ) { hex ->
                DockManager.updateThemeColors(graphicHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Image Accent set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Attention Alert Color
        binding.btnPickAttentionColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🚨 Choose Attention Alert Color",
                initialColorHex = DockManager.attentionColorHex
            ) { hex ->
                DockManager.updateThemeColors(attentionHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Attention Alert set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Badge Background Color
        binding.btnPickBadgeBgColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🔴 Choose Notification Badge Background Color",
                initialColorHex = DockManager.badgeBgColorHex
            ) { hex ->
                DockManager.updateThemeColors(badgeBgHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Badge Background set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. Badge Text Color
        binding.btnPickBadgeTextColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🔤 Choose Notification Badge Text Color",
                initialColorHex = DockManager.badgeTextColorHex
            ) { hex ->
                DockManager.updateThemeColors(badgeTextHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Badge Text Color set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. High Threshold Alert Color
        binding.btnPickHighColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🔴 Choose High Threshold Alert Color",
                initialColorHex = DockManager.colorHighHex
            ) { hex ->
                DockManager.updateThemeColors(highHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "High Threshold Color set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. Medium Threshold Color
        binding.btnPickMedColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🟡 Choose Medium Threshold Color",
                initialColorHex = DockManager.colorMedHex
            ) { hex ->
                DockManager.updateThemeColors(medHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Medium Threshold Color set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // 7. Low Threshold Normal Color
        binding.btnPickLowColor.setOnClickListener {
            ColorPickerDialogHelper.show(
                context = this,
                title = "🟢 Choose Low Threshold Normal Color",
                initialColorHex = DockManager.colorLowHex
            ) { hex ->
                DockManager.updateThemeColors(lowHex = hex, context = this)
                updateColorLabels()
                Toast.makeText(this, "Low Threshold Color set to $hex", Toast.LENGTH_SHORT).show()
            }
        }

        // Quick Preset Themes
        binding.btnThemeCyber.setOnClickListener {
            setThemePalette(
                textHex = "#FFFFFF",
                graphicHex = "#00E5FF",
                highHex = "#FF5252",
                medHex = "#FFD700",
                lowHex = "#00E676",
                label = "⚡ Cyberpunk Neon"
            )
        }

        binding.btnThemeMatrix.setOnClickListener {
            setThemePalette(
                textHex = "#A3FFB4",
                graphicHex = "#00E676",
                highHex = "#FF5252",
                medHex = "#FFEA00",
                lowHex = "#00E676",
                label = "🟢 Terminal Matrix"
            )
        }

        binding.btnThemeSunset.setOnClickListener {
            setThemePalette(
                textHex = "#FFE082",
                graphicHex = "#FF9100",
                highHex = "#FF1744",
                medHex = "#FFC400",
                lowHex = "#00E676",
                label = "🌅 Solar Sunset"
            )
        }

        binding.btnResetDefaults.setOnClickListener {
            DockManager.resetDocksToDefaults(this)
            updateIconSizeLabel()
            updateColorLabels()
            Toast.makeText(this, "Dock layouts & theme reset to default!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setIconSize(sizeDp: Int) {
        DockManager.updateIconSize(sizeDp, this)
        updateIconSizeLabel()
        Toast.makeText(this, "Icon size set to ${sizeDp}dp!", Toast.LENGTH_SHORT).show()
    }

    private fun setThemePalette(
        textHex: String,
        graphicHex: String,
        highHex: String,
        medHex: String,
        lowHex: String,
        label: String
    ) {
        DockManager.updateThemeColors(
            textHex = textHex,
            graphicHex = graphicHex,
            highHex = highHex,
            medHex = medHex,
            lowHex = lowHex,
            context = this
        )
        updateColorLabels()
        Toast.makeText(this, "Theme palette updated to $label!", Toast.LENGTH_SHORT).show()
    }

    private fun updateIconSizeLabel() {
        val currentSize = DockManager.tileIconSizeDp
        binding.tvCurrentIconSize.text = "Current Icon Size: ${currentSize}dp"
    }

    private fun updateColorLabels() {
        binding.btnPickAccentColor.text = "🎨 Tile Background Accent: ${DockManager.accentColorHex}"

        binding.btnPickTextColor.text = "✍️ Text Accent Color: ${DockManager.textColorHex}"
        try { binding.btnPickTextColor.setTextColor(Color.parseColor(DockManager.textColorHex)) } catch (e: Exception) {}

        binding.btnPickGraphicColor.text = "🖼️ Image Accent Color: ${DockManager.graphicColorHex}"
        try { binding.btnPickGraphicColor.setTextColor(Color.parseColor(DockManager.graphicColorHex)) } catch (e: Exception) {}

        binding.btnPickAttentionColor.text = "🚨 Attention Alert Tint: ${DockManager.attentionColorHex}"
        try { binding.btnPickAttentionColor.setTextColor(Color.parseColor(DockManager.attentionColorHex)) } catch (e: Exception) {}

        binding.btnPickBadgeBgColor.text = "🔴 Notification Badge Background: ${DockManager.badgeBgColorHex}"
        try { binding.btnPickBadgeBgColor.setTextColor(Color.parseColor(DockManager.badgeBgColorHex)) } catch (e: Exception) {}

        binding.btnPickBadgeTextColor.text = "🔤 Notification Badge Text: ${DockManager.badgeTextColorHex}"
        try { binding.btnPickBadgeTextColor.setTextColor(Color.parseColor(DockManager.badgeTextColorHex)) } catch (e: Exception) {}

        binding.btnPickHighColor.text = "🔴 High Threshold Alert Color: ${DockManager.colorHighHex}"
        try { binding.btnPickHighColor.setTextColor(Color.parseColor(DockManager.colorHighHex)) } catch (e: Exception) {}

        binding.btnPickMedColor.text = "🟡 Medium Threshold Color: ${DockManager.colorMedHex}"
        try { binding.btnPickMedColor.setTextColor(Color.parseColor(DockManager.colorMedHex)) } catch (e: Exception) {}

        binding.btnPickLowColor.text = "🟢 Low Threshold Normal Color: ${DockManager.colorLowHex}"
        try { binding.btnPickLowColor.setTextColor(Color.parseColor(DockManager.colorLowHex)) } catch (e: Exception) {}
    }
}
