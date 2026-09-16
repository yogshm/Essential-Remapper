package com.essentialremapper.domain.action.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Build
import com.essentialremapper.domain.action.ActionHandler
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.util.AppLogger

/**
 * Executes official Android AccessibilityService global actions.
 * Checks API availability and service connection state before attempting execution.
 */
class SystemActionHandler(
    private val serviceProvider: () -> AccessibilityService?
) : ActionHandler {

    companion object {
        private const val TAG = "SystemActionHandler"
    }

    override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
        if (action !is RemapAction.SystemAction) {
            return ActionResult.Failure("Unsupported system action: ${action.id}")
        }

        val service = serviceProvider()
            ?: return ActionResult.Unavailable("Accessibility Service is not connected. Enable the service in Settings.")

        val globalActionId = when (action.systemType) {
            RemapAction.SystemActionType.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            RemapAction.SystemActionType.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            RemapAction.SystemActionType.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
            RemapAction.SystemActionType.NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            RemapAction.SystemActionType.QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            RemapAction.SystemActionType.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    return ActionResult.Unavailable("Lock Screen action requires Android 9 (API 28)+")
                }
            }
            RemapAction.SystemActionType.TAKE_SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                } else {
                    return ActionResult.Unavailable("Screenshot action requires Android 9 (API 28)+")
                }
            }
        }

        return try {
            val success = service.performGlobalAction(globalActionId)
            if (success) {
                AppLogger.i("Global action '${action.systemType.displayName}' executed successfully", TAG)
                ActionResult.Success("Performed ${action.systemType.displayName}")
            } else {
                AppLogger.w("Global action '${action.systemType.displayName}' was rejected by the system", TAG)
                ActionResult.Failure("System rejected ${action.systemType.displayName}")
            }
        } catch (e: Exception) {
            AppLogger.e("Exception performing global action '${action.systemType.displayName}': ${e.message}", TAG, e)
            ActionResult.Failure("Failed to execute ${action.systemType.displayName}: ${e.message}", e)
        }
    }
}
