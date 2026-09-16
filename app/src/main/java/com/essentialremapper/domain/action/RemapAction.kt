package com.essentialremapper.domain.action

/**
 * Scalable action model representing all configurable operations for the Essential Button.
 */
sealed interface RemapAction {

    val id: String
    val title: String

    data object None : RemapAction {
        override val id = "NONE"
        override val title = "No Action"
    }

    data object Flashlight : RemapAction {
        override val id = "FLASHLIGHT"
        override val title = "Toggle Flashlight"
    }

    data object Camera : RemapAction {
        override val id = "CAMERA"
        override val title = "Launch Camera"
    }

    data object RotationLock : RemapAction {
        override val id = "ROTATION_LOCK"
        override val title = "Toggle Rotation Lock"
    }

    data object MediaPlayPause : RemapAction {
        override val id = "MEDIA_PLAY_PAUSE"
        override val title = "Play / Pause"
    }

    data object MediaNextTrack : RemapAction {
        override val id = "MEDIA_NEXT_TRACK"
        override val title = "Next Track"
    }

    data object MediaPreviousTrack : RemapAction {
        override val id = "MEDIA_PREVIOUS_TRACK"
        override val title = "Previous Track"
    }

    data class LaunchApp(
        val packageName: String = "",
        val appName: String = "App"
    ) : RemapAction {
        override val id = "LAUNCH_APP"
        override val title = if (appName.isNotBlank() && appName != "App") "Launch $appName" else "Launch App"
    }

    data class AppShortcut(
        val packageName: String = "",
        val shortcutId: String = "",
        val shortcutLabel: String = "Shortcut",
        val intentUri: String = ""
    ) : RemapAction {
        override val id = "APP_SHORTCUT"
        override val title = "Shortcut: $shortcutLabel"
    }

    data class DeepLink(
        val uri: String = ""
    ) : RemapAction {
        override val id = "DEEP_LINK"
        override val title = if (uri.isNotBlank()) "Open $uri" else "Open URL / Deep Link"
    }

    data class SystemAction(
        val systemType: SystemActionType = SystemActionType.NOTIFICATIONS
    ) : RemapAction {
        override val id = "SYSTEM_ACTION"
        override val title = systemType.displayName
    }

    enum class SystemActionType(val displayName: String) {
        NOTIFICATIONS("Open Notifications"),
        QUICK_SETTINGS("Open Quick Settings"),
        LOCK_SCREEN("Lock Screen"),
        BACK("Back"),
        HOME("Home"),
        RECENTS("Recents"),
        TAKE_SCREENSHOT("Take Screenshot")
    }

    companion object {
        val DEFAULT_SINGLE_PRESS: RemapAction = Flashlight
        val DEFAULT_DOUBLE_PRESS: RemapAction = Camera
        val DEFAULT_LONG_PRESS: RemapAction = MediaNextTrack

        /**
         * Simple string serializer for DataStore persistence.
         */
        fun fromSerializedString(value: String): RemapAction {
            return when {
                value == "NONE" -> None
                value == "FLASHLIGHT" -> Flashlight
                value == "CAMERA" -> Camera
                value == "ROTATION_LOCK" -> RotationLock
                value == "MEDIA_PLAY_PAUSE" -> MediaPlayPause
                value == "MEDIA_NEXT_TRACK" -> MediaNextTrack
                value == "MEDIA_PREVIOUS_TRACK" -> MediaPreviousTrack
                value.startsWith("LAUNCH_APP:") -> {
                    val parts = value.removePrefix("LAUNCH_APP:").split("|", limit = 2)
                    val pkg = parts.getOrNull(0) ?: ""
                    val name = parts.getOrNull(1) ?: "App"
                    LaunchApp(pkg, name)
                }
                value.startsWith("APP_SHORTCUT:") -> {
                    val parts = value.removePrefix("APP_SHORTCUT:").split("|", limit = 4)
                    val pkg = parts.getOrNull(0) ?: ""
                    val id = parts.getOrNull(1) ?: ""
                    val label = parts.getOrNull(2) ?: "Shortcut"
                    val intentUri = parts.getOrNull(3) ?: ""
                    AppShortcut(pkg, id, label, intentUri)
                }
                value.startsWith("DEEP_LINK:") -> {
                    DeepLink(value.removePrefix("DEEP_LINK:"))
                }
                value.startsWith("SYSTEM_ACTION:") -> {
                    val typeName = value.removePrefix("SYSTEM_ACTION:")
                    val type = SystemActionType.entries.find { it.name == typeName } ?: SystemActionType.NOTIFICATIONS
                    SystemAction(type)
                }
                else -> None
            }
        }

        fun toSerializedString(action: RemapAction): String {
            return when (action) {
                is None -> "NONE"
                is Flashlight -> "FLASHLIGHT"
                is Camera -> "CAMERA"
                is RotationLock -> "ROTATION_LOCK"
                is MediaPlayPause -> "MEDIA_PLAY_PAUSE"
                is MediaNextTrack -> "MEDIA_NEXT_TRACK"
                is MediaPreviousTrack -> "MEDIA_PREVIOUS_TRACK"
                is LaunchApp -> "LAUNCH_APP:${action.packageName}|${action.appName}"
                is AppShortcut -> "APP_SHORTCUT:${action.packageName}|${action.shortcutId}|${action.shortcutLabel}|${action.intentUri}"
                is DeepLink -> "DEEP_LINK:${action.uri}"
                is SystemAction -> "SYSTEM_ACTION:${action.systemType.name}"
            }
        }
    }
}
