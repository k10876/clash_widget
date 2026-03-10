/**
 * Test Verification Script for Clash Widget (Production Ready)
 * 
 * Verifies the test cases match the new production architecture.
 * 
 * Run with: node android-widget/test-verify.js
 */

// ==================== Sealed Classes Simulation ====================

class ShellResult {
    static Success = class {
        constructor(output, exitCode = 0) {
            this.output = output;
            this.exitCode = exitCode;
            this.isSuccess = true;
            this.hasOutput = output.trim().length > 0;
            this.message = output;
        }
        onSuccess(fn) { fn(this.output); return this; }
        onFailure(fn) { return this; }
    };
    
    static Failure = class {
        constructor(error, exitCode = -1) {
            this.error = error;
            this.exitCode = exitCode;
            this.isSuccess = false;
            this.message = error;
            this.isPermissionDenied = error.toLowerCase().includes('permission denied');
            this.isNotFound = error.toLowerCase().includes('not found');
        }
        onSuccess(fn) { return this; }
        onFailure(fn) { fn(this.error); return this; }
    };
}

class ToggleResult {
    static Started = class {
        constructor(message = "Clash STARTED") {
            this.message = message;
            this.isSuccess = true;
            this.isNowRunning = true;
            this.resultMessage = message;
        }
    };
    
    static Stopped = class {
        constructor(message = "Clash STOPPED") {
            this.message = message;
            this.isSuccess = true;
            this.isNowRunning = false;
            this.resultMessage = message;
        }
    };
    
    static Failed = class {
        constructor(message, wasRunning) {
            this.message = message;
            this.wasRunning = wasRunning;
            this.isSuccess = false;
            this.isNowRunning = false;
            this.resultMessage = message;
        }
    };
}

class ClashState {
    static Running = class {
        constructor(pid = null, processInfo = null) {
            this.pid = pid;
            this.processInfo = processInfo;
            this.isRunning = true;
            this.displayText = "● RUNNING";
        }
    };
    
    static Stopped = class {
        constructor() {
            this.isRunning = false;
            this.displayText = "○ STOPPED";
        }
    };
    
    static Unknown = class {
        constructor(error) {
            this.error = error;
            this.isRunning = false;
            this.displayText = `○ UNKNOWN`;
        }
    };
}

// ==================== Mock Executor ====================

class MockShellExecutor {
    constructor() {
        this.mockOutput = "";
        this.mockError = "";
        this.mockSuccess = true;
        this.lastCommand = "";
        this.wasRootExecution = false;
        this.executeCallCount = 0;
        this.executeRootCallCount = 0;
        this.returnSequence = [];
        this.sequenceIndex = 0;
    }

    execute(command, timeout = 10000) {
        this.lastCommand = command;
        this.wasRootExecution = false;
        this.executeCallCount++;
        
        if (this.returnSequence.length > this.sequenceIndex) {
            return this.returnSequence[this.sequenceIndex++];
        }
        
        return this.mockSuccess
            ? new ShellResult.Success(this.mockOutput)
            : new ShellResult.Failure(this.mockError);
    }

    executeRoot(command, timeout = 10000) {
        this.lastCommand = command;
        this.wasRootExecution = true;
        this.executeRootCallCount++;
        
        if (this.returnSequence.length > this.sequenceIndex) {
            return this.returnSequence[this.sequenceIndex++];
        }
        
        return this.mockSuccess
            ? new ShellResult.Success(this.mockOutput)
            : new ShellResult.Failure(this.mockError);
    }

    setReturnSequence(results) {
        this.returnSequence = results;
        this.sequenceIndex = 0;
    }

    reset() {
        this.mockOutput = "";
        this.mockError = "";
        this.mockSuccess = true;
        this.lastCommand = "";
        this.wasRootExecution = false;
        this.executeCallCount = 0;
        this.executeRootCallCount = 0;
        this.returnSequence = [];
        this.sequenceIndex = 0;
    }
}

// ==================== Mock Config ====================

class MockWidgetConfig {
    constructor() {
        this._scriptPath = "/data/adb/modules/Clash/Scripts/Clash.Service";
        this._processName = "Clash.Core";
        this._showToast = true;
        this._lastState = false;
    }
    
