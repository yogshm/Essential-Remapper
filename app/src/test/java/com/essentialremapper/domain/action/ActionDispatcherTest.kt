package com.essentialremapper.domain.action

import com.essentialremapper.data.model.HapticIntensity
import com.essentialremapper.data.model.LockScreenSettings
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.action.handlers.DeepLinkHandler
import com.essentialremapper.domain.gesture.GestureEvent
import com.essentialremapper.domain.gesture.GestureType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionDispatcherTest {

    private class FakeActionHandler(
        var resultToReturn: ActionResult = ActionResult.Success("Default success")
    ) : ActionHandler {
        val executedActions = mutableListOf<Pair<RemapAction, Boolean>>()

        override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
            executedActions.add(action to isScreenLocked)
            return resultToReturn
        }
    }

    private class FakeHapticsController : HapticsController() {
        var vibrationCount = 0
        var lastIntensity: HapticIntensity? = null

        override fun vibrate(intensity: HapticIntensity) {
            vibrationCount++
            lastIntensity = intensity
        }
    }

    private lateinit var fakeFlashlightHandler: FakeActionHandler
    private lateinit var fakeMediaHandler: FakeActionHandler
    private lateinit var fakeAppLauncherHandler: FakeActionHandler
    private lateinit var fakeShortcutHandler: FakeActionHandler
    private lateinit var fakeDeepLinkHandler: FakeActionHandler
    private lateinit var fakeSystemActionHandler: FakeActionHandler
    private lateinit var fakeHapticsController: FakeHapticsController

    private var currentSettings = RemapperSettings()

    @Before
    fun setUp() {
        fakeFlashlightHandler = FakeActionHandler()
        fakeMediaHandler = FakeActionHandler()
        fakeAppLauncherHandler = FakeActionHandler()
        fakeShortcutHandler = FakeActionHandler()
        fakeDeepLinkHandler = FakeActionHandler()
        fakeSystemActionHandler = FakeActionHandler()
        fakeHapticsController = FakeHapticsController()

        currentSettings = RemapperSettings(
            singlePressAction = RemapAction.Flashlight,
            doublePressAction = RemapAction.Camera,
            longPressAction = RemapAction.MediaNextTrack,
            lockScreenSettings = LockScreenSettings(
                singlePressEnabled = false,
                doublePressEnabled = true,
                longPressEnabled = false
            ),
            hapticEnabled = true,
            hapticIntensity = HapticIntensity.MEDIUM
        )
    }

    private fun createDispatcher(): ActionDispatcher {
        return ActionDispatcher(
            settingsProvider = { currentSettings },
            hapticsController = fakeHapticsController,
            flashlightHandler = fakeFlashlightHandler,
            mediaHandler = fakeMediaHandler,
            appLauncherHandler = fakeAppLauncherHandler,
            shortcutHandler = fakeShortcutHandler,
            deepLinkHandler = fakeDeepLinkHandler,
            systemActionHandler = fakeSystemActionHandler
        )
    }

    @Test
    fun test1_singlePressConfiguredAction() = runTest {
        val dispatcher = createDispatcher()
        val event = GestureEvent(type = GestureType.SinglePress, isScreenLocked = false)

        val result = dispatcher.dispatch(event)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeFlashlightHandler.executedActions.size)
        assertEquals(RemapAction.Flashlight, fakeFlashlightHandler.executedActions.first().first)
        assertFalse("Should be unlocked", fakeFlashlightHandler.executedActions.first().second)
        assertEquals(1, dispatcher.actionHistory.value.size)
        assertEquals(1, fakeHapticsController.vibrationCount)
        assertEquals(HapticIntensity.MEDIUM, fakeHapticsController.lastIntensity)
    }

    @Test
    fun test2_doublePressConfiguredAction() = runTest {
        val dispatcher = createDispatcher()
        val event = GestureEvent(type = GestureType.DoublePress, isScreenLocked = false)

        val result = dispatcher.dispatch(event)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeAppLauncherHandler.executedActions.size)
        assertEquals(RemapAction.Camera, fakeAppLauncherHandler.executedActions.first().first)
    }

    @Test
    fun test3_longPressConfiguredAction() = runTest {
        val dispatcher = createDispatcher()
        val event = GestureEvent(type = GestureType.LongPress, isScreenLocked = false)

        val result = dispatcher.dispatch(event)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeMediaHandler.executedActions.size)
        assertEquals(RemapAction.MediaNextTrack, fakeMediaHandler.executedActions.first().first)
    }

    @Test
    fun test4_lockedAndDisabledGestureBlocked() = runTest {
        val dispatcher = createDispatcher()
        // SinglePress is disabled on lock screen in settings
        val event = GestureEvent(type = GestureType.SinglePress, isScreenLocked = true)

        val result = dispatcher.dispatch(event)

        assertFalse("Must not succeed when gesture is disabled on lock screen", result.isSuccess)
        assertTrue("Result must be Unavailable", result is ActionResult.Unavailable)
        assertTrue(result.message.contains("disabled on lock screen"))
        assertTrue("Flashlight handler must not have executed", fakeFlashlightHandler.executedActions.isEmpty())

        // Verify diagnostic history records the blocked interaction
        assertEquals(1, dispatcher.actionHistory.value.size)
        val record = dispatcher.actionHistory.value.first()
        assertTrue(record.isScreenLocked)
        assertTrue(record.result is ActionResult.Unavailable)
        assertEquals(0, fakeHapticsController.vibrationCount)
    }

    @Test
    fun test5_lockedAndEnabledGestureDispatched() = runTest {
        val dispatcher = createDispatcher()
        // DoublePress is enabled on lock screen in settings
        val event = GestureEvent(type = GestureType.DoublePress, isScreenLocked = true)

        val result = dispatcher.dispatch(event)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeAppLauncherHandler.executedActions.size)
        val (action, isLocked) = fakeAppLauncherHandler.executedActions.first()
        assertEquals(RemapAction.Camera, action)
        assertTrue("isScreenLocked must be passed through to handler", isLocked)
    }

    @Test
    fun test6_noneActionNoOperation() = runTest {
        currentSettings = currentSettings.copy(singlePressAction = RemapAction.None)
        val dispatcher = createDispatcher()
        val event = GestureEvent(type = GestureType.SinglePress, isScreenLocked = false)

        val result = dispatcher.dispatch(event)

        assertTrue(result.isSuccess)
        assertEquals("No action configured", result.message)
        assertTrue(fakeFlashlightHandler.executedActions.isEmpty())
        assertTrue(fakeMediaHandler.executedActions.isEmpty())
        assertEquals(1, dispatcher.actionHistory.value.size)
    }

    @Test
    fun test7_actionHandlerFailureHandledGracefully() = runTest {
        fakeFlashlightHandler.resultToReturn = ActionResult.Failure("Camera hardware busy")
        val dispatcher = createDispatcher()
        val event = GestureEvent(type = GestureType.SinglePress, isScreenLocked = false)

        val result = dispatcher.dispatch(event)

        assertFalse(result.isSuccess)
        assertTrue(result is ActionResult.Failure)
        assertEquals("Camera hardware busy", result.message)

        val latest = dispatcher.latestExecution.value
        assertNotNull(latest)
        assertFalse(latest!!.result.isSuccess)
        assertEquals(0, fakeHapticsController.vibrationCount)
    }

    @Test
    fun test8_missingApplicationFailure() = runTest {
        val missingAppAction = RemapAction.LaunchApp("com.uninstalled.app", "MissingApp")
        currentSettings = currentSettings.copy(doublePressAction = missingAppAction)
        fakeAppLauncherHandler.resultToReturn = ActionResult.Failure("App 'MissingApp' is not installed")

        val dispatcher = createDispatcher()
        val event = GestureEvent(type = GestureType.DoublePress, isScreenLocked = false)

        val result = dispatcher.dispatch(event)

        assertFalse(result.isSuccess)
        assertTrue(result.message.contains("not installed"))
        assertEquals(missingAppAction, fakeAppLauncherHandler.executedActions.first().first)
    }

    @Test
    fun test9_invalidDeepLinkSchemeRejected() = runTest {
        val deepLinkHandler = DeepLinkHandler()

        // Allowed schemes
        assertEquals("https", deepLinkHandler.extractScheme("https://nothing.tech"))
        assertEquals("http", deepLinkHandler.extractScheme("http://example.com"))
        assertEquals("tel", deepLinkHandler.extractScheme("tel:+123456789"))
        assertEquals("mailto", deepLinkHandler.extractScheme("mailto:support@nothing.tech"))
        assertEquals("content", deepLinkHandler.extractScheme("content://media/external/images"))

        // Disallowed schemes
        val disallowedSchemes = listOf(
            "javascript:alert(1)",
            "file:///data/system/users/0",
            "sh://command",
            "intent:#Intent;action=android.intent.action.VIEW;end"
        )

        for (uri in disallowedSchemes) {
            val result = deepLinkHandler.execute(RemapAction.DeepLink(uri), isScreenLocked = false)
            assertFalse("Disallowed scheme URI should be rejected: $uri", result.isSuccess)
            assertTrue(result is ActionResult.Failure)
        }
    }

    @Test
    fun test10_multipleGesturesUseIndependentMappings() = runTest {
        val dispatcher = createDispatcher()

        // 1. Single Press -> Flashlight
        val res1 = dispatcher.dispatch(GestureEvent(type = GestureType.SinglePress, isScreenLocked = false))
        assertTrue(res1.isSuccess)
        assertEquals(1, fakeFlashlightHandler.executedActions.size)

        // 2. Double Press -> Camera
        val res2 = dispatcher.dispatch(GestureEvent(type = GestureType.DoublePress, isScreenLocked = false))
        assertTrue(res2.isSuccess)
        assertEquals(1, fakeAppLauncherHandler.executedActions.size)

        // 3. Long Press -> Next Track
        val res3 = dispatcher.dispatch(GestureEvent(type = GestureType.LongPress, isScreenLocked = false))
        assertTrue(res3.isSuccess)
        assertEquals(1, fakeMediaHandler.executedActions.size)

        // History contains all 3 distinct executions
        assertEquals(3, dispatcher.actionHistory.value.size)
        assertEquals(GestureType.LongPress, dispatcher.actionHistory.value[0].gesture)
        assertEquals(GestureType.DoublePress, dispatcher.actionHistory.value[1].gesture)
        assertEquals(GestureType.SinglePress, dispatcher.actionHistory.value[2].gesture)
    }
}
