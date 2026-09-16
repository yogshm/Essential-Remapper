package com.essentialremapper.domain.gesture

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a recognized physical interaction with the Essential Button.
 *
 * @param type The identified gesture (SinglePress, DoublePress, LongPress)
 * @param timestamp Time in milliseconds when the gesture was confirmed
 * @param isScreenLocked Whether the device was locked when the gesture was initiated
 */
data class GestureEvent(
    val type: GestureType,
    val timestamp: Long = System.currentTimeMillis(),
    val isScreenLocked: Boolean = false
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun toDiagnosticString(): String {
        val lockTag = if (isScreenLocked) " [LOCKED]" else ""
        return "${type.displayName}$lockTag at $formattedTime"
    }
}
