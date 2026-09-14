package com.essentialremapper.domain.gesture

import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.util.AppLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ButtonEventDetector(
    private val maxHistorySize: Int = 50
) {
    private val _rawEvents = MutableSharedFlow<RawButtonEvent>(extraBufferCapacity = 64)
    val rawEvents: SharedFlow<RawButtonEvent> = _rawEvents.asSharedFlow()

    private val _diagnosticHistory = MutableStateFlow<List<RawButtonEvent>>(emptyList())
    val diagnosticHistory: StateFlow<List<RawButtonEvent>> = _diagnosticHistory.asStateFlow()

    /**
     * Determines whether an incoming hardware KeyEvent matches the configured Essential Button profile.
     * Primary matching criterion: hardware scanCode.
     * Optional secondary criterion: keyCode (if non-zero in profile).
     */
    fun matches(keyCode: Int, scanCode: Int, activeProfile: DeviceProfile): Boolean {
        // If profile is explicitly unsupported, reject all
        if (!activeProfile.isOfficiallySupported || activeProfile.essentialButtonScanCode <= 0) {
            return false
        }

        // 1. Primary matching condition: hardware scanCode must match active profile
        if (scanCode != activeProfile.essentialButtonScanCode) {
            return false
        }

        // 2. Secondary verification: if profile specifies a non-zero keyCode, verify it
        if (activeProfile.essentialButtonKeyCode != 0 && keyCode != activeProfile.essentialButtonKeyCode) {
            return false
        }

        // On Nothing OS / CMF, Essential Button emits keyCode 0 (KEYCODE_UNKNOWN) and scanCode 250
        return true
    }

    /**
     * Dispatches a verified physical button event to consumers and records it in diagnostic history.
     */
    fun onButtonEvent(event: RawButtonEvent) {
        AppLogger.d("ButtonEventDetector: ${event.action} (scanCode=${event.scanCode}, keyCode=${event.keyCode})")

        // Emit to stream for future GestureRecognizer
        _rawEvents.tryEmit(event)

        // Record in diagnostic history (capped at maxHistorySize, latest first)
        val current = _diagnosticHistory.value.toMutableList()
        current.add(0, event)
        if (current.size > maxHistorySize) {
            current.removeAt(current.lastIndex)
        }
        _diagnosticHistory.value = current
    }

    fun clearHistory() {
        _diagnosticHistory.value = emptyList()
    }
}
