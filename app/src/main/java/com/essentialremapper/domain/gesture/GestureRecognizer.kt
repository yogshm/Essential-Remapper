package com.essentialremapper.domain.gesture

import com.essentialremapper.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Explicit finite state machine for Essential Button gesture recognition:
 * - Idle: No button pressed, waiting for interaction.
 * - FirstDown: Physical button is pressed down, long-press timer is active.
 * - FirstUpWaiting: First button click released, waiting for potential second click before timeout.
 * - SecondDown: Second button click pressed within double-press window.
 * - LongPressFired: Button was held past threshold, LongPress event emitted, waiting for physical release.
 */
sealed class GestureState {
    data object Idle : GestureState()
    data class FirstDown(val downTime: Long, val isScreenLocked: Boolean) : GestureState()
    data class FirstUpWaiting(val upTime: Long, val isScreenLocked: Boolean) : GestureState()
    data class SecondDown(val downTime: Long, val isScreenLocked: Boolean) : GestureState()
    data class LongPressFired(val downTime: Long) : GestureState()
}

/**
 * Converts raw physical button events into high-level gestures:
 * - Single Press
 * - Double Press
 * - Long Press
 *
 * Uses an explicit finite state machine with structured coroutines,
 * mutex-guaranteed serial transitions, and configurable timings.
 */
