package com.essentialremapper.domain.device

/**
 * Normalized screen coordinate percentages (0.0f .. 1.0f) representing the physical button location.
 * x: 1.0f means right edge, 0.0f means left edge.
 * y: 0.0f means top edge, 1.0f means bottom edge.
 */
data class ButtonPosition(
    val x: Float,
    val y: Float
) {
    init {
        require(x in 0f..1f) { "x must be between 0.0 and 1.0, was $x" }
        require(y in 0f..1f) { "y must be between 0.0 and 1.0, was $y" }
    }
}
