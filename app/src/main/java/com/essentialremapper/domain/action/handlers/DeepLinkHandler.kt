package com.essentialremapper.domain.action.handlers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.essentialremapper.domain.action.ActionHandler
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.util.AppLogger
import java.net.URI

/**
 * Validates and opens user-configured URLs and deep links.
 * Restricts schemes strictly to safe communication protocols to prevent arbitrary execution.
 */
class DeepLinkHandler(
    private val context: Context? = null
) : ActionHandler {

    companion object {
        private const val TAG = "DeepLinkHandler"
        val ALLOWED_SCHEMES = setOf("https", "http", "content", "tel", "mailto")
    }

    override suspend fun execute(action: RemapAction, isScreenLocked: Boolean): ActionResult {
        if (action !is RemapAction.DeepLink) {
            return ActionResult.Failure("Unsupported deep link action: ${action.id}")
        }

        val rawUri = action.uri.trim()
        if (rawUri.isBlank()) {
            return ActionResult.Failure("Deep link URI is empty")
        }

        val scheme = extractScheme(rawUri)
        if (scheme == null || scheme !in ALLOWED_SCHEMES) {
            AppLogger.w("Rejected deep link with disallowed scheme: '$scheme' for URI: $rawUri", TAG)
            return ActionResult.Failure("Disallowed URI scheme '${scheme ?: "none"}'. Allowed: ${ALLOWED_SCHEMES.joinToString(", ")}")
        }

        val ctx = context ?: return ActionResult.Unavailable("Context unavailable to launch deep link")

        return try {
            val parsedUri = Uri.parse(rawUri)
            val intent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            AppLogger.i("Opened deep link: $rawUri", TAG)
            ActionResult.Success("Opened link: $rawUri")
        } catch (e: ActivityNotFoundException) {
            AppLogger.e("No application found to handle deep link: $rawUri", TAG, e)
            ActionResult.Failure("No app installed to handle: $rawUri", e)
        } catch (e: SecurityException) {
            AppLogger.e("SecurityException opening deep link: ${e.message}", TAG, e)
            ActionResult.Failure("Permission denied opening: $rawUri", e)
        } catch (e: Exception) {
            AppLogger.e("Unexpected error opening deep link: ${e.message}", TAG, e)
            ActionResult.Failure("Failed to open deep link: ${e.message}", e)
        }
    }

    fun extractScheme(rawUri: String): String? {
        return try {
            val parsed = URI(rawUri)
            parsed.scheme?.lowercase() ?: rawUri.substringBefore(":", "").trim().lowercase().ifBlank { null }
        } catch (_: Exception) {
            val fallback = rawUri.substringBefore(":", "").trim().lowercase()
            fallback.ifBlank { null }
        }
    }
}