    get scriptPath() { return this._scriptPath; }
    set scriptPath(v) { this._scriptPath = v; }
    
    get processName() { return this._processName; }
    set processName(v) { this._processName = v; }
    
    get showToast() { return this._showToast; }
    set showToast(v) { this._showToast = v; }
    
    get lastState() { return this._lastState; }
    set lastState(v) { this._lastState = v; }
    
    reset() {
        this._scriptPath = "/data/adb/modules/Clash/Scripts/Clash.Service";
        this._processName = "Clash.Core";
        this._showToast = true;
        this._lastState = false;
    }
}

// ==================== ClashController ====================

class ClashController {
    constructor(executor, config) {
        this.executor = executor;
        this.config = config;
    }
    
    getState() {
        const processName = this.config.processName;
        const command = `ps -ef | grep '${processName} -d' | grep -vE 'inotifyd|grep'`;
        const result = this.executor.execute(command);
        
        if (result.isSuccess && result.output.includes(processName)) {
            const pidMatch = result.output.match(/\s+(\d+)\s+/);
            return new ClashState.Running(
                pidMatch ? pidMatch[1] : null,
                result.output.split('\n')[0]
            );
        } else if (result.isSuccess) {
            return new ClashState.Stopped();
        } else {
            return new ClashState.Unknown(result.error);
        }
    }
    
    isRunning() {
        return this.getState().isRunning;
    }
    
    toggle() {
        const state = this.getState();
        const scriptPath = this.config.scriptPath;
        
        if (state.isRunning) {
            const result = this.executor.executeRoot(`sh ${scriptPath} stop`);
            if (result.isSuccess) {
                this.config.lastState = false;
                return new ToggleResult.Stopped();
            } else {
                return new ToggleResult.Failed(`Stop failed: ${result.error}`, true);
            }
        } else if (state instanceof ClashState.Stopped) {
            const result = this.executor.executeRoot(`sh ${scriptPath} start`);
            if (result.isSuccess) {
                this.config.lastState = true;
                return new ToggleResult.Started();
            } else {
                return new ToggleResult.Failed(`Start failed: ${result.error}`, false);
            }
        } else {
            return new ToggleResult.Failed(`Cannot toggle: ${state.error}`, this.config.lastState);
        }
    }
    
    start() {
        const scriptPath = this.config.scriptPath;
        const result = this.executor.executeRoot(`sh ${scriptPath} start`);
        if (result.isSuccess) {
            this.config.lastState = true;
            return new ToggleResult.Started();
        }
        return new ToggleResult.Failed(`Start failed: ${result.error}`, false);
    }
    
    stop() {
        const scriptPath = this.config.scriptPath;
        const result = this.executor.executeRoot(`sh ${scriptPath} stop`);
        if (result.isSuccess) {
            this.config.lastState = false;
            return new ToggleResult.Stopped();
        }
        return new ToggleResult.Failed(`Stop failed: ${result.error}`, true);
    }
    
    getStatusText() {
        const state = this.getState();
        if (state instanceof ClashState.Running) return "Status: RUNNING";
        if (state instanceof ClashState.Stopped) return "Status: STOPPED";
        return `Status: UNKNOWN (${state.error})`;
    }
    
    executeCustomCommand(command, useRoot = false) {
        return useRoot 
            ? this.executor.executeRoot(command)
            : this.executor.execute(command);
    }
}

// ==================== Test Runner ====================

class TestRunner {
    constructor() {
        this.passed = 0;
        this.failed = 0;
        this.tests = [];
    }

    test(name, fn) {
        this.tests.push({ name, fn });
    }

    assertEqual(actual, expected, message = "") {
        if (actual !== expected) {
            throw new Error(`${message}\nExpected: ${expected}\nActual: ${actual}`);
        }
    }

    assertTrue(value, message = "") {
        if (!value) throw new Error(`${message}\nExpected true but got false`);
    }

    assertFalse(value, message = "") {
        if (value) throw new Error(`${message}\nExpected false but got true`);
    }

    assertContains(str, substr, message = "") {
        if (!str.includes(substr)) {
            throw new Error(`${message}\nExpected "${str}" to contain "${substr}"`);
        }
    }

