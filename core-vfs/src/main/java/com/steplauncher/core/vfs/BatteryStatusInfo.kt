package com.steplauncher.core.vfs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryStatusInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val chargePlugStr: String,
    val healthStr: String,
    val voltageMv: Int,
    val tempCelsius: Float,
    val technology: String,
    val connectedDeviceBatteries: List<Pair<String, Int>> = emptyList()
)

object BatteryUtils {

    fun getBatteryStatus(context: Context): BatteryStatusInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, filter)

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 100

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargePlugStr = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Charging" else "Discharging"
        }

        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10.0f
        val tech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val connectedDevices = getConnectedBluetoothBatteries(context)

        return BatteryStatusInfo(
            levelPercent = percent,
            isCharging = isCharging,
            chargePlugStr = chargePlugStr,
            healthStr = healthStr,
            voltageMv = voltageMv,
            tempCelsius = tempCelsius,
            technology = tech,
            connectedDeviceBatteries = connectedDevices
        )
    }

    private fun getConnectedBluetoothBatteries(context: Context): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                val bondedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
                bondedDevices?.forEach { device ->
                    try {
                        val method = device.javaClass.getMethod("getBatteryLevel")
                        val batteryLevel = method.invoke(device) as Int
                        if (batteryLevel >= 0) {
                            val name = device.name ?: "Bluetooth Device"
                            result.add(Pair(name, batteryLevel))
                        }
                    } catch (e: Exception) {
                        // Reflection fail fallback
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
