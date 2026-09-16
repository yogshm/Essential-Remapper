package com.essentialremapper.domain.action.handlers

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.essentialremapper.domain.action.ActionHandler
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.util.AppLogger

/**
 * Dispatches standard Android media key events via AudioManager.
 * Note: Android dispatches media key events to the media session currently holding audio focus.
 */
class MediaHandler(
    private val context: Context
) : ActionHandler {

    companion object {
        private const val TAG = "MediaHandler"
    }

    private val audioManager: AudioManager? by lazy {
        try {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (e: Exception) {
            AppLogger.e("Failed to acquire AudioManager: ${e.message}", TAG, e)
            null
        }
    }

    override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
        val am = audioManager ?: return ActionResult.Unavailable("Audio service unavailable")

        val keyCode = when (action) {
            is RemapAction.MediaPlayPause -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            is RemapAction.MediaNextTrack -> KeyEvent.KEYCODE_MEDIA_NEXT
            is RemapAction.MediaPreviousTrack -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return ActionResult.Failure("Unsupported media action: ${action.id}")
        }

        return try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)

            am.dispatchMediaKeyEvent(downEvent)
            am.dispatchMediaKeyEvent(upEvent)

            val isMusicActive = am.isMusicActive
            AppLogger.i("Dispatched media key $keyCode (isMusicActive=$isMusicActive)", TAG)
            ActionResult.Success("${action.title} event dispatched (playbackActive=$isMusicActive)")
        } catch (e: SecurityException) {
            AppLogger.e("SecurityException dispatching media key: ${e.message}", TAG, e)
            ActionResult.Failure("Security restriction dispatching media key: ${e.message}", e)
        } catch (e: Exception) {
            AppLogger.e("Error dispatching media key: ${e.message}", TAG, e)
            ActionResult.Failure("Failed to dispatch media command: ${e.message}", e)
        }
    }
}
