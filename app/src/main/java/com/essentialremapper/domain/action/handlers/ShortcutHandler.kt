package com.essentialremapper.domain.action.handlers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import com.essentialremapper.domain.action.ActionHandler
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.util.AppLogger

/**
 * Handles execution of Android launcher app shortcuts.
 */
class ShortcutHandler(
    private val context: Context
) : ActionHandler {

    companion object {
        private const val TAG = "ShortcutHandler"
    }

    override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
        if (action !is RemapAction.AppShortcut) {
            return ActionResult.Failure("Unsupported shortcut action: ${action.id}")
        }

        if (action.packageName.isBlank() && action.intentUri.isBlank()) {
            return ActionResult.Failure("Shortcut configuration is empty")
        }

        // 1. Try launching via LauncherApps.startShortcut if supported
        if (action.shortcutId.isNotBlank() && action.packageName.isNotBlank()) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null) {
                    val userHandle = Process.myUserHandle()
                    launcherApps.startShortcut(
                        action.packageName,
                        action.shortcutId,
                        null,
                        null,
                        userHandle
                    )
                    AppLogger.i("Started shortcut '${action.shortcutLabel}' via LauncherApps", TAG)
                    return ActionResult.Success("Launched shortcut: ${action.shortcutLabel}")
                }
            } catch (e: SecurityException) {
                AppLogger.w("LauncherApps permission restricted for shortcut: ${e.message}", TAG)
            } catch (e: Exception) {
                AppLogger.w("LauncherApps.startShortcut failed: ${e.message}, attempting URI fallback", TAG)
            }
        }

        // 2. Fallback to Intent URI if available
        if (action.intentUri.isNotBlank()) {
            try {
                val parsedIntent = Intent.parseUri(action.intentUri, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(parsedIntent)
                AppLogger.i("Started shortcut '${action.shortcutLabel}' via parsed Intent URI", TAG)
                return ActionResult.Success("Launched shortcut: ${action.shortcutLabel}")
            } catch (e: ActivityNotFoundException) {
                AppLogger.e("ActivityNotFound for shortcut intent: ${e.message}", TAG, e)
                return ActionResult.Failure("Shortcut '${action.shortcutLabel}' is no longer available", e)
            } catch (e: Exception) {
                AppLogger.e("Error launching shortcut URI: ${e.message}", TAG, e)
                return ActionResult.Failure("Failed to launch shortcut: ${e.message}", e)
            }
        }

        return ActionResult.Failure("Shortcut '${action.shortcutLabel}' could not be resolved")
    }
}
