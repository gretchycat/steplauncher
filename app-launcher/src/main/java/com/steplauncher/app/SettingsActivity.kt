package com.steplauncher.app

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
        updateAccentColorLabel()

        binding.btnCloseSettings.setOnClickListener {
            finish()
        }

        binding.btnSize28.setOnClickListener { setIconSize(28) }
        binding.btnSize40.setOnClickListener { setIconSize(40) }
        binding.btnSize56.setOnClickListener { setIconSize(56) }
        binding.btnSize72.setOnClickListener { setIconSize(72) }

        binding.btnColorIce.setOnClickListener { setAccentColor("#FFFFFF", "❄️ Frosted Ice") }
        binding.btnColorCyan.setOnClickListener { setAccentColor("#00E5FF", "💎 Cyan Glass") }
        binding.btnColorViolet.setOnClickListener { setAccentColor("#AB47BC", "💜 Amethyst Violet") }
        binding.btnColorAmber.setOnClickListener { setAccentColor("#FFAB00", "🧡 Amber Gold") }

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
            updateAccentColorLabel()
            Toast.makeText(this, "Dock layouts reset to default!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setIconSize(sizeDp: Int) {
        DockManager.updateIconSize(sizeDp, this)
        updateIconSizeLabel()
        Toast.makeText(this, "Icon size set to ${sizeDp}dp!", Toast.LENGTH_SHORT).show()
    }

    private fun setAccentColor(hex: String, label: String) {
        DockManager.updateThemeColors(accentHex = hex, context = this)
        updateAccentColorLabel()
        Toast.makeText(this, "Tile background tint set to $label!", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Theme palette updated to $label!", Toast.LENGTH_SHORT).show()
    }

    private fun updateIconSizeLabel() {
        val currentSize = DockManager.tileIconSizeDp
        binding.tvCurrentIconSize.text = "Current Icon Size: ${currentSize}dp${if (currentSize == 56) " (Default 2x Large)" else ""}"
    }

    private fun updateAccentColorLabel() {
        val hex = DockManager.accentColorHex
        val label = when (hex) {
            "#00E5FF" -> "💎 Cyan Glass (#00E5FF)"
            "#AB47BC" -> "💜 Amethyst Violet (#AB47BC)"
            "#FFAB00" -> "🧡 Amber Gold (#FFAB00)"
            else -> "❄️ Frosted Ice (#FFFFFF)"
        }
        binding.tvCurrentAccentColor.text = "Current Tint: $label"
    }
}
