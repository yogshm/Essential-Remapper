package com.essentialremapper.util

import android.util.Log

object AppLogger {
    private const val TAG = "EssentialRemapper"
    var isDebugEnabled: Boolean = true

    fun d(message: String, tag: String = TAG) {
        if (isDebugEnabled) {
            try {
                Log.d(tag, message)
            } catch (_: RuntimeException) {
                println("DEBUG: [$tag] $message")
            }
        }
    }

    fun i(message: String, tag: String = TAG) {
        try {
            Log.i(tag, message)
        } catch (_: RuntimeException) {
            println("INFO: [$tag] $message")
        }
    }

    fun w(message: String, tag: String = TAG, throwable: Throwable? = null) {
        try {
            Log.w(tag, message, throwable)
        } catch (_: RuntimeException) {
            println("WARN: [$tag] $message")
            throwable?.printStackTrace()
        }
    }

    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: RuntimeException) {
            System.err.println("ERROR: [$tag] $message")
            throwable?.printStackTrace()
        }
    }
}
