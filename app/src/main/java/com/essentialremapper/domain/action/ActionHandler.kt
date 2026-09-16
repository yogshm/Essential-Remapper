package com.essentialremapper.domain.action

/**
 * Common contract for all modular action executors.
 */
interface ActionHandler {

    /**
     * Executes the requested [RemapAction].
     *
     * @param action The specific action configuration to execute.
     * @param isScreenLocked Whether the device was locked at the moment of button interaction.
     * @return [ActionResult] indicating success, failure with error details, or unavailable state.
     */
    suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult
}
