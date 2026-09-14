package com.essentialremapper.domain.gesture

/**
 * Supported physical interaction gestures for the Essential Button.
 */
enum class GestureType(val displayName: String) {
    SinglePress("Single Press"),
    DoublePress("Double Press"),
    LongPress("Long Press")
}
