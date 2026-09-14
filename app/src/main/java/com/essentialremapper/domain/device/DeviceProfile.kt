package com.essentialremapper.domain.device

/**
 * Encapsulates device-specific hardware parameters for Nothing and CMF devices.
 */
data class DeviceProfile(
    val id: String,
    val displayName: String,
    val modelNames: List<String>,
    val deviceCodename: String,
    val essentialButtonScanCode: Int = 250,
    val essentialButtonKeyCode: Int = 0,
    val defaultPortraitPosition: ButtonPosition = ButtonPosition(x = 1.0f, y = 0.35f),
    val defaultLandscapePosition: ButtonPosition = ButtonPosition(x = 0.35f, y = 0.0f),
    val isOfficiallySupported: Boolean = true
) {
    companion object {
        /**
         * Nothing Phone (3a) Pro - Hardware validated on physical device A059P / AsteroidsProIND.
         */
        val NOTHING_PHONE_3A_PRO = DeviceProfile(
            id = "nothing_phone_3a_pro",
            displayName = "Nothing Phone (3a) Pro",
            modelNames = listOf("A059P", "A059", "A142P"),
            deviceCodename = "AsteroidsProIND",
            essentialButtonScanCode = 250,
            essentialButtonKeyCode = 0,
            defaultPortraitPosition = ButtonPosition(x = 1.0f, y = 0.34f),
            defaultLandscapePosition = ButtonPosition(x = 0.34f, y = 0.0f),
            isOfficiallySupported = true
        )

        /**
         * Nothing Phone (3a) standard profile.
         */
        val NOTHING_PHONE_3A = DeviceProfile(
            id = "nothing_phone_3a",
            displayName = "Nothing Phone (3a)",
            modelNames = listOf("A142", "A059"),
            deviceCodename = "Asteroids",
            essentialButtonScanCode = 250,
            essentialButtonKeyCode = 0,
            defaultPortraitPosition = ButtonPosition(x = 1.0f, y = 0.35f),
            defaultLandscapePosition = ButtonPosition(x = 0.35f, y = 0.0f),
            isOfficiallySupported = true
        )

        /**
         * Nothing Phone (3) flagship profile.
         */
        val NOTHING_PHONE_3 = DeviceProfile(
            id = "nothing_phone_3",
            displayName = "Nothing Phone (3)",
            modelNames = listOf("A065", "A065P"),
            deviceCodename = "Tetris",
            essentialButtonScanCode = 250,
            essentialButtonKeyCode = 0,
            defaultPortraitPosition = ButtonPosition(x = 1.0f, y = 0.36f),
            defaultLandscapePosition = ButtonPosition(x = 0.36f, y = 0.0f),
            isOfficiallySupported = true
        )

        /**
         * CMF Phone 1 profile.
         */
        val CMF_PHONE_1 = DeviceProfile(
            id = "cmf_phone_1",
            displayName = "CMF Phone 1",
            modelNames = listOf("A015"),
            deviceCodename = "Galaga",
            essentialButtonScanCode = 250,
            essentialButtonKeyCode = 0,
            defaultPortraitPosition = ButtonPosition(x = 1.0f, y = 0.33f),
            defaultLandscapePosition = ButtonPosition(x = 0.33f, y = 0.0f),
            isOfficiallySupported = true
        )

        /**
         * Fallback profile for future or unlisted Nothing/CMF devices.
         */
        val OTHER_NOTHING_CMF = DeviceProfile(
            id = "other_nothing_cmf",
            displayName = "Other Nothing / CMF Phone",
            modelNames = emptyList(),
            deviceCodename = "generic_nothing",
            essentialButtonScanCode = 250,
            essentialButtonKeyCode = 0,
            defaultPortraitPosition = ButtonPosition(x = 1.0f, y = 0.35f),
            defaultLandscapePosition = ButtonPosition(x = 0.35f, y = 0.0f),
            isOfficiallySupported = true
        )

        /**
         * Explicit unsupported marker for non-Nothing/CMF devices.
         */
        val UNSUPPORTED = DeviceProfile(
            id = "unsupported",
            displayName = "Unsupported Device",
            modelNames = emptyList(),
            deviceCodename = "unsupported",
            essentialButtonScanCode = -1,
            essentialButtonKeyCode = -1,
            defaultPortraitPosition = ButtonPosition(x = 1.0f, y = 0.5f),
            defaultLandscapePosition = ButtonPosition(x = 0.5f, y = 0.0f),
            isOfficiallySupported = false
        )
    }
}
