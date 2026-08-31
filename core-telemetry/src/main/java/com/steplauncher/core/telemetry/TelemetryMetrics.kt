package com.steplauncher.core.telemetry

data class TelemetryMetrics(
    val cpuUsagePercent: Int,
    val memoryUsagePercent: Int,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val rxSpeedKbps: Long,
    val txSpeedKbps: Long,
    val timestamp: Long = System.currentTimeMillis()
)
