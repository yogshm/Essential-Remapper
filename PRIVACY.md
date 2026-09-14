# Privacy & Security Policy

**Essential Button Remapper** is built with an uncompromising commitment to user privacy and device security.

---

## 1. Zero Telemetry & Zero Data Collection

* **No Analytics:** The application contains zero third-party analytics libraries (no Google Analytics, Firebase, Mixpanel, or Flurry).
* **No Crash Reporting Trackers:** No third-party crash trackers (e.g. Sentry, Crashlytics) that harvest system state.
* **No User Identifiers:** No advertising IDs, MAC addresses, device serial numbers, or accounts are recorded or retained.
* **Zero Cloud Synchronization:** All configurations and settings reside strictly on the local device.

---

## 2. Network Isolation

* The application **does not declare** the `android.permission.INTERNET` permission in `AndroidManifest.xml`.
* Android's sandboxing mechanism guarantees that the application cannot establish network sockets, issue HTTP requests, or exfiltrate data off the device.

---

## 3. Accessibility Service Constraints

Android Accessibility Services are granted elevated privileges by the user. To prevent abuse:

1. **`canRetrieveWindowContent="false"`**:
   * The application is configured to strictly deny reading screen nodes or UI trees.
   * It cannot observe on-screen text, view passwords, inspect notifications, or monitor other applications.
2. **Dedicated Scope:**
   * The AccessibilityService is strictly used to receive hardware input events via `onKeyEvent()`.
   * Only events matching the physical Essential Button (`scanCode == 250`) are processed; all other system input is passed through untouched.

---

## 4. Local Storage Security

* User preferences are saved using Android's modern `DataStore` API stored in the application's private sandbox directory (`/data/data/com.essentialremapper/files/datastore`).
* Other non-root applications cannot access this storage.
