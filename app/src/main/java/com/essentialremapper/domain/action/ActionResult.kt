package com.essentialremapper.domain.action

/**
 * Strongly-typed execution outcome of a RemapAction.
 */
sealed interface ActionResult {

    val isSuccess: Boolean
    val message: String

    data class Success(
        override val message: String = "Action executed successfully"
    ) : ActionResult {
        override val isSuccess: Boolean = true
    }

    data class Failure(
        override val message: String,
        val throwable: Throwable? = null
    ) : ActionResult {
        override val isSuccess: Boolean = false
    }

    data class Unavailable(
        override val message: String
    ) : ActionResult {
        override val isSuccess: Boolean = false
    }
}
