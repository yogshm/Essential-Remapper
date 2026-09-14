package com.essentialremapper.domain.device

class DeviceRepository(
    private val deviceDetector: DeviceDetector = DeviceDetector()
) {
    val allProfiles: List<DeviceProfile> = listOf(
        DeviceProfile.NOTHING_PHONE_3A_PRO,
        DeviceProfile.NOTHING_PHONE_3A,
        DeviceProfile.NOTHING_PHONE_3,
        DeviceProfile.CMF_PHONE_1,
        DeviceProfile.OTHER_NOTHING_CMF
    )

    fun getDetectedProfile(): DeviceProfile {
        return deviceDetector.detectCurrentDevice()
    }

    fun getProfileById(id: String): DeviceProfile {
        return allProfiles.find { it.id == id } ?: DeviceProfile.OTHER_NOTHING_CMF
    }
}