    async run() {
        console.log("\n" + "=".repeat(60));
        console.log("  Clash Widget Test Verification (Production Ready)");
        console.log("=".repeat(60) + "\n");

        for (const { name, fn } of this.tests) {
            try {
                await fn();
                this.passed++;
                console.log(`✅ PASS: ${name}`);
            } catch (error) {
                this.failed++;
                console.log(`❌ FAIL: ${name}`);
                console.log(`   Error: ${error.message}\n`);
            }
        }

        console.log("\n" + "-".repeat(60));
        console.log(`  Results: ${this.passed} passed, ${this.failed} failed`);
        console.log("=".repeat(60) + "\n");

        return this.failed === 0;
    }
}

// ==================== Tests ====================

const runner = new TestRunner();
let mockExecutor, mockConfig, controller;

// ---------- ShellResult Tests ----------

runner.test("ShellResult.Success has correct properties", () => {
    const result = new ShellResult.Success("test output", 0);
    runner.assertTrue(result.isSuccess);
    runner.assertTrue(result.hasOutput);
    runner.assertEqual(result.message, "test output");
});

runner.test("ShellResult.Failure has correct properties", () => {
    const result = new ShellResult.Failure("error message", 1);
    runner.assertFalse(result.isSuccess);
    runner.assertEqual(result.message, "error message");
});

runner.test("ShellResult.Failure detects permission denied", () => {
    const result = new ShellResult.Failure("Permission denied", 1);
    runner.assertTrue(result.isPermissionDenied);
});

runner.test("ShellResult.Failure detects not found", () => {
    const result = new ShellResult.Failure("Script not found", 127);
    runner.assertTrue(result.isNotFound);
});

// ---------- ToggleResult Tests ----------

runner.test("ToggleResult.Started has correct properties", () => {
    const result = new ToggleResult.Started("Custom message");
    runner.assertTrue(result.isSuccess);
    runner.assertTrue(result.isNowRunning);
    runner.assertEqual(result.resultMessage, "Custom message");
});

runner.test("ToggleResult.Stopped has correct properties", () => {
    const result = new ToggleResult.Stopped();
    runner.assertTrue(result.isSuccess);
    runner.assertFalse(result.isNowRunning);
    runner.assertEqual(result.resultMessage, "Clash STOPPED");
});

runner.test("ToggleResult.Failed has correct properties", () => {
    const result = new ToggleResult.Failed("Error", true);
    runner.assertFalse(result.isSuccess);
    runner.assertFalse(result.isNowRunning);
    runner.assertEqual(result.resultMessage, "Error");
});

// ---------- ClashState Tests ----------

runner.test("ClashState.Running has correct properties", () => {
    const state = new ClashState.Running("1234", "info");
    runner.assertTrue(state.isRunning);
    runner.assertEqual(state.displayText, "● RUNNING");
    runner.assertEqual(state.pid, "1234");
});

runner.test("ClashState.Stopped has correct properties", () => {
    const state = new ClashState.Stopped();
    runner.assertFalse(state.isRunning);
    runner.assertEqual(state.displayText, "○ STOPPED");
});

runner.test("ClashState.Unknown has correct properties", () => {
    const state = new ClashState.Unknown("Error");
    runner.assertFalse(state.isRunning);
    runner.assertContains(state.displayText, "UNKNOWN");
});

// ---------- getState Tests ----------

runner.test("getState returns Running when process found", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "root 1234 1 0 12:00 ? 00:00:05 Clash.Core -d /data";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const state = controller.getState();
    runner.assertTrue(state instanceof ClashState.Running);
    runner.assertTrue(state.isRunning);
});

runner.test("getState returns Stopped when no process", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const state = controller.getState();
    runner.assertTrue(state instanceof ClashState.Stopped);
});

runner.test("getState returns Unknown on failure", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockSuccess = false;
    mockExecutor.mockError = "Command failed";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const state = controller.getState();
    runner.assertTrue(state instanceof ClashState.Unknown);
});

// ---------- toggle Tests ----------

