package com.example.pocketscholar.data

import android.content.Context
import android.util.Log

private const val TAG = "QuantizationPreference"
private const val PREFS_NAME = "quantization_prefs"
private const val KEY_CHOICE = "quantization_choice"

/**
 * User's quantization preference.
 *
 * - AUTO:     Device RAM determines Q4 vs Q8 (via DeviceMemoryManager)
 * - FORCE_Q4: Always use 4-bit models (low memory mode)
 * - FORCE_Q8: Always use 8-bit models (high quality mode)
 */
enum class QuantizationChoice(val label: String, val description: String) {
    AUTO("Otomatik", "RAM'e göre otomatik seçim"),
    FORCE_Q4("Düşük Bellek", "Her zaman 4-bit (Q4) kullan"),
    FORCE_Q8("Yüksek Kalite", "Her zaman 8-bit (Q8) kullan")
}

/**
 * Persists the user's quantization choice to SharedPreferences
 * and resolves the effective quantization mode.
 */
class QuantizationPreference(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Read the saved choice (defaults to AUTO). */
    fun getChoice(): QuantizationChoice {
        val saved = prefs.getString(KEY_CHOICE, QuantizationChoice.AUTO.name)
        return try {
            QuantizationChoice.valueOf(saved ?: QuantizationChoice.AUTO.name)
        } catch (_: IllegalArgumentException) {
            QuantizationChoice.AUTO
        }
    }

    /** Save the user's choice. */
    fun setChoice(choice: QuantizationChoice) {
        prefs.edit().putString(KEY_CHOICE, choice.name).apply()
        Log.d(TAG, "Quantization choice set to: ${choice.name}")
    }

    /**
     * Resolve the actual QuantizationMode to use right now.
     *
     * If AUTO → delegates to DeviceMemoryManager.
     * If FORCE_Q4 / FORCE_Q8 → returns the forced mode directly.
     */
    fun getEffectiveQuantization(memoryManager: DeviceMemoryManager): QuantizationMode {
        return when (getChoice()) {
            QuantizationChoice.AUTO -> memoryManager.getRecommendedQuantization()
            QuantizationChoice.FORCE_Q4 -> QuantizationMode.Q4_K_M
            QuantizationChoice.FORCE_Q8 -> QuantizationMode.Q8_0
        }
    }
}
