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

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSize28.setOnClickListener { setIconSize(28) }
        binding.btnSize40.setOnClickListener { setIconSize(40) }
        binding.btnSize56.setOnClickListener { setIconSize(56) }
        binding.btnSize72.setOnClickListener { setIconSize(72) }

        binding.btnResetDefaults.setOnClickListener {
            DockManager.resetDocksToDefaults(this)
            updateIconSizeLabel()
            Toast.makeText(this, "Dock layouts reset to default!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setIconSize(sizeDp: Int) {
        DockManager.updateIconSize(sizeDp, this)
        updateIconSizeLabel()
        Toast.makeText(this, "Icon size set to ${sizeDp}dp!", Toast.LENGTH_SHORT).show()
    }

    private fun updateIconSizeLabel() {
        val currentSize = DockManager.tileIconSizeDp
        binding.tvCurrentIconSize.text = "Current Icon Size: ${currentSize}dp${if (currentSize == 56) " (Default 2x Large)" else ""}"
    }
}
