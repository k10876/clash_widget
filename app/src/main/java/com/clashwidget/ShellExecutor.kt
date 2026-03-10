package com.clashwidget

import com.clashwidget.Constants.Commands
import com.clashwidget.Constants.Timeouts
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Interface for shell command execution.
 * Allows decoupling for testing without actual root access.
 */
interface ShellExecutor {
    
    /**
     * Execute a shell command.
     * @param command The command to execute
     * @param timeout Timeout in milliseconds
     * @return ShellResult containing success status and output
     */
    fun execute(command: String, timeout: Long = Timeouts.SHELL_EXECUTION): ShellResult
    
    /**
     * Execute a shell command with root privileges.
     * @param command The command to execute
     * @param timeout Timeout in milliseconds
     * @return ShellResult containing success status and output
     */
    fun executeRoot(command: String, timeout: Long = Timeouts.SHELL_EXECUTION): ShellResult
}

/**
 * Real implementation of ShellExecutor that executes commands on the device.
 * Optimized for minimal memory allocation and fast execution.
 * Thread-safe for concurrent use.
 */
class RealShellExecutor : ShellExecutor {
    
    override fun execute(command: String, timeout: Long): ShellResult {
        Logger.d(Tags.SHELL, "Executing: $command")
        
        return try {
            val process = Runtime.getRuntime().exec(command)
            
            // Use try-finally to ensure resources are cleaned up
            try {
                val output = readStream(process.inputStream)
                val error = readStream(process.errorStream)
                
                val completed = process.waitFor(timeout, TimeUnit.MILLISECONDS)
                
                if (!completed) {
                    process.destroyForcibly()
                    Logger.w(Tags.SHELL, "Command timed out: $command")
                    ShellResult.Failure(
                        error = "Command timed out after ${timeout}ms",
                        exitCode = -1
                    )
                } else {
                    val exitCode = process.exitValue()
                    Logger.logCommand(command, false, exitCode == 0)
                    
                    if (exitCode == 0) {
                        ShellResult.Success(output = output.trim(), exitCode = exitCode)
                    } else {
                        ShellResult.Failure(
                            error = error.ifEmpty { "Command failed with exit code $exitCode" },
                            exitCode = exitCode
                        )
                    }
                }
            } finally {
                process.destroy()
            }
        } catch (e: Exception) {
            Logger.e(Tags.SHELL, "Execution failed: $command", e)
            ShellResult.Failure(
                error = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    override fun executeRoot(command: String, timeout: Long): ShellResult {
        Logger.d(Tags.SHELL, "Executing with root: $command")
        
        return try {
            // Build the root command
            val rootCommand = "${Commands.SU} -c $command"
            
            val process = Runtime.getRuntime().exec(rootCommand)
            
            try {
                val output = readStream(process.inputStream)
                val error = readStream(process.errorStream)
                
                val completed = process.waitFor(timeout, TimeUnit.MILLISECONDS)
                
                if (!completed) {
                    process.destroyForcibly()
                    Logger.w(Tags.SHELL, "Root command timed out: $command")
                    ShellResult.Failure(
                        error = "Root command timed out",
                        exitCode = -1
                    )
                } else {
                    val exitCode = process.exitValue()
                    Logger.logCommand(command, true, exitCode == 0)
                    
                    if (exitCode == 0) {
                        ShellResult.Success(output = output.trim(), exitCode = exitCode)
                    } else {
                        ShellResult.Failure(
                            error = error.ifEmpty { "Root command failed with exit code $exitCode" },
                            exitCode = exitCode
                        )
                    }
                }
            } finally {
                process.destroy()
            }
        } catch (e: Exception) {
            Logger.e(Tags.SHELL, "Root execution failed: $command", e)
            ShellResult.Failure(
                error = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    /**
     * Read an input stream efficiently.
     * Uses pre-allocated StringBuilder to minimize allocations.
     */
    private fun readStream(stream: java.io.InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream))
        val builder = StringBuilder(256)
        
        return try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line).append('\n')
            }
            builder.toString()
        } catch (e: Exception) {
            Logger.w(Tags.SHELL, "Error reading stream: ${e.message}")
            ""
        } finally {
            try {
                reader.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}
