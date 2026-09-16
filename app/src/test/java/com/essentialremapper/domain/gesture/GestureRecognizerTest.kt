package com.essentialremapper.domain.gesture

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GestureRecognizerTest {

    private fun createRawEvent(
        action: RawButtonAction,
        timestamp: Long = 1000L,
        isScreenLocked: Boolean = false
    ): RawButtonEvent {
        return RawButtonEvent(
            action = action,
            timestamp = timestamp,
            keyCode = 0,
            scanCode = 250,
            deviceId = 2,
            source = 0x101,
            flags = 0x8,
            repeatCount = 0,
            isScreenLocked = isScreenLocked
        )
    }

    @Test
    fun test1_singlePress() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // DOWN -> UP
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        assertEquals(GestureState.FirstDown::class, recognizer.currentState::class)

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 177L))
        runCurrent()
        assertEquals(GestureState.FirstUpWaiting::class, recognizer.currentState::class)

        // Advance past doublePressTimeoutMs (350ms)
        advanceTimeBy(350L)
        runCurrent()

        assertEquals(GestureState.Idle, recognizer.currentState)
        val latest = recognizer.latestGesture.value
        assertNotNull("Latest gesture should not be null", latest)
        assertEquals(GestureType.SinglePress, latest!!.type)
        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)
    }

    @Test
    fun test2_doublePress() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // First click: DOWN -> UP
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        // Second click within 100ms: DOWN -> UP
        advanceTimeBy(100L)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 250L))
        runCurrent()
        assertEquals(GestureState.SecondDown::class, recognizer.currentState::class)

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 300L))
        runCurrent()

        // Wait to verify no delayed SinglePress is emitted
        advanceTimeBy(500L)
        runCurrent()

        assertEquals(GestureState.Idle, recognizer.currentState)
        val history = recognizer.diagnosticGestureHistory.value
        assertEquals(1, history.size)
        assertEquals(GestureType.DoublePress, history.first().type)
        assertEquals(GestureType.DoublePress, recognizer.latestGesture.value?.type)
    }

    @Test
    fun test3_longPress() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // DOWN and hold
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()

        // Advance to long press duration (700ms)
        advanceTimeBy(700L)
        runCurrent()

        assertEquals(GestureState.LongPressFired::class, recognizer.currentState::class)
        assertEquals(GestureType.LongPress, recognizer.latestGesture.value?.type)

        // Physical release UP after long press has fired
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 900L))
        runCurrent()

        advanceTimeBy(500L)
        runCurrent()

        assertEquals(GestureState.Idle, recognizer.currentState)
        val history = recognizer.diagnosticGestureHistory.value
        assertEquals("Must produce exactly 1 LongPress event and no SinglePress", 1, history.size)
        assertEquals(GestureType.LongPress, history.first().type)
    }

    @Test
    fun test4_singlePressTimeout() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        // Advance just before timeout (340ms)
        advanceTimeBy(340L)
        runCurrent()
        assertNull("SinglePress should not fire before 350ms timeout", recognizer.latestGesture.value)

        // Advance remaining time
        advanceTimeBy(15L)
        runCurrent()
        assertEquals(GestureType.SinglePress, recognizer.latestGesture.value?.type)
    }

    @Test
    fun test5_doublePressInsideTimeout() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // DOWN -> UP
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        // 250ms later (inside 350ms timeout window)
        advanceTimeBy(250L)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 400L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 450L))
        runCurrent()

        advanceTimeBy(500L)
        runCurrent()

        val history = recognizer.diagnosticGestureHistory.value
        assertEquals("Should only emit 1 DoublePress and zero SinglePress", 1, history.size)
        assertEquals(GestureType.DoublePress, history.first().type)
    }

    @Test
    fun test6_secondPressAfterTimeout() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // First click
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        // Wait past 350ms -> First SinglePress fires
        advanceTimeBy(360L)
        runCurrent()
        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)
        assertEquals(GestureType.SinglePress, recognizer.diagnosticGestureHistory.value.first().type)

        // Second click after timeout elapsed
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 600L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 650L))
        runCurrent()

        advanceTimeBy(360L)
        runCurrent()

        val history = recognizer.diagnosticGestureHistory.value
        assertEquals("Must produce two independent SinglePress gestures", 2, history.size)
        assertEquals(GestureType.SinglePress, history[0].type)
        assertEquals(GestureType.SinglePress, history[1].type)
    }

    @Test
    fun test7_longPressDoesNotEmitSinglePress() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()

        advanceTimeBy(700L)
        runCurrent()
        assertEquals(GestureType.LongPress, recognizer.latestGesture.value?.type)

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 1000L))
        runCurrent()

        advanceTimeBy(1000L)
        runCurrent()

        val history = recognizer.diagnosticGestureHistory.value
        assertEquals(1, history.size)
        assertEquals(GestureType.LongPress, history.first().type)
    }

    @Test
    fun test8_longPressEmitsExactlyOnce() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()

        // Wait 700ms -> LongPress
        advanceTimeBy(700L)
        runCurrent()
        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)

        // Hold for another 1500ms
        advanceTimeBy(1500L)
        runCurrent()
        assertEquals("LongPress must only emit once while held", 1, recognizer.diagnosticGestureHistory.value.size)

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 2400L))
        runCurrent()
        advanceTimeBy(500L)
        runCurrent()

        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)
    }

    @Test
    fun test9_rapidPresses() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // Rapid click 1: DOWN (10ms) -> UP (20ms)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 10L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 20L))
        runCurrent()

        // Rapid click 2: DOWN (30ms) -> UP (40ms)
        advanceTimeBy(10L)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 30L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 40L))
        runCurrent()

        // DoublePress emitted
        assertEquals(GestureType.DoublePress, recognizer.latestGesture.value?.type)
        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)

        // Immediately followed by a single press: DOWN (100ms) -> UP (150ms)
        advanceTimeBy(60L)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        advanceTimeBy(350L)
        runCurrent()

        val history = recognizer.diagnosticGestureHistory.value
        assertEquals(2, history.size)
        assertEquals(GestureType.SinglePress, history[0].type)
        assertEquals(GestureType.DoublePress, history[1].type)
    }

    @Test
    fun test10_invalidUp() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher
        )

        // Spurious UP while Idle
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 100L))
        runCurrent()
        advanceTimeBy(1000L)
        runCurrent()

        assertEquals(GestureState.Idle, recognizer.currentState)
        assertNull(recognizer.latestGesture.value)
        assertTrue(recognizer.diagnosticGestureHistory.value.isEmpty())

        // Regular click followed by spurious UP while waiting
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 200L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 250L))
        runCurrent()

        // Spurious second UP
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 260L))
        runCurrent()

        advanceTimeBy(350L)
        runCurrent()

        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)
        assertEquals(GestureType.SinglePress, recognizer.diagnosticGestureHistory.value.first().type)
    }

    @Test
    fun test11_repeatedDown() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            longPressDurationMs = 700L
        )

        // First DOWN
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()

        // Repeated DOWN from driver repeat
        advanceTimeBy(50L)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 150L))
        runCurrent()

        // Advance to long press duration
        advanceTimeBy(650L)
        runCurrent()

        assertEquals(GestureType.LongPress, recognizer.latestGesture.value?.type)
        assertEquals(1, recognizer.diagnosticGestureHistory.value.size)
    }

    @Test
    fun test12_resetAfterInterruption() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L
        )

        // Start a single press (waiting in FirstUpWaiting)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        assertEquals(GestureState.FirstUpWaiting::class, recognizer.currentState::class)

        // Service interrupted / reset
        recognizer.reset()
        assertEquals(GestureState.Idle, recognizer.currentState)

        // Advance past original timeout
        advanceTimeBy(500L)
        runCurrent()

        assertNull("Cancelled timer must not emit gesture after reset", recognizer.latestGesture.value)
        assertTrue(recognizer.diagnosticGestureHistory.value.isEmpty())
    }

    @Test
    fun test13_customDoublePressTimeout() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            doublePressTimeoutMs = 350L,
            longPressDurationMs = 700L
        )

        // Set custom 500ms timeout
        recognizer.updateTimings(doublePressTimeout = 500L, longPressDuration = 700L)
        assertEquals(500L, recognizer.doublePressTimeoutMs)

        // First click
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 150L))
        runCurrent()

        // Advance 420ms (would have expired under 350ms, but valid under 500ms!)
        advanceTimeBy(420L)
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 570L))
        runCurrent()
        recognizer.onButtonEvent(createRawEvent(RawButtonAction.UP, timestamp = 620L))
        runCurrent()

        advanceTimeBy(600L)
        runCurrent()

        val history = recognizer.diagnosticGestureHistory.value
        assertEquals(1, history.size)
        assertEquals(GestureType.DoublePress, history.first().type)
    }

    @Test
    fun test14_customLongPressDuration() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher,
            longPressDurationMs = 700L
        )

        // Set custom 1000ms long press duration
        recognizer.updateTimings(doublePressTimeout = 350L, longPressDuration = 1000L)
        assertEquals(1000L, recognizer.longPressDurationMs)

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()

        // Advance 700ms (default duration, should NOT trigger yet)
        advanceTimeBy(700L)
        runCurrent()
        assertNull("Should not trigger long press at 700ms when threshold is 1000ms", recognizer.latestGesture.value)

        // Advance remaining 300ms
        advanceTimeBy(300L)
        runCurrent()
        assertEquals(GestureType.LongPress, recognizer.latestGesture.value?.type)
    }

    @Test
    fun test15_coroutineCancellation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val recognizer = GestureRecognizer(
            scope = this,
            dispatcher = testDispatcher
        )

        recognizer.onButtonEvent(createRawEvent(RawButtonAction.DOWN, timestamp = 100L))
        runCurrent()

        recognizer.destroy()
        assertEquals(GestureState.Idle, recognizer.currentState)

        advanceTimeBy(1000L)
        runCurrent()

        assertNull(recognizer.latestGesture.value)
    }
}
