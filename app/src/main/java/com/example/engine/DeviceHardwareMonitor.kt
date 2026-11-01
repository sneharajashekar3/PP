package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import kotlin.math.roundToInt

data class HardwareTelemetrySnapshot(
    val totalDeviceRamMb: Long,
    val availableDeviceRamMb: Long,
    val usedDeviceRamMb: Long,
    val deviceRamUsagePercent: Int,
    val isLowMemorySystemFlag: Boolean,
    val lowMemoryThresholdMb: Long,
    val jvmAllocatedMb: Long,
    val jvmMaxMb: Long,
    val nativeHeapAllocatedMb: Long,
    val thermalStatusLabel: String,
    val isThermalThrottlingWarning: Boolean,
    val batteryLevelPercent: Int,
    val isCharging: Boolean,
    val batteryCurrentReport: String,
    val isBatteryCurrentHardwareSupported: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun canSafelyAllocateModel(estimatedModelRamMb: Long, safetyCeilingPct: Int = 80): Boolean {
        if (isLowMemorySystemFlag) return false
        val projectedUsedMb = usedDeviceRamMb + estimatedModelRamMb
        val projectedPct = ((projectedUsedMb.toDouble() / totalDeviceRamMb.coerceAtLeast(1L)) * 100.0).roundToInt()
        val remainingAboveThreshold = availableDeviceRamMb - estimatedModelRamMb - lowMemoryThresholdMb
        return projectedPct <= safetyCeilingPct && remainingAboveThreshold > 128L
    }
}

/**
 * Strictly local, on-device hardware monitor for 8 GB RAM target devices.
 *
 * - Never transmits device statistics over any network.
 * - Treats battery-current measurements (`BATTERY_PROPERTY_CURRENT_NOW`) and thermal status
 *   as best-effort diagnostics because OEM HAL / kernel fuel-gauge support varies across Android devices.
 */
class DeviceHardwareMonitor(private val context: Context) {

    fun captureSnapshot(): HardwareTelemetrySnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val mb = 1024L * 1024L
        val totalRamMb = if (memInfo.totalMem > 0) memInfo.totalMem / mb else 8192L
        val availRamMb = if (memInfo.availMem > 0) memInfo.availMem / mb else 4096L
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0L)
        val usagePct = ((usedRamMb.toDouble() / totalRamMb.coerceAtLeast(1L)) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
        val thresholdMb = (memInfo.threshold / mb).coerceAtLeast(256L)

        val runtime = Runtime.getRuntime()
        val jvmAllocatedMb = ((runtime.totalMemory() - runtime.freeMemory()) / mb).coerceAtLeast(1L)
        val jvmMaxMb = (runtime.maxMemory() / mb).coerceAtLeast(64L)
        val nativeHeapMb = (Debug.getNativeHeapAllocatedSize() / mb).coerceAtLeast(1L)

        // Best-effort thermal state via PowerManager (API 29+)
        val (thermalLabel, thermalWarning) = readThermalStatusBestEffort()

        // Best-effort battery capacity & instantaneous current via BatteryManager
        val (batteryPct, isCharging, currentLabel, currentSupported) = readBatteryMetricsBestEffort()

        return HardwareTelemetrySnapshot(
            totalDeviceRamMb = totalRamMb,
            availableDeviceRamMb = availRamMb,
            usedDeviceRamMb = usedRamMb,
            deviceRamUsagePercent = usagePct,
            isLowMemorySystemFlag = memInfo.lowMemory,
            lowMemoryThresholdMb = thresholdMb,
            jvmAllocatedMb = jvmAllocatedMb,
            jvmMaxMb = jvmMaxMb,
            nativeHeapAllocatedMb = nativeHeapMb,
            thermalStatusLabel = thermalLabel,
            isThermalThrottlingWarning = thermalWarning,
            batteryLevelPercent = batteryPct,
            isCharging = isCharging,
            batteryCurrentReport = currentLabel,
            isBatteryCurrentHardwareSupported = currentSupported
        )
    }

    private fun readThermalStatusBestEffort(): Pair<String, Boolean> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "Not reported (Requires API 29+)" to false
        }
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return "HAL unavailable (Best-Effort)" to false
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal (Cool)" to false
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth" to false
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Warmth" to false
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe (Throttling Risk)" to true
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Thermal Limit" to true
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Thermal Limit" to true
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown Imminent" to true
                else -> "Unknown HAL State" to false
            }
        } catch (_: Exception) {
            "Not reported by OEM HAL" to false
        }
    }

    private data class BatteryResult(
        val levelPct: Int,
        val isCharging: Boolean,
        val currentReport: String,
        val isCurrentSupported: Boolean
    )

    private fun readBatteryMetricsBestEffort(): BatteryResult {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                ?: return BatteryResult(100, false, "Battery HAL unavailable", false)

            val capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                .let { if (it in 0..100) it else 100 }
            val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bm.isCharging
            } else {
                false
            }

            val rawCurrentMicroAmps = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (rawCurrentMicroAmps == Int.MIN_VALUE || rawCurrentMicroAmps == 0) {
                BatteryResult(
                    levelPct = capacity,
                    isCharging = isCharging,
                    currentReport = "Best-effort: Sensor not reported by hardware HAL",
                    isCurrentSupported = false
                )
            } else {
                // Some OEMs report in mA instead of uA if abs(val) < 10_000
                val milliAmps = if (kotlin.math.abs(rawCurrentMicroAmps) > 10_000) {
                    rawCurrentMicroAmps / 1000
                } else {
                    rawCurrentMicroAmps
                }
                BatteryResult(
                    levelPct = capacity,
                    isCharging = isCharging,
                    currentReport = "${milliAmps} mA (Best-Effort Local Gauge)",
                    isCurrentSupported = true
                )
            }
        } catch (_: Exception) {
            BatteryResult(100, false, "Best-effort: Not reported by hardware", false)
        }
    }
}