class GestureRecognizer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    var doublePressTimeoutMs: Long = 350L,
    var longPressDurationMs: Long = 700L,
    private val maxHistorySize: Int = 50,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    var currentState: GestureState = GestureState.Idle
        private set

    private var longPressJob: Job? = null
    private var doublePressTimeoutJob: Job? = null

    private val _gestureEvents = MutableSharedFlow<GestureEvent>(extraBufferCapacity = 64)
    val gestureEvents: SharedFlow<GestureEvent> = _gestureEvents.asSharedFlow()

    private val _latestGesture = MutableStateFlow<GestureEvent?>(null)
    val latestGesture: StateFlow<GestureEvent?> = _latestGesture.asStateFlow()

    private val _diagnosticGestureHistory = MutableStateFlow<List<GestureEvent>>(emptyList())
    val diagnosticGestureHistory: StateFlow<List<GestureEvent>> = _diagnosticGestureHistory.asStateFlow()

    fun updateTimings(doublePressTimeout: Long, longPressDuration: Long) {
        this.doublePressTimeoutMs = doublePressTimeout.coerceIn(200L, 600L)
        this.longPressDurationMs = longPressDuration.coerceIn(400L, 1200L)
    }

    /**
     * Primary entry point for incoming hardware button events from ButtonEventDetector.
     */
    fun onButtonEvent(event: RawButtonEvent): Job {
        return scope.launch(dispatcher) {
            processEvent(event)
        }
    }

    /**
     * Synchronous state machine execution inside coroutine context.
     * Directly accessible for deterministic testing with virtual clocks.
     */
    suspend fun processEvent(event: RawButtonEvent) {
        mutex.withLock {
            when (event.action) {
                RawButtonAction.DOWN -> handleDown(event)
                RawButtonAction.UP -> handleUp(event)
            }
        }
    }

    private fun handleDown(event: RawButtonEvent) {
        when (val state = currentState) {
            is GestureState.Idle -> {
                // First click initiated
                currentState = GestureState.FirstDown(
                    downTime = event.timestamp,
                    isScreenLocked = event.isScreenLocked
                )

                // Start long press timer
                longPressJob?.cancel()
                longPressJob = scope.launch(dispatcher) {
                    delay(longPressDurationMs)
                    mutex.withLock {
                        if (currentState is GestureState.FirstDown) {
                            val downState = currentState as GestureState.FirstDown
                            currentState = GestureState.LongPressFired(downState.downTime)
                            emitGesture(
                                GestureEvent(
                                    type = GestureType.LongPress,
                                    timestamp = timeProvider(),
                                    isScreenLocked = downState.isScreenLocked
                                )
                            )
                        }
                    }
                }
            }

            is GestureState.FirstDown -> {
                // Hardware driver repeat/bounce while button is held: ignore
                AppLogger.d("Repeated DOWN in FirstDown ignored")
            }

            is GestureState.FirstUpWaiting -> {
                // Second click arrived within double-press timeout window!
                doublePressTimeoutJob?.cancel()
                doublePressTimeoutJob = null

                currentState = GestureState.SecondDown(
                    downTime = event.timestamp,
                    isScreenLocked = state.isScreenLocked || event.isScreenLocked
                )
            }

            is GestureState.SecondDown -> {
                // Hardware driver repeat during second press: ignore
                AppLogger.d("Repeated DOWN in SecondDown ignored")
            }

            is GestureState.LongPressFired -> {
                // Button is still held after long press already triggered: ignore
                AppLogger.d("Repeated DOWN in LongPressFired ignored")
            }
        }
    }

    private fun handleUp(event: RawButtonEvent) {
        when (val state = currentState) {
            is GestureState.Idle -> {
                // Spurious UP without preceding DOWN: ignore safely
                AppLogger.d("Spurious UP in Idle ignored")
            }

            is GestureState.FirstDown -> {
                // First click released before long press duration
                longPressJob?.cancel()
                longPressJob = null

                currentState = GestureState.FirstUpWaiting(
                    upTime = event.timestamp,
                    isScreenLocked = state.isScreenLocked
                )

                // Start double-press timeout window
                doublePressTimeoutJob?.cancel()
                doublePressTimeoutJob = scope.launch(dispatcher) {
                    delay(doublePressTimeoutMs)
                    mutex.withLock {
                        if (currentState is GestureState.FirstUpWaiting) {
                            val waitingState = currentState as GestureState.FirstUpWaiting
                            currentState = GestureState.Idle
                            emitGesture(
                                GestureEvent(
                                    type = GestureType.SinglePress,
                                    timestamp = timeProvider(),
                                    isScreenLocked = waitingState.isScreenLocked
                                )
                            )
                        }
                    }
                }
            }

            is GestureState.FirstUpWaiting -> {
                // Spurious second UP: ignore
                AppLogger.d("Spurious UP in FirstUpWaiting ignored")
            }

            is GestureState.SecondDown -> {
                // Second click released: Double Press complete!
                currentState = GestureState.Idle
                emitGesture(
                    GestureEvent(
                        type = GestureType.DoublePress,
                        timestamp = timeProvider(),
                        isScreenLocked = state.isScreenLocked
                    )
                )
            }

            is GestureState.LongPressFired -> {
                // Physical button released after long press already triggered
                currentState = GestureState.Idle
                // Do NOT emit single press or any other gesture
                AppLogger.d("Button released after LongPressFired -> reset to Idle")
            }
        }
    }

    /**
     * Resets gesture state and cancels active timers.
     */
    fun reset() {
        longPressJob?.cancel()
        longPressJob = null
        doublePressTimeoutJob?.cancel()
        doublePressTimeoutJob = null
        currentState = GestureState.Idle
        AppLogger.d("GestureRecognizer cleanly reset to Idle")
    }

    /**
     * Complete teardown of active jobs and scopes.
     */
    fun destroy() {
        reset()
        scope.coroutineContext[Job]?.cancelChildren()
    }

    private fun emitGesture(event: GestureEvent) {
        AppLogger.i("Gesture recognized: ${event.type.displayName} (locked=${event.isScreenLocked})")
        _gestureEvents.tryEmit(event)
        _latestGesture.value = event

        val currentList = _diagnosticGestureHistory.value.toMutableList()
        currentList.add(0, event)
        if (currentList.size > maxHistorySize) {
            currentList.removeAt(currentList.lastIndex)
        }
        _diagnosticGestureHistory.value = currentList
    }

    fun clearDiagnosticHistory() {
        _diagnosticGestureHistory.value = emptyList()
        _latestGesture.value = null
    }
}
