package com.example.pocketscholar.data

import android.app.ActivityManager
import android.content.Context
import android.util.Log

private const val TAG = "DeviceMemoryManager"

/**
 * Reads device RAM info and determines the recommended quantization level.
 *
 * Rule:
 *  - RAM < 6 GB → Q4_K_M (4-bit, low memory)
 *  - RAM ≥ 6 GB → Q8_0  (8-bit, higher quality)
 *
 * Users can override this recommendation via QuantizationPreference.
 */
class DeviceMemoryManager(context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /** Total device RAM in MB. */
    fun getTotalRamMB(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    /** Available (free) RAM in MB. */
    fun getAvailableRamMB(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    /** Whether the device is in a low-memory state. */
    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    /**
     * Recommended quantization based on total RAM.
     * Threshold: 6 GB — below → Q4, above → Q8.
     */
    fun getRecommendedQuantization(): QuantizationMode {
        val totalMB = getTotalRamMB()
        val recommended = if (totalMB < 6 * 1024) {
            QuantizationMode.Q4_K_M
        } else {
            QuantizationMode.Q8_0
        }
        Log.d(TAG, "Total RAM: ${totalMB} MB → Recommended: ${recommended.label}")
        return recommended
    }

    /**
     * Memory pressure level — used for model unload decisions.
     */
    fun getMemoryPressureLevel(): MemoryPressureLevel {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        if (memInfo.lowMemory) return MemoryPressureLevel.CRITICAL

        val usedPercent = ((memInfo.totalMem - memInfo.availMem) * 100) / memInfo.totalMem
        return when {
            usedPercent > 85 -> MemoryPressureLevel.HIGH
            usedPercent > 70 -> MemoryPressureLevel.MEDIUM
            else -> MemoryPressureLevel.LOW
        }
    }

    /** Human-readable RAM summary for UI display (e.g. "2.1 / 6.0 GB"). */
    fun getRamSummary(): String {
        val totalGB = String.format("%.1f", getTotalRamMB() / 1024.0)
        val availGB = String.format("%.1f", getAvailableRamMB() / 1024.0)
        return "$availGB / $totalGB GB"
    }
}

/**
 * Memory pressure levels for runtime monitoring.
 */
enum class MemoryPressureLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}
