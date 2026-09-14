# Troubleshooting Guide: Essential Button Remapper

This guide covers common issues, Nothing OS firmware specifics, and diagnostic steps.

---

## 1. Accessibility Service Disconnection

### Symptoms
* Home screen shows `○ Accessibility disabled`.
* Button clicks do not appear on the Diagnostic screen.

### Root Cause
* Android OS or aggressive battery optimization may kill background accessibility services after prolonged inactivity or system updates.

### Solution
1. Tap the **Enable** button on the Home screen to open Android Accessibility settings.
2. Ensure **Essential Button Remapper** is toggled **ON**.
3. **Disable Battery Optimization**:
   * Navigate to *Settings > Apps > Essential Button Remapper > Battery*.
   * Set battery usage to **Unrestricted**.

---

## 2. Button Press Takes Screenshots (Essential Space Active)

### Symptoms
* Pressing the Essential Button remaps, but also takes a screenshot or prompts for voice notes.

### Root Cause
* Nothing OS factory firmware routes the physical button to `com.nothing.ntessentialspace` and `com.nothing.ntessentialrecorder`.

### Solution
In Phase 9/10, the app will automate this via Wireless Debugging / Shizuku. To manually disable via ADB right now:

```bash
adb shell pm disable-user --user 0 com.nothing.ntessentialspace
adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder
```

To restore factory Essential Space behavior:

```bash
adb shell pm enable com.nothing.ntessentialspace
adb shell pm enable com.nothing.ntessentialrecorder
```

---

## 3. Diagnostic Screen Shows No Events

### Symptoms
* Accessibility service is active, but pressing the button produces no entries.

### Verification Checklist
1. Open the **Diagnostics** screen from the Home screen.
2. Verify that your device shows **ScanCode 250** as the active filter.
3. Run ADB logcat to observe system input directly:
   ```bash
   adb logcat -s "EssentialButtonService"
   ```
4. If your device uses a custom firmware build with a different scancode, use the Device Confirmation screen to recalibrate.
