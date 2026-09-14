package com.essentialremapper.data.model

import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.domain.device.ButtonPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemapperSettingsTest {

    @Test
    fun testRemapActionSerializationRoundTrip() {
        val actions = listOf(
            RemapAction.None,
            RemapAction.Flashlight,
            RemapAction.RotationLock,
            RemapAction.MediaPlayPause,
            RemapAction.MediaNextTrack,
            RemapAction.MediaPreviousTrack,
            RemapAction.LaunchApp("com.nothing.camera", "Camera"),
            RemapAction.DeepLink("https://nothing.tech"),
            RemapAction.SystemAction(RemapAction.SystemActionType.NOTIFICATIONS)
        )

        for (action in actions) {
            val serialized = RemapAction.toSerializedString(action)
            val deserialized = RemapAction.fromSerializedString(serialized)
            assertEquals("Failed for action $action", action, deserialized)
        }
    }

    @Test
    fun testButtonPositionValidCoordinates() {
        val pos = ButtonPosition(x = 1.0f, y = 0.35f)
        assertEquals(1.0f, pos.x, 0.001f)
        assertEquals(0.35f, pos.y, 0.001f)
    }

    @Test
    fun testButtonPositionInvalidCoordinatesThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            ButtonPosition(x = 1.2f, y = 0.5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ButtonPosition(x = 0.5f, y = -0.1f)
        }
    }

    @Test
    fun testDefaultSettingsIntegrity() {
        val settings = RemapperSettings()
        assertEquals("nothing_phone_3a_pro", settings.selectedDeviceId)
        assertEquals(RemapAction.Flashlight, settings.singlePressAction)
        assertEquals(350L, settings.doublePressTimeoutMs)
        assertEquals(700L, settings.longPressDurationMs)
        assertEquals(HapticIntensity.MEDIUM, settings.hapticIntensity)
        assertEquals(OverlayStyle.NOTHING_DOT_MATRIX, settings.overlayStyle)
    }
}
