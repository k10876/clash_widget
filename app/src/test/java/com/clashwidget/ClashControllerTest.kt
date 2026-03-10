package com.clashwidget

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ClashController with production-ready architecture.
 * Tests the logic without actual shell execution.
 */
class ClashControllerTest {
    
    private lateinit var mockExecutor: ShellExecutor
    private lateinit var mockConfig: WidgetConfig
    private lateinit var controller: ClashController
    
    @Before
    fun setup() {
        mockExecutor = mockk(relaxed = true)
        mockConfig = mockk(relaxed = true)
        
        // Default config values
        every { mockConfig.scriptPath } returns "/test/script"
        every { mockConfig.processName } returns "Clash.Core"
        
        controller = ClashController(mockExecutor, mockConfig)
    }
    
    // ==================== getState Tests ====================
    
    @Test
    fun `getState returns Running when process found`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("root 1234 1 0 12:00 ? 00:00:05 Clash.Core -d /data/adb/modules/Clash")
        
        val state = controller.getState()
        
        assertTrue(state is ClashState.Running)
        assertTrue(state.isRunning)
        assertEquals("● RUNNING", state.displayText)
    }
    
    @Test
    fun `getState returns Stopped when process not found`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("")
        
        val state = controller.getState()
        
        assertTrue(state is ClashState.Stopped)
        assertFalse(state.isRunning)
        assertEquals("○ STOPPED", state.displayText)
    }
    
    @Test
    fun `getState returns Unknown on execution failure`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Failure("Command failed", 1)
        
        val state = controller.getState()
        
        assertTrue(state is ClashState.Unknown)
        assertFalse(state.isRunning)
        assertTrue(state.displayText.contains("UNKNOWN"))
    }
    
    @Test
    fun `getState extracts PID when running`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("root 1234 1 0 12:00 ? 00:00:05 Clash.Core -d /data/adb/modules/Clash")
        
        val state = controller.getState()
        
        assertTrue(state is ClashState.Running)
        state as ClashState.Running
        assertEquals("1234", state.pid)
    }
    
    // ==================== isRunning Tests ====================
    
    @Test
    fun `isRunning returns true when Clash process found`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("Clash.Core process running")
        
        val result = controller.isRunning()
        
        assertTrue(result)
    }
    
    @Test
    fun `isRunning returns false when Clash process not found`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("")
        
        val result = controller.isRunning()
        
        assertFalse(result)
    }
    
    @Test
    fun `isRunning returns false on execution error`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Failure("Error")
        
        val result = controller.isRunning()
        
        assertFalse(result)
    }
    
    // ==================== toggle Tests ====================
    
    @Test
    fun `toggle stops Clash when running`() {
        // First call: getState returns running
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success("Clash.Core running"),  // getState
            ShellResult.Success("Stopped")               // stop
        )
        
        val result = controller.toggle()
        
        assertTrue(result.isSuccess)
        assertTrue(result is ToggleResult.Stopped)
        assertEquals("Clash STOPPED", result.resultMessage)
    }
    
    @Test
    fun `toggle starts Clash when stopped`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success(""),           // getState
            ShellResult.Success("Started")     // start
        )
        
        val result = controller.toggle()
        
        assertTrue(result.isSuccess)
        assertTrue(result is ToggleResult.Started)
        assertEquals("Clash STARTED", result.resultMessage)
    }
    
    @Test
    fun `toggle returns Failed when stop fails`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success("Clash.Core running"),   // getState
            ShellResult.Failure("Permission denied", 1)   // stop
        )
        
        val result = controller.toggle()
        
        assertFalse(result.isSuccess)
        assertTrue(result is ToggleResult.Failed)
        assertTrue(result.resultMessage.contains("Stop failed"))
    }
    
    @Test
    fun `toggle returns Failed when start fails`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success(""),           // getState
            ShellResult.Failure("Script not found", 127)  // start
        )
        
        val result = controller.toggle()
        
        assertFalse(result.isSuccess)
        assertTrue(result is ToggleResult.Failed)
        assertTrue(result.resultMessage.contains("Start failed"))
    }
    
    @Test
    fun `toggle uses configured script path`() {
        val scriptPath = "/custom/path/script"
        every { mockConfig.scriptPath } returns scriptPath
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success(""),
            ShellResult.Success("Done")
        )
        
        controller.toggle()
        
        verify { mockExecutor.executeRoot(match { it.contains(scriptPath) }, any()) }
    }
    
    // ==================== start/stop Tests ====================
    
    @Test
    fun `start returns Started on success`() {
        every { mockExecutor.executeRoot(any<String>(), any()) } returns 
            ShellResult.Success("Started")
        
        val result = controller.start()
        
        assertTrue(result is ToggleResult.Started)
    }
    
    @Test
    fun `stop returns Stopped on success`() {
        every { mockExecutor.executeRoot(any<String>(), any()) } returns 
            ShellResult.Success("Stopped")
        
        val result = controller.stop()
        
        assertTrue(result is ToggleResult.Stopped)
    }
    
    // ==================== getStatusText Tests ====================
    
    @Test
    fun `getStatusText returns correct string for running state`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("Clash.Core running")
        
        val status = controller.getStatusText()
        
        assertEquals("Status: RUNNING", status)
    }
    
    @Test
    fun `getStatusText returns correct string for stopped state`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("")
        
        val status = controller.getStatusText()
        
        assertEquals("Status: STOPPED", status)
    }
    
    @Test
    fun `getStatusText returns correct string for unknown state`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Failure("Error message")
        
        val status = controller.getStatusText()
        
        assertTrue(status.contains("UNKNOWN"))
        assertTrue(status.contains("Error message"))
    }
    
    // ==================== executeCustomCommand Tests ====================
    
    @Test
    fun `executeCustomCommand uses regular execution by default`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("output")
        
        val result = controller.executeCustomCommand("ls")
        
        assertTrue(result.isSuccess)
        verify { mockExecutor.execute("ls", any()) }
    }
    
    @Test
    fun `executeCustomCommand uses root execution when requested`() {
        every { mockExecutor.executeRoot(any<String>(), any()) } returns 
            ShellResult.Success("root output")
        
        val result = controller.executeCustomCommand("ls /data", useRoot = true)
        
        assertTrue(result.isSuccess)
        verify { mockExecutor.executeRoot("ls /data", any()) }
    }
    
    @Test
    fun `executeCustomCommand returns failure on error`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Failure("Command not found", 127)
        
        val result = controller.executeCustomCommand("invalid")
        
        assertFalse(result.isSuccess)
        assertTrue(result is ShellResult.Failure)
    }
    
    // ==================== ShellResult Tests ====================
    
    @Test
    fun `ShellResult Success has correct properties`() {
        val result = ShellResult.Success("test output", 0)
        
        assertTrue(result.isSuccess)
        assertTrue(result.hasOutput)
        assertEquals("test output", result.message)
    }
    
    @Test
    fun `ShellResult Failure has correct properties`() {
        val result = ShellResult.Failure("error message", 1)
        
        assertFalse(result.isSuccess)
        assertEquals("error message", result.message)
        assertFalse(result.isPermissionDenied)
        assertFalse(result.isNotFound)
    }
    
    @Test
    fun `ShellResult Failure detects permission denied`() {
        val result = ShellResult.Failure("Permission denied", 1)
        
        assertTrue(result.isPermissionDenied)
    }
    
    @Test
    fun `ShellResult Failure detects not found`() {
        val result = ShellResult.Failure("Script not found", 127)
        
        assertTrue(result.isNotFound)
    }
    
    @Test
    fun `ShellResult onSuccess executes on success`() {
        var executed = false
        ShellResult.Success("test").onSuccess { executed = true }
        
        assertTrue(executed)
    }
    
    @Test
    fun `ShellResult onFailure executes on failure`() {
        var executed = false
        ShellResult.Failure("error").onFailure { executed = true }
        
        assertTrue(executed)
    }
    
    // ==================== ToggleResult Tests ====================
    
    @Test
    fun `ToggleResult Started has correct properties`() {
        val result = ToggleResult.Started("Custom started message")
        
        assertTrue(result.isSuccess)
        assertTrue(result.isNowRunning)
        assertEquals("Custom started message", result.resultMessage)
    }
    
    @Test
    fun `ToggleResult Stopped has correct properties`() {
        val result = ToggleResult.Stopped()
        
        assertTrue(result.isSuccess)
        assertFalse(result.isNowRunning)
        assertEquals("Clash STOPPED", result.resultMessage)
    }
    
    @Test
    fun `ToggleResult Failed has correct properties`() {
        val result = ToggleResult.Failed("Error message", wasRunning = true)
        
        assertFalse(result.isSuccess)
        assertFalse(result.isNowRunning)
        assertEquals("Error message", result.resultMessage)
    }
    
    // ==================== ClashState Tests ====================
    
    @Test
    fun `ClashState Running has correct properties`() {
        val state = ClashState.Running(pid = "1234", processInfo = "info")
        
        assertTrue(state.isRunning)
        assertEquals("● RUNNING", state.displayText)
        assertEquals("1234", state.pid)
    }
    
    @Test
    fun `ClashState Stopped has correct properties`() {
        val state = ClashState.Stopped
        
        assertFalse(state.isRunning)
        assertEquals("○ STOPPED", state.displayText)
    }
    
    @Test
    fun `ClashState Unknown has correct properties`() {
        val state = ClashState.Unknown("Error")
        
        assertFalse(state.isRunning)
        assertTrue(state.displayText.contains("UNKNOWN"))
    }
}
