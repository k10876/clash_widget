package com.clashwidget

import android.app.Application

/**
 * Application class for Clash Widget.
 * Initializes global state and configuration.
 */
class ClashWidgetApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app context for widget provider
        ClashWidgetProvider.setAppContext(this)
        
        // Initialize logger
        Logger.setDebug(BuildConfig.DEBUG)
        
        Logger.i(Tags.APP, "Clash Widget initialized")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Clean up resources
        ClashWidgetProvider.cancelOperations()
        
        Logger.i(Tags.APP, "Clash Widget terminated")
    }
}
