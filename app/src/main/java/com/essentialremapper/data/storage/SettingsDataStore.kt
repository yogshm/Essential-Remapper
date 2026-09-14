package com.essentialremapper.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.essentialremapper.data.model.HapticIntensity
import com.essentialremapper.data.model.LockScreenSettings
import com.essentialremapper.data.model.OverlayStyle
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.domain.device.ButtonPosition
import com.essentialremapper.domain.device.DeviceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "essential_remapper_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SELECTED_DEVICE_ID = stringPreferencesKey("selected_device_id")
        val IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val IS_ACCESSIBILITY_ENABLED = booleanPreferencesKey("is_accessibility_enabled")
        val IS_ESSENTIAL_SPACE_DISABLED = booleanPreferencesKey("is_essential_space_disabled")

        val SINGLE_PRESS_ACTION = stringPreferencesKey("single_press_action")
        val DOUBLE_PRESS_ACTION = stringPreferencesKey("double_press_action")
        val LONG_PRESS_ACTION = stringPreferencesKey("long_press_action")

        val DOUBLE_PRESS_TIMEOUT_MS = longPreferencesKey("double_press_timeout_ms")
        val LONG_PRESS_DURATION_MS = longPreferencesKey("long_press_duration_ms")

        val LOCK_SINGLE_PRESS = booleanPreferencesKey("lock_single_press")
        val LOCK_DOUBLE_PRESS = booleanPreferencesKey("lock_double_press")
        val LOCK_LONG_PRESS = booleanPreferencesKey("lock_long_press")

        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")

        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_STYLE = stringPreferencesKey("overlay_style")
        val OVERLAY_COLOR_HEX = stringPreferencesKey("overlay_color_hex")
        val GLOW_ENABLED = booleanPreferencesKey("glow_enabled")

        val PORTRAIT_X = floatPreferencesKey("portrait_x")
        val PORTRAIT_Y = floatPreferencesKey("portrait_y")
        val LANDSCAPE_X = floatPreferencesKey("landscape_x")
        val LANDSCAPE_Y = floatPreferencesKey("landscape_y")
    }

    val settingsFlow: Flow<RemapperSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            mapPreferencesToSettings(prefs)
        }

    private fun mapPreferencesToSettings(prefs: Preferences): RemapperSettings {
        val defaultProfile = DeviceProfile.NOTHING_PHONE_3A_PRO

        val singleActionStr = prefs[Keys.SINGLE_PRESS_ACTION] ?: RemapAction.toSerializedString(RemapAction.DEFAULT_SINGLE_PRESS)
        val doubleActionStr = prefs[Keys.DOUBLE_PRESS_ACTION] ?: RemapAction.toSerializedString(RemapAction.DEFAULT_DOUBLE_PRESS)
        val longActionStr = prefs[Keys.LONG_PRESS_ACTION] ?: RemapAction.toSerializedString(RemapAction.DEFAULT_LONG_PRESS)

        val hapticIntensityStr = prefs[Keys.HAPTIC_INTENSITY] ?: HapticIntensity.MEDIUM.name
        val hapticIntensity = try {
            HapticIntensity.valueOf(hapticIntensityStr)
        } catch (_: Exception) {
            HapticIntensity.MEDIUM
        }

        val overlayStyleStr = prefs[Keys.OVERLAY_STYLE] ?: OverlayStyle.NOTHING_DOT_MATRIX.name
        val overlayStyle = try {
            OverlayStyle.valueOf(overlayStyleStr)
        } catch (_: Exception) {
            OverlayStyle.NOTHING_DOT_MATRIX
        }

        val portraitX = (prefs[Keys.PORTRAIT_X] ?: defaultProfile.defaultPortraitPosition.x).coerceIn(0f, 1f)
        val portraitY = (prefs[Keys.PORTRAIT_Y] ?: defaultProfile.defaultPortraitPosition.y).coerceIn(0f, 1f)
        val landscapeX = (prefs[Keys.LANDSCAPE_X] ?: defaultProfile.defaultLandscapePosition.x).coerceIn(0f, 1f)
        val landscapeY = (prefs[Keys.LANDSCAPE_Y] ?: defaultProfile.defaultLandscapePosition.y).coerceIn(0f, 1f)

        return RemapperSettings(
            selectedDeviceId = prefs[Keys.SELECTED_DEVICE_ID] ?: defaultProfile.id,
            isSetupCompleted = prefs[Keys.IS_SETUP_COMPLETED] ?: false,
            isAccessibilityEnabled = prefs[Keys.IS_ACCESSIBILITY_ENABLED] ?: false,
            isEssentialSpaceDisabled = prefs[Keys.IS_ESSENTIAL_SPACE_DISABLED] ?: false,
            singlePressAction = RemapAction.fromSerializedString(singleActionStr),
            doublePressAction = RemapAction.fromSerializedString(doubleActionStr),
            longPressAction = RemapAction.fromSerializedString(longActionStr),
            doublePressTimeoutMs = prefs[Keys.DOUBLE_PRESS_TIMEOUT_MS] ?: 350L,
            longPressDurationMs = prefs[Keys.LONG_PRESS_DURATION_MS] ?: 700L,
            lockScreenSettings = LockScreenSettings(
                singlePressEnabled = prefs[Keys.LOCK_SINGLE_PRESS] ?: false,
                doublePressEnabled = prefs[Keys.LOCK_DOUBLE_PRESS] ?: true,
                longPressEnabled = prefs[Keys.LOCK_LONG_PRESS] ?: false
            ),
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            hapticIntensity = hapticIntensity,
            overlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: true,
            overlayStyle = overlayStyle,
            overlayColorHex = prefs[Keys.OVERLAY_COLOR_HEX] ?: "#E50914",
            glowEnabled = prefs[Keys.GLOW_ENABLED] ?: true,
            portraitPosition = ButtonPosition(portraitX, portraitY),
            landscapePosition = ButtonPosition(landscapeX, landscapeY)
        )
    }

    suspend fun updateSettings(transform: (RemapperSettings) -> RemapperSettings) {
        context.dataStore.edit { prefs ->
            val current = mapPreferencesToSettings(prefs)
            val updated = transform(current)

            prefs[Keys.SELECTED_DEVICE_ID] = updated.selectedDeviceId
            prefs[Keys.IS_SETUP_COMPLETED] = updated.isSetupCompleted
            prefs[Keys.IS_ACCESSIBILITY_ENABLED] = updated.isAccessibilityEnabled
            prefs[Keys.IS_ESSENTIAL_SPACE_DISABLED] = updated.isEssentialSpaceDisabled

            prefs[Keys.SINGLE_PRESS_ACTION] = RemapAction.toSerializedString(updated.singlePressAction)
            prefs[Keys.DOUBLE_PRESS_ACTION] = RemapAction.toSerializedString(updated.doublePressAction)
            prefs[Keys.LONG_PRESS_ACTION] = RemapAction.toSerializedString(updated.longPressAction)

            prefs[Keys.DOUBLE_PRESS_TIMEOUT_MS] = updated.doublePressTimeoutMs
            prefs[Keys.LONG_PRESS_DURATION_MS] = updated.longPressDurationMs

            prefs[Keys.LOCK_SINGLE_PRESS] = updated.lockScreenSettings.singlePressEnabled
            prefs[Keys.LOCK_DOUBLE_PRESS] = updated.lockScreenSettings.doublePressEnabled
            prefs[Keys.LOCK_LONG_PRESS] = updated.lockScreenSettings.longPressEnabled

            prefs[Keys.HAPTIC_ENABLED] = updated.hapticEnabled
            prefs[Keys.HAPTIC_INTENSITY] = updated.hapticIntensity.name

            prefs[Keys.OVERLAY_ENABLED] = updated.overlayEnabled
            prefs[Keys.OVERLAY_STYLE] = updated.overlayStyle.name
            prefs[Keys.OVERLAY_COLOR_HEX] = updated.overlayColorHex
            prefs[Keys.GLOW_ENABLED] = updated.glowEnabled

            prefs[Keys.PORTRAIT_X] = updated.portraitPosition.x
            prefs[Keys.PORTRAIT_Y] = updated.portraitPosition.y
            prefs[Keys.LANDSCAPE_X] = updated.landscapePosition.x
            prefs[Keys.LANDSCAPE_Y] = updated.landscapePosition.y
        }
    }
}
