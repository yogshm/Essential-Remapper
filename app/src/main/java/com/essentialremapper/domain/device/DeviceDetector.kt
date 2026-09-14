package com.essentialremapper.domain.device

import android.os.Build

class DeviceDetector {

    private val supportedProfiles = listOf(
        DeviceProfile.NOTHING_PHONE_3A_PRO,
        DeviceProfile.NOTHING_PHONE_3A,
        DeviceProfile.NOTHING_PHONE_3,
        DeviceProfile.CMF_PHONE_1
    )

    /**
     * Detects the profile using Android runtime Build properties.
     */
    fun detectCurrentDevice(): DeviceProfile {
        return detectDevice(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT
        )
    }

    /**
     * Pure function for device matching, easily testable without Android framework.
     */
    fun detectDevice(
        manufacturer: String,
        brand: String,
        model: String,
        device: String,
        product: String
    ): DeviceProfile {
        val cleanModel = model.trim().uppercase()
        val cleanDevice = device.trim()
        val cleanProduct = product.trim()
        val cleanManufacturer = manufacturer.trim().lowercase()
        val cleanBrand = brand.trim().lowercase()

        val isNothingOrCmf = cleanManufacturer.contains("nothing") ||
                cleanManufacturer.contains("cmf") ||
                cleanBrand.contains("nothing") ||
                cleanBrand.contains("cmf")

        // 1. Direct Model Name Match
        for (profile in supportedProfiles) {
            if (profile.modelNames.any { it.equals(cleanModel, ignoreCase = true) }) {
                return profile
            }
        }

        // 2. Codename / Product Match
        if (cleanProduct.contains("AsteroidsPro", ignoreCase = true) ||
            cleanDevice.contains("AsteroidsPro", ignoreCase = true)
        ) {
            return DeviceProfile.NOTHING_PHONE_3A_PRO
        }

        if (cleanProduct.contains("Asteroids", ignoreCase = true) ||
            cleanDevice.contains("Asteroids", ignoreCase = true)
        ) {
            return DeviceProfile.NOTHING_PHONE_3A
        }

        if (cleanProduct.contains("Tetris", ignoreCase = true) ||
            cleanDevice.contains("Tetris", ignoreCase = true)
        ) {
            return DeviceProfile.NOTHING_PHONE_3
        }

        if (cleanProduct.contains("Galaga", ignoreCase = true) ||
            cleanDevice.contains("Galaga", ignoreCase = true)
        ) {
            return DeviceProfile.CMF_PHONE_1
        }

        // 3. Fallback for Nothing / CMF
        if (isNothingOrCmf) {
            return DeviceProfile.OTHER_NOTHING_CMF
        }

        // 4. Non-Nothing/CMF devices are marked unsupported
        return DeviceProfile.UNSUPPORTED
    }
}
