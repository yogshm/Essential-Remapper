# Architecture Documentation: Essential Button Remapper

This document details the architectural topology, threading models, state boundaries, and data flow of **Essential Button Remapper**.

---

## High-Level Topology

```text
┌─────────────────────────────────────────────────────────────┐
│                    Physical Hardware Layer                  │
│ Nothing Phone (3a) Pro Physical Button                      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Pressed (ScanCode 250)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Linux Kernel / Driver                   │
│ /dev/input/event* (Keyboard Input Device 2)                 │
└──────────────────────────────┬──────────────────────────────┘
                               │ Raw EV_KEY
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   Android InputManagerService               │
│ Generates android.view.KeyEvent (DOWN / UP, KeyCode 0)      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Intercepted via Filter Flag
                               ▼
┌─────────────────────────────────────────────────────────────┐
│        EssentialButtonAccessibilityService                  │
│ - canRequestFilterKeyEvents = true                          │
│ - canRetrieveWindowContent = false                          │
└──────────────────────────────┬──────────────────────────────┘
                               │ onKeyEvent()
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     ButtonEventDetector                     │
│ Validates ScanCode == ActiveProfile.ScanCode (250)          │
│ Dispatches RawButtonEvent via MutableSharedFlow             │
└──────────────────────────────┬──────────────────────────────┘
                               │ Emitted Event Stream
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  GestureRecognizer (Phase 4)                │
│ State Machine: SinglePress, DoublePress, LongPress          │
└──────────────────────────────┬──────────────────────────────┘
                               │ Gesture Event Triggered
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 ActionDispatcher & ActionHandlers           │
│ Executes RemapAction (Flashlight, Media, LaunchApp, etc.)   │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Layers

### 1. Hardware & Driver Abstraction (`domain/device/`)
* **`DeviceProfile`**: Immutable record defining keycodes, scancodes, display names, and physical button coordinates for portrait and landscape modes.
* **`DeviceDetector`**: Pure matching engine comparing Android runtime system properties (`Build.MODEL`, `Build.PRODUCT`, `Build.DEVICE`, `Build.MANUFACTURER`) to detect the specific device without mock dependencies.
* **`DeviceRepository`**: Catalog of supported profiles, providing the active profile to consumers.

### 2. Physical Key Interception (`accessibility/`)
* **`EssentialButtonAccessibilityService`**:
  * Inherits from Android's `AccessibilityService`.
  * Configured with `flagRequestFilterKeyEvents` to intercept hardware keys before they reach focused application windows.
  * Explicitly configured with `canRetrieveWindowContent="false"` to prevent screen node inspection and maximize privacy.
  * Dispatches raw events to `ButtonEventDetector`.

### 3. Gesture Detection Engine (`domain/gesture/`)
* **`RawButtonEvent`**: Data model representing transition events (`DOWN`, `UP`), physical timestamps, device IDs, and keyguard lock state.
* **`ButtonEventDetector`**: Filters incoming key events by comparing `scanCode` and `keyCode` against the active device profile. Distributes valid events to downstream consumers.

### 4. Reactive State & Persistence Layer (`data/`)
* **Unidirectional Data Flow (UDF)**:
  * Application state is stored in AndroidX `DataStore<Preferences>`.
  * `SettingsRepository` exposes a single reactive `StateFlow<RemapperSettings>` across the entire application lifecycle.
  * Mutative operations use suspend functions (`updateSettings`, `setAction`, `setGestureTimings`) executing within a dedicated `CoroutineScope(Dispatchers.IO + SupervisorJob())`.

### 5. UI Layer (`ui/`)
* **Declarative Jetpack Compose (Material 3)**:
  * Composable screens consume state directly from `SettingsRepository.settings.collectAsState()`.
  * Navigation is managed by a top-level `NavHost` connecting `Onboarding`, `Home`, `Settings`, and `Diagnostic` screens.
  * Strict separation between presentation and business logic.
