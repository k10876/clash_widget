package com.clashwidget

/**
 * Sealed class representing the result of a shell command.
 * Provides type-safe success/failure handling.
 */
sealed class ShellResult {
    
    /**
     * Successful command execution.
     */
    data class Success(
        val output: String,
        val exitCode: Int = 0
    ) : ShellResult() {
        val hasOutput: Boolean
            get() = output.isNotBlank()
    }
    
    /**
     * Failed command execution.
     */
    data class Failure(
        val error: String,
        val exitCode: Int = -1,
        val exception: Throwable? = null
    ) : ShellResult() {
        val isPermissionDenied: Boolean
            get() = error.contains("Permission denied", ignoreCase = true) ||
                    error.contains("not allowed", ignoreCase = true)
        
        val isNotFound: Boolean
            get() = error.contains("not found", ignoreCase = true) ||
                    error.contains("No such file", ignoreCase = true)
    }
    
    /**
     * Check if the result is successful.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Get output or error message.
     */
    val message: String
        get() = when (this) {
            is Success -> output
            is Failure -> error
        }
    
    /**
     * Map the result to another type.
     */
    inline fun <T> map(transform: (ShellResult) -> T): T {
        return transform(this)
    }
    
    /**
     * Execute action on success.
     */
    inline fun onSuccess(action: (String) -> Unit): ShellResult {
        if (this is Success) action(output)
        return this
    }
    
    /**
     * Execute action on failure.
     */
    inline fun onFailure(action: (String) -> Unit): ShellResult {
        if (this is Failure) action(error)
        return this
    }
}

/**
 * Sealed class representing the result of a toggle operation.
 */
sealed class ToggleResult {
    
    /**
     * Clash was started successfully.
     */
    data class Started(
        val message: String = "Clash STARTED"
    ) : ToggleResult()
    
    /**
     * Clash was stopped successfully.
     */
    data class Stopped(
        val message: String = "Clash STOPPED"
    ) : ToggleResult()
    
    /**
     * Toggle operation failed.
     */
    data class Failed(
        val message: String,
        val wasRunning: Boolean
    ) : ToggleResult()
    
    /**
     * Check if the operation was successful.
     */
    val isSuccess: Boolean
        get() = this !is Failed
    
    /**
     * Get the result message.
     */
    val resultMessage: String
        get() = when (this) {
            is Started -> message
            is Stopped -> message
            is Failed -> message
        }
    
    /**
     * Check if Clash is now running.
     */
    val isNowRunning: Boolean
        get() = this is Started
}

/**
 * Sealed class representing the current state of Clash.
 */
sealed class ClashState {
    
    /**
     * Clash is running.
     */
    data class Running(
        val pid: String? = null,
        val processInfo: String? = null
    ) : ClashState()
    
    /**
     * Clash is stopped.
     */
    object Stopped : ClashState()
    
    /**
     * State is unknown (error checking).
     */
    data class Unknown(
        val error: String
    ) : ClashState()
    
    /**
     * Check if Clash is running.
     */
    val isRunning: Boolean
        get() = this is Running
    
    /**
     * Get display text for the state.
     */
    val displayText: String
        get() = when (this) {
            is Running -> "● RUNNING"
            is Stopped -> "○ STOPPED"
            is Unknown -> "○ UNKNOWN"
        }
}
