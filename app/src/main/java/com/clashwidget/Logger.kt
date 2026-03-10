package com.clashwidget

import android.util.Log
import com.clashwidget.Constants.Tags

/**
 * Centralized logging utility.
 * In production builds, debug logs are stripped out by ProGuard.
 */
object Logger {
    
    private var isDebug = BuildConfig.DEBUG
    
    /**
     * Enable or disable debug logging.
     */
    fun setDebug(enabled: Boolean) {
        isDebug = enabled
    }
    
    /**
     * Debug level log (stripped in release builds).
     */
    fun d(message: String) {
        if (isDebug) {
            Log.d(Tags.APP, message)
        }
    }
    
    /**
     * Debug level log with custom tag.
     */
    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Info level log.
     */
    fun i(message: String) {
        Log.i(Tags.APP, message)
    }
    
    /**
     * Info level log with custom tag.
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    /**
     * Warning level log.
     */
    fun w(message: String) {
        Log.w(Tags.APP, message)
    }
    
    /**
     * Warning level log with custom tag.
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    /**
     * Error level log.
     */
    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(Tags.APP, message, throwable)
        } else {
            Log.e(Tags.APP, message)
        }
    }
    
    /**
     * Error level log with custom tag.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log shell command execution.
     */
    fun logCommand(command: String, useRoot: Boolean, success: Boolean) {
        val prefix = if (useRoot) "[ROOT] " else "[SHELL] "
        val status = if (success) "✓" else "✗"
        d(Tags.SHELL, "$prefix$status $command")
    }
}
