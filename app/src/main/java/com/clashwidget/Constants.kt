package com.clashwidget

/**
 * Application-wide constants.
 * Centralizes all configuration values for easy maintenance.
 */
object Constants {
    
    // Widget Actions
    object Actions {
        const val TOGGLE = "com.clashwidget.ACTION_TOGGLE"
        const val REFRESH = "com.clashwidget.ACTION_REFRESH"
        const val CUSTOM_COMMAND = "com.clashwidget.ACTION_CUSTOM_COMMAND"
        const val CONFIGURATION_CHANGED = "com.clashwidget.ACTION_CONFIGURATION_CHANGED"
    }
    
    // Intent Extras
    object Extras {
        const val COMMAND = "com.clashwidget.EXTRA_COMMAND"
        const val USE_ROOT = "com.clashwidget.EXTRA_USE_ROOT"
        const val WIDGET_ID = "com.clashwidget.EXTRA_WIDGET_ID"
    }
    
    // Default Values
    object Defaults {
        const val CONTROL_SCRIPT_PATH = "/data/adb/modules/Clash/Scripts/Clash.Service"
        const val PROCESS_NAME = "Clash.Core"
        const val PROCESS_ARG = "-d"
        const val WIDGET_UPDATE_INTERVAL_MS = 0L // Manual updates only for battery optimization
    }
    
    // UI Colors
    object Colors {
        const val STATUS_RUNNING = "#4CAF50"
        const val STATUS_STOPPED = "#F44336"
        const val BUTTON_PRIMARY = "#2196F3"
        const val BUTTON_SECONDARY = "#424242"
        const val BACKGROUND = "#1E1E1E"
        const val TEXT_PRIMARY = "#FFFFFF"
        const val TEXT_SECONDARY = "#CCCCCC"
    }
    
    // Shell Commands
    object Commands {
        const val SHELL = "/system/bin/sh"
        const val SU = "su"
        const val PS = "ps -ef"
        const val GREP = "grep"
    }
    
    // Timeout values (milliseconds)
    object Timeouts {
        const val SHELL_EXECUTION = 10_000L
        const val TOAST_DURATION = 3_000L
    }
    
    // SharedPreferences
    object Preferences {
        const val FILE_NAME = "clash_widget_prefs"
        const val KEY_SCRIPT_PATH = "script_path"
        const val KEY_PROCESS_NAME = "process_name"
        const val KEY_SHOW_TOAST = "show_toast"
        const val KEY_LAST_STATE = "last_state"
        const val KEY_LAST_UPDATE = "last_update"
    }
    
    // Logging Tags
    object Tags {
        const val APP = "ClashWidget"
        const val SHELL = "ClashWidget:Shell"
        const val WIDGET = "ClashWidget:Widget"
        const val CONFIG = "ClashWidget:Config"
    }
}
