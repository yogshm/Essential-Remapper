package com.essentialremapper.domain.action

import com.essentialremapper.domain.gesture.GestureType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic record of an executed or attempted action dispatched from a recognized physical gesture.
 */
data class ActionExecutionEvent(
    val gesture: GestureType,
    val action: RemapAction,
    val result: ActionResult,
    val timestamp: Long = System.currentTimeMillis(),
    val isScreenLocked: Boolean = false
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun toDiagnosticString(): String {
        val lockTag = if (isScreenLocked) " [LOCKED]" else ""
        val statusTag = when (result) {
            is ActionResult.Success -> "SUCCESS"
            is ActionResult.Failure -> "FAILURE: ${result.message}"
            is ActionResult.Unavailable -> "BLOCKED: ${result.message}"
        }
        return "${gesture.displayName}$lockTag -> ${action.title}: $statusTag at $formattedTime"
    }
}
