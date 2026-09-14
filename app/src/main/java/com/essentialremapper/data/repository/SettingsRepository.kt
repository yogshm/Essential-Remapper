package com.essentialremapper.data.repository

import com.essentialremapper.data.model.HapticIntensity
import com.essentialremapper.data.model.OverlayStyle
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.data.storage.SettingsDataStore
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.domain.device.ButtonPosition
import com.essentialremapper.domain.gesture.GestureType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

interface SettingsRepository {
    val settings: StateFlow<RemapperSettings>

    suspend fun updateSettings(transform: (RemapperSettings) -> RemapperSettings)
    suspend fun setSelectedDevice(deviceId: String)
    suspend fun setSetupCompleted(completed: Boolean)
    suspend fun setAction(gestureType: GestureType, action: RemapAction)
    suspend fun setGestureTimings(doublePressTimeoutMs: Long, longPressDurationMs: Long)
    suspend fun setHaptics(enabled: Boolean, intensity: HapticIntensity)
    suspend fun setOverlay(enabled: Boolean, style: OverlayStyle, colorHex: String, glow: Boolean)
    suspend fun setPositions(portrait: ButtonPosition, landscape: ButtonPosition)
}

class DefaultSettingsRepository(
    private val dataStore: SettingsDataStore,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : SettingsRepository {

    override val settings: StateFlow<RemapperSettings> = dataStore.settingsFlow
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = RemapperSettings()
        )

    override suspend fun updateSettings(transform: (RemapperSettings) -> RemapperSettings) {
        dataStore.updateSettings(transform)
    }

    override suspend fun setSelectedDevice(deviceId: String) {
        updateSettings { it.copy(selectedDeviceId = deviceId) }
    }

    override suspend fun setSetupCompleted(completed: Boolean) {
        updateSettings { it.copy(isSetupCompleted = completed) }
    }

    override suspend fun setAction(gestureType: GestureType, action: RemapAction) {
        updateSettings { current ->
            when (gestureType) {
                GestureType.SinglePress -> current.copy(singlePressAction = action)
                GestureType.DoublePress -> current.copy(doublePressAction = action)
                GestureType.LongPress -> current.copy(longPressAction = action)
            }
        }
    }

    override suspend fun setGestureTimings(doublePressTimeoutMs: Long, longPressDurationMs: Long) {
        updateSettings {
            it.copy(
                doublePressTimeoutMs = doublePressTimeoutMs,
                longPressDurationMs = longPressDurationMs
            )
        }
    }

    override suspend fun setHaptics(enabled: Boolean, intensity: HapticIntensity) {
        updateSettings {
            it.copy(
                hapticEnabled = enabled,
                hapticIntensity = intensity
            )
        }
    }

    override suspend fun setOverlay(
        enabled: Boolean,
        style: OverlayStyle,
        colorHex: String,
        glow: Boolean
    ) {
        updateSettings {
            it.copy(
                overlayEnabled = enabled,
                overlayStyle = style,
                overlayColorHex = colorHex,
                glowEnabled = glow
            )
        }
    }

    override suspend fun setPositions(portrait: ButtonPosition, landscape: ButtonPosition) {
        updateSettings {
            it.copy(
                portraitPosition = portrait,
                landscapePosition = landscape
            )
        }
    }
}
