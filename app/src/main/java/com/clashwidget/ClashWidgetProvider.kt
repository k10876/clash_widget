package com.clashwidget

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast
import kotlinx.coroutines.*
import com.clashwidget.Constants.Actions
import com.clashwidget.Constants.Colors
import com.clashwidget.Constants.Extras
import com.clashwidget.Constants.Tags

/**
 * Lightweight Clash Control Widget
 * 
 * Features:
 * - Minimal RAM usage (~1-3 MB idle)
 * - Kotlin coroutines for async operations
 * - Toast notifications for feedback
 * - Configuration via SharedPreferences
 * - Proper error handling
 */
class ClashWidgetProvider : AppWidgetProvider() {
    
    companion object {
        // Coroutine scope for async operations
        private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        // Controller instance - lazy initialized
        private var _controller: ClashController? = null
        
        internal val controller: ClashController
            get() = _controller ?: synchronized(this) {
                _controller ?: ClashController(
                    RealShellExecutor(),
                    WidgetConfig.getInstance(getAppContext())
                ).also { _controller = it }
            }
        
        // Application context reference
        private var appContext: Context? = null
        
        internal fun setAppContext(context: Context) {
            appContext = context.applicationContext
        }
        
        internal fun getAppContext(): Context {
            return appContext ?: throw IllegalStateException("Application context not set")
        }
        
        /**
         * Update a single widget instance.
         */
        fun updateWidget(
            context: Context,
            appWidgetId: Int,
            state: ClashState,
            output: String? = null,
            showToast: Boolean = false
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_clash)
            
            // Update status indicator color
            val statusColor = when (state) {
                is ClashState.Running -> Color.parseColor(Colors.STATUS_RUNNING)
                is ClashState.Stopped -> Color.parseColor(Colors.STATUS_STOPPED)
                is ClashState.Unknown -> Color.parseColor(Colors.STATUS_STOPPED)
            }
            views.setInt(R.id.status_indicator, "setBackgroundColor", statusColor)
            
            // Update status text
            views.setTextViewText(R.id.status_text, state.displayText)
            
            // Update output if provided
            if (!output.isNullOrEmpty()) {
                views.setTextViewText(R.id.output_text, output)
                if (showToast) {
                    showToast(context, output)
                }
            }
            
            // Set up click handlers
            setupClickHandlers(context, views)
            
            // Update the widget
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }
        
        /**
         * Update all widget instances.
         */
        fun updateAllWidgets(
            context: Context,
            state: ClashState,
            output: String? = null,
            showToast: Boolean = false
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ClashWidgetProvider::class.java)
            )
            
            widgetIds.forEach { widgetId ->
                updateWidget(context, widgetId, state, output, showToast)
            }
        }
        
        /**
         * Setup click handlers for widget buttons.
         */
        private fun setupClickHandlers(context: Context, views: RemoteViews) {
            // Toggle button
            val toggleIntent = Intent(context, ClashWidgetProvider::class.java).apply {
                action = Actions.TOGGLE
            }
            views.setOnClickPendingIntent(
                R.id.toggle_button,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            
            // Refresh button
            val refreshIntent = Intent(context, ClashWidgetProvider::class.java).apply {
                action = Actions.REFRESH
            }
            views.setOnClickPendingIntent(
                R.id.refresh_button,
                PendingIntent.getBroadcast(
                    context,
                    1,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        
        /**
         * Show a toast message.
         */
        private fun showToast(context: Context, message: String) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        
        /**
         * Reset controller (for testing).
         */
        internal fun resetController() {
            _controller = null
        }
        
        /**
         * Cancel all pending operations.
         */
        internal fun cancelOperations() {
            widgetScope.coroutineContext.cancelChildren()
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Logger.d(Tags.WIDGET, "onUpdate: ${appWidgetIds.size} widgets")
        setAppContext(context)
        
        // Update widgets with current state
        widgetScope.launch {
            val state = controller.getState()
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { widgetId ->
                    updateWidget(context, widgetId, state)
                }
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        setAppContext(context)
        
        Logger.d(Tags.WIDGET, "onReceive: ${intent.action}")
        
        when (intent.action) {
            Actions.TOGGLE -> handleToggle(context)
            Actions.REFRESH -> handleRefresh(context)
            Actions.CUSTOM_COMMAND -> handleCustomCommand(context, intent)
            Actions.CONFIGURATION_CHANGED -> handleConfigurationChanged(context)
        }
    }
    
    /**
     * Handle toggle action.
     */
    private fun handleToggle(context: Context) {
        Logger.i(Tags.WIDGET, "Toggle action received")
        
        widgetScope.launch {
            try {
                val result = controller.toggle()
                val state = controller.getState()
                val config = WidgetConfig.getInstance(context)
                
                withContext(Dispatchers.Main) {
                    updateAllWidgets(
                        context,
                        state,
                        result.resultMessage,
                        showToast = config.showToast
                    )
                }
            } catch (e: Exception) {
                Logger.e(Tags.WIDGET, "Toggle failed", e)
                withContext(Dispatchers.Main) {
                    updateAllWidgets(
                        context,
                        ClashState.Unknown(e.message ?: "Unknown error"),
                        "Error: ${e.message}",
                        showToast = true
                    )
                }
            }
        }
    }
    
    /**
     * Handle refresh action.
     */
    private fun handleRefresh(context: Context) {
        Logger.i(Tags.WIDGET, "Refresh action received")
        
        widgetScope.launch {
            try {
                val state = controller.getState()
                val config = WidgetConfig.getInstance(context)
                
                withContext(Dispatchers.Main) {
                    updateAllWidgets(
                        context,
                        state,
                        "Status: ${state.displayText}",
                        showToast = config.showToast
                    )
                }
            } catch (e: Exception) {
                Logger.e(Tags.WIDGET, "Refresh failed", e)
                withContext(Dispatchers.Main) {
                    showToast(context, "Refresh failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Handle custom command action.
     */
    private fun handleCustomCommand(context: Context, intent: Intent) {
        val command = intent.getStringExtra(Extras.COMMAND) ?: return
        val useRoot = intent.getBooleanExtra(Extras.USE_ROOT, false)
        
        Logger.i(Tags.WIDGET, "Custom command: $command (root=$useRoot)")
        
        widgetScope.launch {
            try {
                val result = controller.executeCustomCommand(command, useRoot)
                val state = controller.getState()
                val output = when (result) {
                    is ShellResult.Success -> result.output.ifEmpty { "Command executed" }
                    is ShellResult.Failure -> "Error: ${result.error}"
                }
                
                withContext(Dispatchers.Main) {
                    updateAllWidgets(context, state, output, showToast = true)
                }
            } catch (e: Exception) {
                Logger.e(Tags.WIDGET, "Custom command failed", e)
                withContext(Dispatchers.Main) {
                    showToast(context, "Error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Handle configuration changed action.
     */
    private fun handleConfigurationChanged(context: Context) {
        Logger.i(Tags.WIDGET, "Configuration changed")
        
        widgetScope.launch {
            val state = controller.getState()
            withContext(Dispatchers.Main) {
                updateAllWidgets(context, state)
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        Logger.i(Tags.WIDGET, "Widget enabled")
        setAppContext(context)
    }
    
    override fun onDisabled(context: Context) {
        Logger.i(Tags.WIDGET, "Widget disabled")
        cancelOperations()
        WidgetConfig.clearInstance()
    }
}
