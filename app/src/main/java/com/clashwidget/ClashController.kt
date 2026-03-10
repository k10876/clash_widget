package com.clashwidget

import com.clashwidget.Constants.Commands
import com.clashwidget.Constants.Defaults
import com.clashwidget.Constants.Tags

/**
 * Controller for Clash service operations.
 * Decoupled from shell execution for testability.
 * Thread-safe for concurrent use.
 */
class ClashController(
    private val executor: ShellExecutor,
    private val config: WidgetConfig
) {
    
    constructor(executor: ShellExecutor) : this(
        executor,
        WidgetConfig.getInstance(android.app.Application().applicationContext)
    )
    
    /**
     * Get the current state of Clash.
     * @return ClashState indicating running, stopped, or unknown
     */
    fun getState(): ClashState {
        Logger.d(Tags.SHELL, "Checking Clash state...")
        
        val processName = config.processName
        val command = "${Commands.PS} | ${Commands.GREP} '$processName ${Defaults.PROCESS_ARG}' | ${Commands.GREP} -vE 'inotifyd|${Commands.GREP}'"
        
        return when (val result = executor.execute(command)) {
            is ShellResult.Success -> {
                val output = result.output
                when {
                    output.contains(processName) -> {
                        // Extract PID if possible
                        val pidMatch = Regex("\\s+(\\d+)\\s+").find(output)
                        ClashState.Running(
                            pid = pidMatch?.groupValues?.get(1),
                            processInfo = output.lines().firstOrNull()
                        ).also {
                            config.lastState = true
                            Logger.d(Tags.SHELL, "Clash is running")
                        }
                    }
                    else -> ClashState.Stopped.also {
                        config.lastState = false
                        Logger.d(Tags.SHELL, "Clash is stopped")
                    }
                }
            }
            is ShellResult.Failure -> {
                Logger.w(Tags.SHELL, "Failed to check state: ${result.error}")
                ClashState.Unknown(result.error)
            }
        }
    }
    
    /**
     * Check if Clash is currently running.
     * Convenience method that returns a boolean.
     * @return true if running, false otherwise
     */
    fun isRunning(): Boolean {
        return getState().isRunning
    }
    
    /**
     * Toggle Clash service on/off.
     * @return ToggleResult indicating the outcome
     */
    fun toggle(): ToggleResult {
        Logger.i(Tags.SHELL, "Toggle requested")
        
        val currentState = getState()
        val scriptPath = config.scriptPath
        
        return when (currentState) {
            is ClashState.Running -> {
                Logger.d(Tags.SHELL, "Stopping Clash...")
                when (val result = executor.executeRoot("sh $scriptPath stop")) {
                    is ShellResult.Success -> {
                        config.lastState = false
                        ToggleResult.Stopped()
                    }
                    is ShellResult.Failure -> {
                        Logger.e(Tags.SHELL, "Failed to stop: ${result.error}")
                        ToggleResult.Failed(
                            message = "Stop failed: ${result.error}",
                            wasRunning = true
                        )
                    }
                }
            }
            is ClashState.Stopped -> {
                Logger.d(Tags.SHELL, "Starting Clash...")
                when (val result = executor.executeRoot("sh $scriptPath start")) {
                    is ShellResult.Success -> {
                        config.lastState = true
                        ToggleResult.Started()
                    }
                    is ShellResult.Failure -> {
                        Logger.e(Tags.SHELL, "Failed to start: ${result.error}")
                        ToggleResult.Failed(
                            message = "Start failed: ${result.error}",
                            wasRunning = false
                        )
                    }
                }
            }
            is ClashState.Unknown -> {
                ToggleResult.Failed(
                    message = "Cannot toggle: ${currentState.error}",
                    wasRunning = config.lastState
                )
            }
        }
    }
    
    /**
     * Get current Clash status string.
     * @return Status string for display
     */
    fun getStatusText(): String {
        return when (val state = getState()) {
            is ClashState.Running -> "Status: RUNNING"
            is ClashState.Stopped -> "Status: STOPPED"
            is ClashState.Unknown -> "Status: UNKNOWN (${state.error})"
        }
    }
    
    /**
     * Execute a custom command and return the result.
     * @param command The command to execute
     * @param useRoot Whether to execute with root privileges
     * @return ShellResult with output
     */
    fun executeCustomCommand(command: String, useRoot: Boolean = false): ShellResult {
        Logger.d(Tags.SHELL, "Custom command: $command (root=$useRoot)")
        
        return if (useRoot) {
            executor.executeRoot(command)
        } else {
            executor.execute(command)
        }
    }
    
    /**
     * Start Clash service.
     * @return ToggleResult indicating the outcome
     */
    fun start(): ToggleResult {
        Logger.i(Tags.SHELL, "Start requested")
        
        val scriptPath = config.scriptPath
        return when (val result = executor.executeRoot("sh $scriptPath start")) {
            is ShellResult.Success -> {
                config.lastState = true
                ToggleResult.Started()
            }
            is ShellResult.Failure -> {
                Logger.e(Tags.SHELL, "Failed to start: ${result.error}")
                ToggleResult.Failed(
                    message = "Start failed: ${result.error}",
                    wasRunning = false
                )
            }
        }
    }
    
    /**
     * Stop Clash service.
     * @return ToggleResult indicating the outcome
     */
    fun stop(): ToggleResult {
        Logger.i(Tags.SHELL, "Stop requested")
        
        val scriptPath = config.scriptPath
        return when (val result = executor.executeRoot("sh $scriptPath stop")) {
            is ShellResult.Success -> {
                config.lastState = false
                ToggleResult.Stopped()
            }
            is ShellResult.Failure -> {
                Logger.e(Tags.SHELL, "Failed to stop: ${result.error}")
                ToggleResult.Failed(
                    message = "Stop failed: ${result.error}",
                    wasRunning = true
                )
            }
        }
    }
    
    companion object {
        /**
         * Create a ClashController with a custom script path.
         */
        fun withScriptPath(
            executor: ShellExecutor,
            scriptPath: String
        ): ClashController {
            // Note: This requires application context to be available
            val config = WidgetConfig.getInstance(
                android.app.Application().applicationContext
            ).apply {
                this.scriptPath = scriptPath
            }
            return ClashController(executor, config)
        }
    }
}
