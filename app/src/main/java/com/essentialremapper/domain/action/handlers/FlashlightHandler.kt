package com.essentialremapper.domain.action.handlers

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.essentialremapper.domain.action.ActionHandler
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.util.AppLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Controls device flashlight hardware via CameraManager.
 * Tracks true hardware state through registered TorchCallback.
 */
class FlashlightHandler(
    private val context: Context
) : ActionHandler {

    companion object {
        private const val TAG = "FlashlightHandler"
    }

    private val cameraManager: CameraManager? by lazy {
        try {
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (e: Exception) {
            AppLogger.e("Failed to acquire CameraManager: ${e.message}", TAG, e)
            null
        }
    }

    private val mutex = Mutex()

    @Volatile
    var isTorchOn: Boolean = false
        private set

    @Volatile
    var isTorchAvailable: Boolean = true
        private set

    private var targetCameraId: String? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == targetCameraId) {
                isTorchOn = enabled
                isTorchAvailable = true
                AppLogger.d("TorchCallback: camera $cameraId torchMode = $enabled", TAG)
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == targetCameraId) {
                isTorchAvailable = false
                isTorchOn = false
                AppLogger.w("TorchCallback: camera $cameraId torch unavailable", TAG)
            }
        }
    }

    init {
        initializeTorch()
    }

    private fun initializeTorch() {
        try {
            val cm = cameraManager ?: return
            resolveRearFlashCameraId(cm)

            // Register TorchCallback to observe true hardware state
            cm.registerTorchCallback(torchCallback, null)
            AppLogger.i("FlashlightHandler initialized with camera ID: $targetCameraId", TAG)
        } catch (e: Exception) {
            AppLogger.w("Error during FlashlightHandler initialization: ${e.message}", TAG)
        }
    }

    private fun resolveRearFlashCameraId(cm: CameraManager) {
        try {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)

                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCameraId = id
                    return
                }
            }

            // Fallback: any camera with flash
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    targetCameraId = id
                    return
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to resolve rear camera with flash: ${e.message}", TAG)
        }
    }

    override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
        return mutex.withLock {
            val cm = cameraManager
                ?: return@withLock ActionResult.Unavailable("Camera service unavailable")

            val cameraId = targetCameraId
                ?: run {
                    resolveRearFlashCameraId(cm)
                    targetCameraId
                }
                ?: return@withLock ActionResult.Failure("No camera with flash hardware detected")

            if (!isTorchAvailable) {
                return@withLock ActionResult.Unavailable("Flashlight is currently in use or unavailable")
            }

            try {
                val newMode = !isTorchOn
                cm.setTorchMode(cameraId, newMode)
                isTorchOn = newMode

                if (newMode) {
                    AppLogger.i("Flashlight ON", TAG)
                    ActionResult.Success("Flashlight ON")
                } else {
                    AppLogger.i("Flashlight OFF", TAG)
                    ActionResult.Success("Flashlight OFF")
                }
            } catch (e: CameraAccessException) {
                AppLogger.e("CameraAccessException while setting torch mode: ${e.message}", TAG, e)
                ActionResult.Failure("Camera access denied or busy: ${e.message}", e)
            } catch (e: SecurityException) {
                AppLogger.e("SecurityException setting torch: ${e.message}", TAG, e)
                ActionResult.Failure("Security exception: ${e.message}", e)
            } catch (e: Exception) {
                AppLogger.e("Unexpected error setting torch: ${e.message}", TAG, e)
                ActionResult.Failure("Flashlight error: ${e.message}", e)
            }
        }
    }

    fun unregister() {
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) {}
    }
}