runner.test("toggle starts when stopped", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.setReturnSequence([
        new ShellResult.Success(""),  // getState
        new ShellResult.Success("Started")  // start
    ]);
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.toggle();
    runner.assertTrue(result instanceof ToggleResult.Started);
    runner.assertEqual(result.resultMessage, "Clash STARTED");
});

runner.test("toggle stops when running", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.setReturnSequence([
        new ShellResult.Success("Clash.Core running"),  // getState
        new ShellResult.Success("Stopped")  // stop
    ]);
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.toggle();
    runner.assertTrue(result instanceof ToggleResult.Stopped);
    runner.assertEqual(result.resultMessage, "Clash STOPPED");
});

runner.test("toggle returns Failed on start error", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.setReturnSequence([
        new ShellResult.Success(""),  // getState
        new ShellResult.Failure("Permission denied", 1)  // start fails
    ]);
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.toggle();
    runner.assertTrue(result instanceof ToggleResult.Failed);
    runner.assertContains(result.resultMessage, "Start failed");
});

runner.test("toggle returns Failed on stop error", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.setReturnSequence([
        new ShellResult.Success("Clash.Core"),  // getState
        new ShellResult.Failure("Error", 1)  // stop fails
    ]);
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.toggle();
    runner.assertTrue(result instanceof ToggleResult.Failed);
    runner.assertContains(result.resultMessage, "Stop failed");
});

// ---------- start/stop Tests ----------

runner.test("start returns Started on success", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "Started";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.start();
    runner.assertTrue(result instanceof ToggleResult.Started);
});

runner.test("stop returns Stopped on success", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "Stopped";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.stop();
    runner.assertTrue(result instanceof ToggleResult.Stopped);
});

// ---------- getStatusText Tests ----------

runner.test("getStatusText returns RUNNING when running", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "Clash.Core";
    controller = new ClashController(mockExecutor, mockConfig);
    
    runner.assertEqual(controller.getStatusText(), "Status: RUNNING");
});

runner.test("getStatusText returns STOPPED when stopped", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "";
    controller = new ClashController(mockExecutor, mockConfig);
    
    runner.assertEqual(controller.getStatusText(), "Status: STOPPED");
});

// ---------- executeCustomCommand Tests ----------

runner.test("executeCustomCommand uses regular execution by default", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "output";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.executeCustomCommand("ls");
    runner.assertTrue(result.isSuccess);
    runner.assertFalse(mockExecutor.wasRootExecution);
});

runner.test("executeCustomCommand uses root when requested", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "root output";
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result = controller.executeCustomCommand("ls /data", true);
    runner.assertTrue(result.isSuccess);
    runner.assertTrue(mockExecutor.wasRootExecution);
});

// ---------- Configuration Tests ----------

runner.test("custom script path is used", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockConfig.scriptPath = "/custom/script";
    mockExecutor.mockOutput = "";
    controller = new ClashController(mockExecutor, mockConfig);
    
    controller.toggle();
    
    runner.assertContains(mockExecutor.lastCommand, "/custom/script");
});

// ---------- Integration Tests ----------

runner.test("toggle test - simulate user clicking toggle button", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.setReturnSequence([
        new ShellResult.Success(""), new ShellResult.Success("Started"),  // toggle 1: start
        new ShellResult.Success("Clash.Core"), new ShellResult.Success("Stopped")  // toggle 2: stop
    ]);
    controller = new ClashController(mockExecutor, mockConfig);
    
    const result1 = controller.toggle();
    runner.assertEqual(result1.resultMessage, "Clash STARTED");
    
    const result2 = controller.toggle();
    runner.assertEqual(result2.resultMessage, "Clash STOPPED");
});

runner.test("refresh test - shows correct status", () => {
    mockExecutor = new MockShellExecutor();
    mockConfig = new MockWidgetConfig();
    mockExecutor.mockOutput = "Clash.Core process";
    controller = new ClashController(mockExecutor, mockConfig);
    
    runner.assertEqual(controller.getStatusText(), "Status: RUNNING");
    
    mockExecutor.mockOutput = "";
    runner.assertEqual(controller.getStatusText(), "Status: STOPPED");
});

// Run all tests
runner.run().then(success => {
    process.exit(success ? 0 : 1);
});
