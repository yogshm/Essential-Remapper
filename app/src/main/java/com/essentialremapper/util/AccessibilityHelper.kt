package com.essentialremapper.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.essentialremapper.accessibility.EssentialButtonAccessibilityService

object AccessibilityHelper {

    /**
     * Accurately checks if EssentialButtonAccessibilityService is enabled in Android system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Method 1: Check live service connection instance
        if (EssentialButtonAccessibilityService.isServiceConnected.value) {
            return true
        }

        // Method 2: Check active accessibility service list from AccessibilityManager
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val expectedComponentName = ComponentName(context, EssentialButtonAccessibilityService::class.java)

        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        if (enabledServices != null) {
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo?.serviceInfo ?: continue
                if (serviceInfo.packageName == expectedComponentName.packageName &&
                    serviceInfo.name == expectedComponentName.className
                ) {
                    return true
                }
            }
        }

        // Method 3: Check Secure Settings enabled_accessibility_services string
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedShort = "${context.packageName}/${EssentialButtonAccessibilityService::class.java.canonicalName}"
            val expectedLong = "${context.packageName}/${EssentialButtonAccessibilityService::class.java.name}"

            return enabledServicesSetting.split(":").any {
                it.equals(expectedShort, ignoreCase = true) ||
                it.equals(expectedLong, ignoreCase = true) ||
                it.contains(EssentialButtonAccessibilityService::class.java.simpleName)
            }
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Creates an intent to navigate the user directly to the Accessibility Settings screen.
     */
    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
