package com.essentialremapper.domain.gesture

import com.essentialremapper.domain.device.ButtonPosition
import com.essentialremapper.domain.device.DeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ButtonEventDetectorTest {

    private lateinit var detector: ButtonEventDetector
    private val nothingPhone3aPro = DeviceProfile.NOTHING_PHONE_3A_PRO

    @Before
    fun setUp() {
        detector = ButtonEventDetector(maxHistorySize = 50)
    }

    @Test
    fun testMatchesScanCode250() {
        // Physical Essential Button event signature from Nothing Phone (3a) Pro
        val matches = detector.matches(
            keyCode = 0,
            scanCode = 250,
            activeProfile = nothingPhone3aPro
        )
        assertTrue("Event with scanCode 250 must match Nothing Phone 3a Pro profile", matches)
    }

    @Test
    fun testRejectsUnrelatedScanCodes() {
        // Volume Down (114), Volume Up (115), Power (26), Camera shutter (212)
        val unrelatedScanCodes = listOf(26, 114, 115, 116, 212, 500)
        for (scanCode in unrelatedScanCodes) {
            val matches = detector.matches(
                keyCode = 0,
                scanCode = scanCode,
                activeProfile = nothingPhone3aPro
            )
            assertFalse("ScanCode $scanCode should be rejected", matches)
        }
    }

    @Test
    fun testKeyCode0WithScanCode250() {
        // Verify specifically that keyCode = 0 (KEYCODE_UNKNOWN) + scanCode = 250 matches
        val matches = detector.matches(
            keyCode = 0,
            scanCode = 250,
            activeProfile = nothingPhone3aPro
        )
        assertTrue("keyCode 0 + scanCode 250 must match", matches)
    }

    @Test
    fun testMatchingPolicyWithExplicitNonZeroKeyCode() {
        // Custom profile requiring exact keyCode = 131 and scanCode = 250
        val customProfile = DeviceProfile(
            id = "custom_device",
            displayName = "Custom Device",
            modelNames = listOf("CUSTOM"),
            deviceCodename = "custom",
            essentialButtonScanCode = 250,
            essentialButtonKeyCode = 131,
            defaultPortraitPosition = ButtonPosition(1f, 0.35f),
            defaultLandscapePosition = ButtonPosition(0.35f, 0f),
            isOfficiallySupported = true
        )

        // Matching both keyCode 131 and scanCode 250 -> TRUE
        assertTrue(detector.matches(keyCode = 131, scanCode = 250, activeProfile = customProfile))

        // Mismatched keyCode 0 when profile expects 131 -> FALSE
        assertFalse(detector.matches(keyCode = 0, scanCode = 250, activeProfile = customProfile))
    }

    @Test
    fun testRejectsWhenProfileIsUnsupported() {
        val unsupportedProfile = DeviceProfile.UNSUPPORTED
        val matches = detector.matches(
            keyCode = 0,
            scanCode = 250,
            activeProfile = unsupportedProfile
        )
        assertFalse("Unsupported device profiles should reject all button events", matches)
    }

    @Test
    fun testDetectsActionDown() {
        val downEvent = RawButtonEvent(
            action = RawButtonAction.DOWN,
            timestamp = 1000L,
            keyCode = 0,
            scanCode = 250,
            deviceId = 2,
            source = 0x101,
            flags = 0x8,
            isScreenLocked = false
        )

        detector.onButtonEvent(downEvent)

        val history = detector.diagnosticHistory.value
        assertEquals(1, history.size)
        assertEquals(RawButtonAction.DOWN, history[0].action)
        assertEquals(250, history[0].scanCode)
        assertEquals(0, history[0].keyCode)
        assertEquals(2, history[0].deviceId)
    }

    @Test
    fun testDetectsActionUp() {
        val downEvent = RawButtonEvent(
            action = RawButtonAction.DOWN,
            timestamp = 1000L,
            keyCode = 0,
            scanCode = 250,
            deviceId = 2,
            source = 0x101,
            flags = 0x8,
            isScreenLocked = false
        )
        val upEvent = RawButtonEvent(
            action = RawButtonAction.UP,
            timestamp = 1080L,
            keyCode = 0,
            scanCode = 250,
            deviceId = 2,
            source = 0x101,
            flags = 0x8,
            isScreenLocked = false
        )

        detector.onButtonEvent(downEvent)
        detector.onButtonEvent(upEvent)

        val history = detector.diagnosticHistory.value
        assertEquals(2, history.size)
        assertEquals(RawButtonAction.UP, history[0].action) // Latest is first
        assertEquals(RawButtonAction.DOWN, history[1].action)
    }

    @Test
    fun testDiagnosticHistoryCappedAtMaxSize() {
        for (i in 1..60) {
            val event = RawButtonEvent(
                action = if (i % 2 == 1) RawButtonAction.DOWN else RawButtonAction.UP,
                timestamp = i.toLong(),
                keyCode = 0,
                scanCode = 250,
                deviceId = 2
            )
            detector.onButtonEvent(event)
        }

        val history = detector.diagnosticHistory.value
        assertEquals(50, history.size)
        assertEquals(60L, history[0].timestamp) // Most recent event
    }
}
