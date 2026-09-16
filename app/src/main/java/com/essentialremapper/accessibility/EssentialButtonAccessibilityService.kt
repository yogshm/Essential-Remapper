package com.essentialremapper.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.essentialremapper.EssentialRemapperApp
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.action.ActionDispatcher
import com.essentialremapper.domain.action.HapticsController
import com.essentialremapper.domain.action.handlers.SystemActionHandler
import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.domain.gesture.ButtonEventDetector
import com.essentialremapper.domain.gesture.GestureRecognizer
import com.essentialremapper.domain.gesture.RawButtonAction
import com.essentialremapper.domain.gesture.RawButtonEvent
import com.essentialremapper.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Privacy-focused AccessibilityService dedicated strictly to detecting physical Essential Button events
 * and dispatching configured user actions.
 *
 * Privacy Guarantees:
 * - canRetrieveWindowContent is FALSE
 * - No accessibility tree traversal
 * - No screen node inspection or reading
 * - Zero user data / screen logging
 * - Zero analytics or network communication
 */
class EssentialButtonAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "EssentialButtonService"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        val buttonEventDetector = ButtonEventDetector(maxHistorySize = 50)
        val gestureRecognizer = GestureRecognizer()

        var activeServiceInstance: EssentialButtonAccessibilityService? = null
            private set

        val actionDispatcher: ActionDispatcher by lazy {
            val app = EssentialRemapperApp.instance
            ActionDispatcher(
                context = app,
                settingsProvider = { app.settingsRepository.settings.value },
                hapticsController = HapticsController(app),
                systemActionHandler = SystemActionHandler { activeServiceInstance }
            )
        }
    }

    private var serviceScope: CoroutineScope? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeServiceInstance = this
        _isServiceConnected.value = true
        AppLogger.i("EssentialButtonAccessibilityService CONNECTED", TAG)

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        serviceScope = scope

        // Synchronize gesture timings dynamically with settings
        val app = applicationContext as? EssentialRemapperApp
        val settingsRepo = app?.settingsRepository
        if (settingsRepo != null) {
            scope.launch {
                settingsRepo.settings.collect { settings ->
                    gestureRecognizer.updateTimings(
                        doublePressTimeout = settings.doublePressTimeoutMs,
                        longPressDuration = settings.longPressDurationMs
                    )
                    AppLogger.d("GestureRecognizer timings updated: doublePress=${settings.doublePressTimeoutMs}ms, longPress=${settings.longPressDurationMs}ms", TAG)
                }
            }
        }

        // Connect GestureRecognizer -> ActionDispatcher
        scope.launch {
            gestureRecognizer.gestureEvents.collect { gestureEvent ->
                AppLogger.d("Forwarding recognized gesture ${gestureEvent.type.displayName} to ActionDispatcher", TAG)
                actionDispatcher.dispatch(gestureEvent)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Explicitly ignored: No screen or window content is processed
    }

    override fun onInterrupt() {
        AppLogger.w("EssentialButtonAccessibilityService INTERRUPTED", TAG)
        _isServiceConnected.value = false
        gestureRecognizer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeServiceInstance = null
        _isServiceConnected.value = false
        serviceScope?.cancel()
        serviceScope = null
        gestureRecognizer.reset()
        AppLogger.i("EssentialButtonAccessibilityService DESTROYED", TAG)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Only process DOWN and UP actions
        val rawAction = when (event.action) {
            KeyEvent.ACTION_DOWN -> RawButtonAction.DOWN
            KeyEvent.ACTION_UP -> RawButtonAction.UP
            else -> return super.onKeyEvent(event)
        }

        // Obtain active device profile from application state
        val activeProfile = resolveActiveProfile()

        // Match against device profile hardware scanCode
        val isMatch = buttonEventDetector.matches(
            keyCode = event.keyCode,
            scanCode = event.scanCode,
            activeProfile = activeProfile
        )

        if (isMatch) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val isLocked = keyguardManager?.isKeyguardLocked ?: false

            val rawButtonEvent = RawButtonEvent(
                action = rawAction,
                timestamp = if (event.eventTime > 0) event.eventTime else System.currentTimeMillis(),
                keyCode = event.keyCode,
                scanCode = event.scanCode,
                deviceId = event.deviceId,
                source = event.source,
                flags = event.flags,
                repeatCount = event.repeatCount,
                isScreenLocked = isLocked
            )

            AppLogger.d("Matched physical button event: ${rawButtonEvent.toDiagnosticString()}", TAG)
            buttonEventDetector.onButtonEvent(rawButtonEvent)
            gestureRecognizer.onButtonEvent(rawButtonEvent)

            // In Phase 5, pass through to avoid breaking Essential Space until Phase 6+
            return super.onKeyEvent(event)
        }

        return super.onKeyEvent(event)
    }

    private fun resolveActiveProfile(): DeviceProfile {
        return try {
            val app = applicationContext as? EssentialRemapperApp
            val selectedId = app?.settingsRepository?.settings?.value?.selectedDeviceId
            if (selectedId != null) {
                app.deviceRepository.getProfileById(selectedId)
            } else {
                DeviceProfile.NOTHING_PHONE_3A_PRO
            }
        } catch (_: Exception) {
            DeviceProfile.NOTHING_PHONE_3A_PRO
        }
    }
}
