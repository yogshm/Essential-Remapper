package com.essentialremapper.domain.action

import android.content.Context
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.action.handlers.AppLauncherHandler
import com.essentialremapper.domain.action.handlers.DeepLinkHandler
import com.essentialremapper.domain.action.handlers.FlashlightHandler
import com.essentialremapper.domain.action.handlers.MediaHandler
import com.essentialremapper.domain.action.handlers.ShortcutHandler
import com.essentialremapper.domain.action.handlers.SystemActionHandler
import com.essentialremapper.domain.gesture.GestureEvent
import com.essentialremapper.domain.gesture.GestureType
import com.essentialremapper.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central router mapping recognized GestureEvents to configured RemapActions.
 * Enforces lock-screen security gating, triggers haptics, and logs diagnostic history.
 */
class ActionDispatcher(
    private val settingsProvider: () -> RemapperSettings,
    private val hapticsController: HapticsController,
    val flashlightHandler: ActionHandler,
    val mediaHandler: ActionHandler,
    val appLauncherHandler: ActionHandler,
    val shortcutHandler: ActionHandler,
    val deepLinkHandler: ActionHandler,
    val systemActionHandler: ActionHandler,
    private val maxHistorySize: Int = 50
) {
    /**
     * Convenience constructor for Android application runtime with standard handlers.
     */
    constructor(
        context: Context,
        settingsProvider: () -> RemapperSettings,
        hapticsController: HapticsController,
        systemActionHandler: ActionHandler = SystemActionHandler { null },
        maxHistorySize: Int = 50
    ) : this(
        settingsProvider = settingsProvider,
        hapticsController = hapticsController,
        flashlightHandler = FlashlightHandler(context),
        mediaHandler = MediaHandler(context),
        appLauncherHandler = AppLauncherHandler(context),
        shortcutHandler = ShortcutHandler(context),
        deepLinkHandler = DeepLinkHandler(context),
        systemActionHandler = systemActionHandler,
        maxHistorySize = maxHistorySize
    )

    companion object {
        private const val TAG = "ActionDispatcher"
    }

    private val mutex = Mutex()

    private val _latestExecution = MutableStateFlow<ActionExecutionEvent?>(null)
    val latestExecution: StateFlow<ActionExecutionEvent?> = _latestExecution.asStateFlow()

    private val _actionHistory = MutableStateFlow<List<ActionExecutionEvent>>(emptyList())
    val actionHistory: StateFlow<List<ActionExecutionEvent>> = _actionHistory.asStateFlow()

    /**
     * Resolves and dispatches an incoming recognized gesture to its configured action handler.
     */
    suspend fun dispatch(gestureEvent: GestureEvent): ActionResult {
        val settings = settingsProvider()

        // 1. Lock Screen Security Gating
        if (gestureEvent.isScreenLocked) {
            val isAllowed = when (gestureEvent.type) {
                GestureType.SinglePress -> settings.lockScreenSettings.singlePressEnabled
                GestureType.DoublePress -> settings.lockScreenSettings.doublePressEnabled
                GestureType.LongPress -> settings.lockScreenSettings.longPressEnabled
            }

            if (!isAllowed) {
                val blockedResult = ActionResult.Unavailable("${gestureEvent.type.displayName} is disabled on lock screen")
                recordExecution(
                    ActionExecutionEvent(
                        gesture = gestureEvent.type,
                        action = RemapAction.None,
                        result = blockedResult,
                        timestamp = System.currentTimeMillis(),
                        isScreenLocked = true
                    )
                )
                AppLogger.w("Action blocked: ${gestureEvent.type.displayName} disabled on lock screen", TAG)
                return blockedResult
            }
        }

        // 2. Resolve mapped RemapAction for this gesture
        val action = when (gestureEvent.type) {
            GestureType.SinglePress -> settings.singlePressAction
            GestureType.DoublePress -> settings.doublePressAction
            GestureType.LongPress -> settings.longPressAction
        }

        // 3. Handle No Action (None)
        if (action is RemapAction.None) {
            val noOpResult = ActionResult.Success("No action configured")
            recordExecution(
                ActionExecutionEvent(
                    gesture = gestureEvent.type,
                    action = action,
                    result = noOpResult,
                    timestamp = System.currentTimeMillis(),
                    isScreenLocked = gestureEvent.isScreenLocked
                )
            )
            AppLogger.d("Gesture ${gestureEvent.type.displayName} -> No Action configured", TAG)
            return noOpResult
        }

        // 4. Delegate to the corresponding ActionHandler
        val handler = resolveHandler(action)
        val result = handler.execute(action, gestureEvent.isScreenLocked)

        // 5. Trigger haptic feedback on success
        if (result is ActionResult.Success && settings.hapticEnabled) {
            hapticsController.vibrate(settings.hapticIntensity)
        }

        // 6. Record diagnostic history
        val executionEvent = ActionExecutionEvent(
            gesture = gestureEvent.type,
            action = action,
            result = result,
            timestamp = System.currentTimeMillis(),
            isScreenLocked = gestureEvent.isScreenLocked
        )
        recordExecution(executionEvent)

        AppLogger.i("Dispatched ${gestureEvent.type.displayName} -> ${action.title}: ${if (result.isSuccess) "SUCCESS" else "FAILURE (${result.message})"}", TAG)
        return result
    }

    private fun resolveHandler(action: RemapAction): ActionHandler {
        return when (action) {
            is RemapAction.Flashlight -> flashlightHandler
            is RemapAction.Camera, is RemapAction.LaunchApp -> appLauncherHandler
            is RemapAction.MediaPlayPause, is RemapAction.MediaNextTrack, is RemapAction.MediaPreviousTrack -> mediaHandler
            is RemapAction.AppShortcut -> shortcutHandler
            is RemapAction.DeepLink -> deepLinkHandler
            is RemapAction.SystemAction -> systemActionHandler
            is RemapAction.RotationLock -> object : ActionHandler {
                override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
                    return ActionResult.Unavailable("Rotation lock toggle requires system write permissions")
                }
            }
            is RemapAction.None -> object : ActionHandler {
                override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
                    return ActionResult.Success("No action")
                }
            }
        }
    }

    private suspend fun recordExecution(event: ActionExecutionEvent) {
        mutex.withLock {
            _latestExecution.value = event
            val current = _actionHistory.value.toMutableList()
            current.add(0, event)
            if (current.size > maxHistorySize) {
                current.removeAt(current.lastIndex)
            }
            _actionHistory.value = current
        }
    }

    fun clearHistory() {
        _actionHistory.value = emptyList()
        _latestExecution.value = null
    }
}
