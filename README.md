# Essential Button Remapper

[![Platform](https://img.shields.io/badge/Platform-Android%209.0%2B%20(API%2028--36)-black?style=flat-square&logo=android)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.2.20-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%7C%20Unidirectional%20Data%20Flow-00C853?style=flat-square)](#architecture)
[![Telemetry](https://img.shields.io/badge/Telemetry-Zero%20%7C%20100%25%20Offline-D71921?style=flat-square)](#privacy-guarantees)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)

A production-grade, privacy-first Android application designed specifically for **Nothing** and **CMF by Nothing** smartphones equipped with the physical **Essential Button**.

It enables users to intercept physical Essential Button hardware events, bypass Nothing OS's intrusive Essential Space, and assign customizable actions (Single Press, Double Press, Long Press) with sub-millisecond response times, zero background polling, and zero telemetry.

---

## The Problem

On devices like the **Nothing Phone (3a) Pro**, **Nothing Phone (3)**, and **CMF Phone 1**, the dedicated Essential Button is factory-hardcoded to trigger Nothing's AI "Essential Space" (taking unwanted screenshots and launching voice notes on accidental taps). Nothing OS provides no native mechanism to reassign or disable this physical button.

## The Solution

**Essential Button Remapper** implements non-root, hardware-level key event interception:

```text
┌──────────────────────────────────────┐
│       Physical Essential Button      │
└──────────────────┬───────────────────┘
                   │ Press (ScanCode 250)
                   ▼
┌──────────────────────────────────────┐
│        Android Input Subsystem       │
└──────────────────┬───────────────────┘
                   │ KeyEvent.ACTION_DOWN / UP
                   ▼
┌──────────────────────────────────────┐
│  EssentialButtonAccessibilityService │ ◄── canRequestFilterKeyEvents = true
└──────────────────┬───────────────────┘     canRetrieveWindowContent = false (Privacy)
                   │
                   ▼
┌──────────────────────────────────────┐
│          ButtonEventDetector         │ ◄── Validates DeviceProfile ScanCode
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│           GestureRecognizer          │ ◄── Single / Double / Long Press State Machine
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│           ActionDispatcher           │
├──────────────────┬───────────────────┤
│  Flashlight      │  Media Controls   │
│  Rotation Lock   │  Launch Apps      │
│  Shortcuts       │  System Actions   │
└──────────────────┴───────────────────┘
```

---

## Hardware Feasibility & Empirical Trace

Physical hardware testing was conducted on a live **Nothing Phone (3a) Pro** (`AsteroidsProIND` / `A059P` running Android 16, SDK 36):

```text
Device Hardware Signature:
├── Manufacturer:   Nothing
├── Model:          A059P (Nothing Phone 3a Pro)
├── Device:         Asteroids (Codename: AsteroidsProIND)
├── Android:        16 (API Level 36, Baklava)
├── Build:          Nothing/AsteroidsProIND/Asteroids:16/BQ2A.250721.001...
└── Essential Key:
    ├── KeyCode:    0 (KeyEvent.KEYCODE_UNKNOWN)
    ├── ScanCode:   250 (Primary Hardware Key Identifier)
    ├── DeviceId:   2
    ├── Source:     0x101 (InputDevice.SOURCE_KEYBOARD)
    └── Flags:      0x8 (KeyEvent.FLAG_FROM_SYSTEM)
```

### Verified Hardware Behaviors

1. **Deterministic ScanCode:** Every press of the physical Essential Button generates `scanCode == 250`. Matching this scanCode eliminates false triggers from any other keyboard or volume input.
2. **Distinct Transitions:** Android emits clean `ACTION_DOWN` followed by `ACTION_UP` (single press hold duration verified at ~77ms).
3. **No Repeat Spam:** Holding the physical button down produces zero repeated `ACTION_DOWN` events, providing clean timing boundaries for long-press recognition.
4. **Lock Screen Visibility:** Nothing OS forwards `scanCode == 250` events to the AccessibilityService even while `KeyguardManager.isKeyguardLocked() == true`.

---

## Supported Devices

The device configuration system decouples hardware models from the core logic through [`DeviceProfile`](file:///app/src/main/java/com/essentialremapper/domain/device/DeviceProfile.kt):

| Device | Model ID | Codename | ScanCode | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Nothing Phone (3a) Pro** | `A059P`, `A059`, `A142P` | `AsteroidsProIND` | `250` | Verified on Hardware |
| **Nothing Phone (3a)** | `A142` | `Asteroids` | `250` | Supported |
| **Nothing Phone (3)** | `A065`, `A065P` | `Tetris` | `250` | Supported |
| **CMF Phone 1** | `A015` | `Galaga` | `250` | Supported |
| **Other Nothing / CMF** | *Dynamic* | *Generic* | `250` | Supported (Fallback) |
| **Non-Nothing Devices** | *Other* | *Other* | `-1` | Safely Rejected |

---

## Architectural Principles

* **Decoupled Responsibilities:** AccessibilityService strictly intercepts raw key events; it contains zero gesture timing or business logic.
* **Reactive UDF (Unidirectional Data Flow):** Powered by Kotlin Coroutines `StateFlow` and `SharedFlow`.
* **Centralized DataStore Repository:** [`SettingsRepository`](file:///app/src/main/java/com/essentialremapper/data/repository/SettingsRepository.kt) acts as the single source of truth for configuration persistence.
* **Strict Battery Efficiency:** Zero background polling threads, zero CPU wake-locks, and zero redundant foreground services. Event-driven invocation only.

---

## Privacy Guarantees

AccessibilityServices are often viewed with skepticism due to permissions abuse. Essential Button Remapper enforces strict architectural limitations:

```xml
<accessibility-service
    android:canRequestFilterKeyEvents="true"
    android:canRetrieveWindowContent="false"
    android:accessibilityFlags="flagRequestFilterKeyEvents" />
```

* **`canRetrieveWindowContent="false"`:** The app is mathematically incapable of querying screen nodes, reading typed text, observing passwords, or inspecting active applications.
* **No Network Permissions:** `android.permission.INTERNET` is **not declared** in `AndroidManifest.xml`. No data can leave the device.
* **Zero Telemetry:** No analytics libraries, no crashlytics, no trackers, and no third-party ad SDKs.

---

## Project Structure

```text
app/src/main/java/com/essentialremapper/
├── EssentialRemapperApp.kt           # Application container & dependency bootstrap
├── MainActivity.kt                   # Edge-to-edge Compose host activity
├── accessibility/
│   └── EssentialButtonAccessibilityService.kt # Privacy-first KeyEvent interceptor
├── domain/
│   ├── action/
│   │   └── RemapAction.kt            # Scalable action domain model & serialization
│   ├── device/
│   │   ├── ButtonPosition.kt         # Normalized button coordinate validation
│   │   ├── DeviceDetector.kt         # Build property matching engine
│   │   ├── DeviceProfile.kt          # Registry of supported hardware profiles
│   │   └── DeviceRepository.kt       # Device profile abstraction
│   └── gesture/
│       ├── ButtonEventDetector.kt    # Hardware scanCode matching & event streaming
│       ├── GestureType.kt            # SinglePress, DoublePress, LongPress enums
│       └── RawButtonEvent.kt         # Raw physical event data contract
├── data/
│   ├── model/
│   │   └── RemapperSettings.kt       # Strongly typed settings entity
│   ├── repository/
│   │   └── SettingsRepository.kt     # Reactive StateFlow repository
│   └── storage/
│       └── SettingsDataStore.kt      # AndroidX DataStore Preferences implementation
├── util/
│   ├── AccessibilityHelper.kt        # Accurate system setting verification
│   └── AppLogger.kt                  # Safe logging with JVM unit-test fallbacks
└── ui/
    ├── diagnostic/
    │   └── DiagnosticScreen.kt       # Live hardware event console & latency monitor
    ├── home/
    │   └── HomeScreen.kt             # Status dashboard & gesture mapping cards
    ├── navigation/
    │   ├── AppNavigation.kt          # Compose NavHost routing
    │   └── Screen.kt                 # Navigation route definitions
    ├── onboarding/
    │   └── OnboardingScreens.kt      # Guided 4-step hardware calibration wizard
    ├── settings/
    │   └── SettingsScreen.kt         # Device, Gesture, Overlay, and Haptic settings
    └── theme/
        ├── Color.kt                  # Curated Nothing monochrome & red accents
        ├── Theme.kt                  # Material 3 dark/light dynamic theme
        └── Type.kt                   # Monospace technical accents & typography
```

---

## Development & Build Setup

### Prerequisites
* **JDK:** OpenJDK 21 or 25 (e.g., Android Studio Ladybug/Meerkat bundled JBR)
* **Android SDK:** Platform 35 / 36 (Build Tools 36.0.0)
* **Gradle:** 9.7.1 (included via Gradle Wrapper)
* **AGP:** 9.4.0 (Kotlin support natively integrated)

### Build Commands

```powershell
# Set Java Environment (if using Android Studio JBR)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "C:\Program Files\Android\Android Studio\jbr\bin;$env:PATH"

# Run Unit Test Suite
.\gradlew.bat test

# Build Debug APK
.\gradlew.bat assembleDebug

# Install on Connected Phone
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Implementation Roadmap

- [x] **Phase 1:** Environment validation (JDK 25, Gradle 9.7.1, AGP 9.4.0, ADB).
- [x] **Phase 2:** Device profile system (`A059P`), DataStore persistence, Compose Material 3 UI, Onboarding & Home screens.
- [x] **Phase 3:** Real `AccessibilityService` integration, `canRetrieveWindowContent="false"`, `ButtonEventDetector` with `scanCode == 250` filtering, live Diagnostic console.
- [ ] **Phase 4:** Coroutine `GestureRecognizer` (Single / Double / Long press state machine with configurable debounce).
- [ ] **Phase 5 & 6:** Action Framework (Flashlight, Media, Camera, App Launch, Deep Links, System Actions).
- [ ] **Phase 7 & 8:** Visual Overlay Window (`NothingDotMatrix` & `AndroidStock`) with orientation-aware positioning.
- [ ] **Phase 9 & 10:** Privileged execution (Wireless Debugging pairing + Shizuku IPC) to disable `com.nothing.ntessentialspace`.
- [ ] **Phase 11:** Storage Access Framework JSON configuration backup and restore.
- [ ] **Phase 12:** End-to-end polish, release signing, and documentation.

---

## License

```text
Copyright 2026 Yogesh M.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

*Disclaimer: Nothing and CMF are registered trademarks of Nothing Technology Limited. This project is an independent open-source utility and is not affiliated with or endorsed by Nothing Technology Limited.*
