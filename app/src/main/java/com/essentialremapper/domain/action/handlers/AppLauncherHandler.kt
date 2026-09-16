package com.essentialremapper.domain.action.handlers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.essentialremapper.domain.action.ActionHandler
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.util.AppLogger

/**
 * Handles launching installed applications and the system camera.
 * Uses standard Android platform Intents and PackageManager without hardcoded vendor packages.
 */
class AppLauncherHandler(
    private val context: Context
) : ActionHandler {

    companion object {
        private const val TAG = "AppLauncherHandler"
    }

    override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
        return when (action) {
            is RemapAction.Camera -> launchCamera(isScreenLocked)
            is RemapAction.LaunchApp -> launchApplication(action.packageName, action.appName)
            else -> ActionResult.Failure("Unsupported app launcher action: ${action.id}")
        }
    }

    private fun launchCamera(isScreenLocked: Boolean): ActionResult {
        return try {
            // Use standard Android secure camera Intent on lockscreen, standard camera Intent when unlocked
            val actionName = if (isScreenLocked) {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }

            val cameraIntent = Intent(actionName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Verify a camera handler exists
            val pm = context.packageManager
            if (cameraIntent.resolveActivity(pm) != null) {
                context.startActivity(cameraIntent)
                AppLogger.i("Launched camera via standard Intent ($actionName)", TAG)
                ActionResult.Success("Camera launched")
            } else {
                // Fallback to standard generic camera intent
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fallbackIntent.resolveActivity(pm) != null) {
                    context.startActivity(fallbackIntent)
                    ActionResult.Success("Camera capture launched")
                } else {
                    ActionResult.Failure("No camera application found on device")
                }
            }
        } catch (e: ActivityNotFoundException) {
            AppLogger.e("No camera activity found: ${e.message}", TAG, e)
            ActionResult.Failure("Camera app not found", e)
        } catch (e: SecurityException) {
            AppLogger.e("SecurityException launching camera: ${e.message}", TAG, e)
            ActionResult.Failure("Security restriction launching camera: ${e.message}", e)
        } catch (e: Exception) {
            AppLogger.e("Error launching camera: ${e.message}", TAG, e)
            ActionResult.Failure("Failed to launch camera: ${e.message}", e)
        }
    }

    private fun launchApplication(packageName: String, appName: String): ActionResult {
        if (packageName.isBlank()) {
            return ActionResult.Failure("No package name configured for application launch")
        }

        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
                ?: return ActionResult.Failure("Application '$appName' ($packageName) is not installed or has no launchable activity")

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(launchIntent)
            AppLogger.i("Launched application '$appName' ($packageName)", TAG)
            ActionResult.Success("Launched $appName")
        } catch (e: ActivityNotFoundException) {
            AppLogger.e("ActivityNotFoundException launching $packageName: ${e.message}", TAG, e)
            ActionResult.Failure("App '$appName' not found", e)
        } catch (e: SecurityException) {
            AppLogger.e("SecurityException launching $packageName: ${e.message}", TAG, e)
            ActionResult.Failure("Permission denied launching '$appName'", e)
        } catch (e: Exception) {
            AppLogger.e("Unexpected error launching $packageName: ${e.message}", TAG, e)
            ActionResult.Failure("Failed to launch '$appName': ${e.message}", e)
        }
    }
}
