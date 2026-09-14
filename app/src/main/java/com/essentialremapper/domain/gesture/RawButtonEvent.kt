package com.essentialremapper.domain.gesture

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RawButtonAction {
    DOWN,
    UP
}

data class RawButtonEvent(
    val action: RawButtonAction,
    val timestamp: Long = System.currentTimeMillis(),
    val keyCode: Int,
    val scanCode: Int,
    val deviceId: Int,
    val source: Int = 0,
    val flags: Int = 0,
    val repeatCount: Int = 0,
    val isScreenLocked: Boolean = false
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun toDiagnosticString(): String {
        return """
            $action
            KeyCode: $keyCode
            ScanCode: $scanCode
            DeviceId: $deviceId
            Source: 0x${Integer.toHexString(source)}
            Flags: 0x${Integer.toHexString(flags)}
            Locked: $isScreenLocked
            Time: $formattedTime
        """.trimIndent()
    }
}
