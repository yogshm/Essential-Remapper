package com.essentialremapper.domain.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceDetectorTest {

    private lateinit var detector: DeviceDetector

    @Before
    fun setUp() {
        detector = DeviceDetector()
    }

    @Test
    fun testDetectNothingPhone3aProByModel() {
        val profile = detector.detectDevice(
            manufacturer = "Nothing",
            brand = "Nothing",
            model = "A059P",
            device = "Asteroids",
            product = "AsteroidsProIND"
        )
        assertEquals("Nothing Phone (3a) Pro", profile.displayName)
        assertEquals(250, profile.essentialButtonScanCode)
        assertEquals(0, profile.essentialButtonKeyCode)
        assertTrue(profile.isOfficiallySupported)
    }

    @Test
    fun testDetectNothingPhone3aProByCodename() {
        val profile = detector.detectDevice(
            manufacturer = "Nothing",
            brand = "Nothing",
            model = "UnknownSubModel",
            device = "AsteroidsProIND",
            product = "AsteroidsProIND"
        )
        assertEquals("Nothing Phone (3a) Pro", profile.displayName)
        assertEquals(250, profile.essentialButtonScanCode)
        assertTrue(profile.isOfficiallySupported)
    }

    @Test
    fun testDetectNothingPhone3aStandard() {
        val profile = detector.detectDevice(
            manufacturer = "Nothing",
            brand = "Nothing",
            model = "A142",
            device = "Asteroids",
            product = "AsteroidsIND"
        )
        assertEquals("Nothing Phone (3a)", profile.displayName)
        assertEquals(250, profile.essentialButtonScanCode)
        assertTrue(profile.isOfficiallySupported)
    }

    @Test
    fun testDetectNothingPhone3() {
        val profile = detector.detectDevice(
            manufacturer = "Nothing",
            brand = "Nothing",
            model = "A065",
            device = "Tetris",
            product = "TetrisGlobal"
        )
        assertEquals("Nothing Phone (3)", profile.displayName)
        assertEquals(250, profile.essentialButtonScanCode)
        assertTrue(profile.isOfficiallySupported)
    }

    @Test
    fun testDetectCmfPhone1() {
        val profile = detector.detectDevice(
            manufacturer = "Nothing",
            brand = "CMF",
            model = "A015",
            device = "Galaga",
            product = "GalagaGlobal"
        )
        assertEquals("CMF Phone 1", profile.displayName)
        assertEquals(250, profile.essentialButtonScanCode)
        assertTrue(profile.isOfficiallySupported)
    }

    @Test
    fun testDetectFallbackForUnknownNothingDevice() {
        val profile = detector.detectDevice(
            manufacturer = "Nothing",
            brand = "Nothing",
            model = "A999",
            device = "UnknownDevice",
            product = "UnknownProduct"
        )
        assertEquals("Other Nothing / CMF Phone", profile.displayName)
        assertEquals(250, profile.essentialButtonScanCode)
        assertTrue(profile.isOfficiallySupported)
    }

    @Test
    fun testDetectUnsupportedNonNothingDevice() {
        val profile = detector.detectDevice(
            manufacturer = "Google",
            brand = "Google",
            model = "Pixel 9 Pro",
            device = "caiman",
            product = "caiman"
        )
        assertEquals("Unsupported Device", profile.displayName)
        assertFalse(profile.isOfficiallySupported)
        assertEquals(-1, profile.essentialButtonScanCode)
    }
}
