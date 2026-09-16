package com.essentialremapper.domain.action

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.essentialremapper.data.model.HapticIntensity
import com.essentialremapper.util.AppLogger

/**
 * Controller for tactile haptic feedback triggered upon successful action execution.
 */
open class HapticsController(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "HapticsController"
    }

    private val vibrator: Vibrator? by lazy {
        try {
            val ctx = context ?: return@lazy null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to obtain Vibrator service: ${e.message}", TAG)
            null
        }
    }

    open fun vibrate(intensity: HapticIntensity) {
        if (intensity == HapticIntensity.DISABLED) return

        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return

            val effect = when (intensity) {
                HapticIntensity.LOW -> {
                    VibrationEffect.createOneShot(25L, 64)
                }
                HapticIntensity.MEDIUM -> {
                    VibrationEffect.createOneShot(45L, 140)
                }
                HapticIntensity.HIGH -> {
                    VibrationEffect.createOneShot(75L, 255)
                }
                HapticIntensity.DISABLED -> return
            }

            v.vibrate(effect)
        } catch (e: Exception) {
            AppLogger.w("Haptic vibration failed: ${e.message}", TAG)
        }
    }
}
