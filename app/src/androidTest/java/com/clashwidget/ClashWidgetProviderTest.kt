package com.clashwidget

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for ClashWidgetProvider.
 * Tests widget behavior and UI updates with production architecture.
 */
@RunWith(AndroidJUnit4::class)
class ClashWidgetProviderTest {
    
    private lateinit var context: Context
    private lateinit var mockExecutor: ShellExecutor
    private lateinit var mockConfig: WidgetConfig
    private lateinit var testController: ClashController
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Setup mock executor
        mockExecutor = mockk(relaxed = true)
        mockConfig = mockk(relaxed = true)
        
        every { mockConfig.scriptPath } returns "/test/script"
        every { mockConfig.processName } returns "Clash.Core"
        every { mockConfig.showToast } returns true
        
        testController = ClashController(mockExecutor, mockConfig)
        
        // Set app context
        ClashWidgetProvider.setAppContext(context)
    }
    
    @After
    fun teardown() {
        ClashWidgetProvider.resetController()
        WidgetConfig.clearInstance()
        unmockkAll()
    }
    
    @Test
    fun useAppContext() {
        assertEquals("com.clashwidget", context.packageName)
    }
    
    @Test
    fun `widgetProvider can be instantiated`() {
        val provider = ClashWidgetProvider()
        assertNotNull(provider)
    }
    
    @Test
    fun `controller getState returns Running when process exists`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("Clash.Core running")
        
        val state = testController.getState()
        
        assertTrue(state is ClashState.Running)
    }
    
    @Test
    fun `controller getState returns Stopped when no process`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("")
        
        val state = testController.getState()
        
        assertTrue(state is ClashState.Stopped)
    }
    
    @Test
    fun `toggle test - start when stopped`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success(""),       // getState
            ShellResult.Success("Started")  // start
        )
        
        val result = testController.toggle()
        
        assertTrue(result is ToggleResult.Started)
        assertEquals("Clash STARTED", result.resultMessage)
    }
    
    @Test
    fun `toggle test - stop when running`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success("Clash.Core"),  // getState
            ShellResult.Success("Stopped")      // stop
        )
        
        val result = testController.toggle()
        
        assertTrue(result is ToggleResult.Stopped)
        assertEquals("Clash STOPPED", result.resultMessage)
    }
    
    @Test
    fun `refresh test - shows running status`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("Clash.Core process")
        
        val status = testController.getStatusText()
        
        assertEquals("Status: RUNNING", status)
    }
    
    @Test
    fun `refresh test - shows stopped status`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("")
        
        val status = testController.getStatusText()
        
        assertEquals("Status: STOPPED", status)
    }
    
    @Test
    fun `custom command test - regular execution`() {
        every { mockExecutor.execute(any<String>(), any()) } returns 
            ShellResult.Success("file1\nfile2")
        
        val result = testController.executeCustomCommand("ls", useRoot = false)
        
        assertTrue(result.isSuccess)
        assertTrue((result as ShellResult.Success).output.contains("file1"))
    }
    
    @Test
    fun `custom command test - root execution`() {
        every { mockExecutor.executeRoot(any<String>(), any()) } returns 
            ShellResult.Success("root content")
        
        val result = testController.executeCustomCommand("cat /data/test", useRoot = true)
        
        assertTrue(result.isSuccess)
        assertTrue((result as ShellResult.Success).output.contains("root content"))
    }
    
    @Test
    fun `widget layout resource exists`() {
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_clash)
        assertNotNull(views)
    }
    
    @Test
    fun `widget has required views`() {
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_clash)
        
        // These should not throw exceptions if views exist
        views.setTextViewText(R.id.status_text, "TEST")
        views.setTextViewText(R.id.output_text, "TEST OUTPUT")
    }
    
    @Test
    fun `ShellResult sealed class works correctly`() {
        val success = ShellResult.Success("output")
        assertTrue(success.isSuccess)
        assertTrue(success.hasOutput)
        
        val failure = ShellResult.Failure("error", 1)
        assertFalse(failure.isSuccess)
        assertFalse(failure.isPermissionDenied)
    }
    
    @Test
    fun `ToggleResult sealed class works correctly`() {
        val started = ToggleResult.Started()
        assertTrue(started.isSuccess)
        assertTrue(started.isNowRunning)
        
        val stopped = ToggleResult.Stopped()
        assertTrue(stopped.isSuccess)
        assertFalse(stopped.isNowRunning)
        
        val failed = ToggleResult.Failed("error", true)
        assertFalse(failed.isSuccess)
    }
    
    @Test
    fun `ClashState sealed class works correctly`() {
        val running = ClashState.Running(pid = "1234")
        assertTrue(running.isRunning)
        assertEquals("● RUNNING", running.displayText)
        
        val stopped = ClashState.Stopped
        assertFalse(stopped.isRunning)
        assertEquals("○ STOPPED", stopped.displayText)
        
        val unknown = ClashState.Unknown("error")
        assertFalse(unknown.isRunning)
        assertTrue(unknown.displayText.contains("UNKNOWN"))
    }
    
    @Test
    fun `WidgetConfig can store and retrieve values`() {
        val config = WidgetConfig.getInstance(context)
        
        config.scriptPath = "/custom/path"
        assertEquals("/custom/path", config.scriptPath)
        
        config.processName = "CustomProcess"
        assertEquals("CustomProcess", config.processName)
        
        config.showToast = false
        assertFalse(config.showToast)
        
        config.reset()
        // After reset, should return defaults
        assertEquals("/data/adb/modules/Clash/Scripts/Clash.Service", config.scriptPath)
    }
    
    @Test
    fun `error handling - permission denied detection`() {
        val result = ShellResult.Failure("Permission denied", 1)
        assertTrue(result.isPermissionDenied)
    }
    
    @Test
    fun `error handling - not found detection`() {
        val result = ShellResult.Failure("Script not found", 127)
        assertTrue(result.isNotFound)
    }
    
    @Test
    fun `error handling - toggle fails gracefully`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success("Clash.Core"),  // getState
            ShellResult.Failure("Permission denied", 1)  // stop fails
        )
        
        val result = testController.toggle()
        
        assertTrue(result is ToggleResult.Failed)
        assertTrue(result.resultMessage.contains("Stop failed"))
    }
    
    @Test
    fun `rapid clicks simulation - multiple toggles`() {
        every { mockExecutor.execute(any<String>(), any()) } returnsMany listOf(
            ShellResult.Success(""), ShellResult.Success("Started"),  // toggle 1: start
            ShellResult.Success("Clash.Core"), ShellResult.Success("Stopped")  // toggle 2: stop
        )
        
        // Simulate rapid clicks
        val result1 = testController.toggle()
        val result2 = testController.toggle()
        
        assertTrue(result1 is ToggleResult.Started)
        assertTrue(result2 is ToggleResult.Stopped)
    }
}
