package com.clashwidget

import android.content.Context
import android.content.SharedPreferences
import com.clashwidget.Constants.Defaults
import com.clashwidget.Constants.Preferences

/**
 * Manages widget configuration stored in SharedPreferences.
 * Thread-safe singleton for configuration access.
 */
class WidgetConfig private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Preferences.FILE_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Script path for Clash control.
     */
    var scriptPath: String
        get() = prefs.getString(Preferences.KEY_SCRIPT_PATH, Defaults.CONTROL_SCRIPT_PATH) ?: Defaults.CONTROL_SCRIPT_PATH
        set(value) = prefs.edit().putString(Preferences.KEY_SCRIPT_PATH, value).apply()
    
    /**
     * Process name to detect running state.
     */
    var processName: String
        get() = prefs.getString(Preferences.KEY_PROCESS_NAME, Defaults.PROCESS_NAME) ?: Defaults.PROCESS_NAME
        set(value) = prefs.edit().putString(Preferences.KEY_PROCESS_NAME, value).apply()
    
    /**
     * Whether to show toast messages.
     */
    var showToast: Boolean
        get() = prefs.getBoolean(Preferences.KEY_SHOW_TOAST, true)
        set(value) = prefs.edit().putBoolean(Preferences.KEY_SHOW_TOAST, value).apply()
    
    /**
     * Last known state (for faster UI updates).
     */
    var lastState: Boolean
        get() = prefs.getBoolean(Preferences.KEY_LAST_STATE, false)
        set(value) = prefs.edit().putBoolean(Preferences.KEY_LAST_STATE, value).apply()
    
    /**
     * Last update timestamp.
     */
    var lastUpdateTime: Long
        get() = prefs.getLong(Preferences.KEY_LAST_UPDATE, 0L)
        set(value) = prefs.edit().putLong(Preferences.KEY_LAST_UPDATE, value).apply()
    
    /**
     * Clear all preferences.
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if configuration has been customized.
     */
    fun isCustomized(): Boolean {
        return prefs.contains(Preferences.KEY_SCRIPT_PATH) ||
               prefs.contains(Preferences.KEY_PROCESS_NAME)
    }
    
    /**
     * Reset to default values.
     */
    fun reset() {
        prefs.edit().apply {
            remove(Preferences.KEY_SCRIPT_PATH)
            remove(Preferences.KEY_PROCESS_NAME)
            apply()
        }
    }
    
    companion object {
        @Volatile
        private var instance: WidgetConfig? = null
        
        /**
         * Get singleton instance of WidgetConfig.
         */
        fun getInstance(context: Context): WidgetConfig {
            return instance ?: synchronized(this) {
                instance ?: WidgetConfig(context.applicationContext).also { instance = it }
            }
        }
        
        /**
         * Clear the singleton instance (for testing).
         */
        internal fun clearInstance() {
            instance = null
        }
    }
}
