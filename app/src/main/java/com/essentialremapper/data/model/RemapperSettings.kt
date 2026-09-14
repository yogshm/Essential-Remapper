package com.essentialremapper.data.model

import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.domain.device.ButtonPosition
import com.essentialremapper.domain.device.DeviceProfile

enum class HapticIntensity(val displayName: String) {
    DISABLED("Disabled"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class OverlayStyle(val displayName: String) {
    NOTHING_DOT_MATRIX("Nothing Dot-Matrix"),
    ANDROID_STOCK("Android Stock")
}

data class LockScreenSettings(
    val singlePressEnabled: Boolean = false,
    val doublePressEnabled: Boolean = true,
    val longPressEnabled: Boolean = false
)

data class RemapperSettings(
    val selectedDeviceId: String = DeviceProfile.NOTHING_PHONE_3A_PRO.id,
    val isSetupCompleted: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isEssentialSpaceDisabled: Boolean = false,

    // Gesture mappings
    val singlePressAction: RemapAction = RemapAction.DEFAULT_SINGLE_PRESS,
    val doublePressAction: RemapAction = RemapAction.DEFAULT_DOUBLE_PRESS,
    val longPressAction: RemapAction = RemapAction.DEFAULT_LONG_PRESS,

    // Gesture timings
    val doublePressTimeoutMs: Long = 350L,
    val longPressDurationMs: Long = 700L,

    // Lock screen behavior
    val lockScreenSettings: LockScreenSettings = LockScreenSettings(),

    // Haptics
    val hapticEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,

    // Visual overlay
    val overlayEnabled: Boolean = true,
    val overlayStyle: OverlayStyle = OverlayStyle.NOTHING_DOT_MATRIX,
    val overlayColorHex: String = "#E50914",
    val glowEnabled: Boolean = true,
    val portraitPosition: ButtonPosition = ButtonPosition(1.0f, 0.34f),
    val landscapePosition: ButtonPosition = ButtonPosition(0.34f, 0.0f)
)
