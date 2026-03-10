# Clash Widget - Deployment Guide

## Overview

This guide provides step-by-step instructions for building and deploying the Clash Control Widget to an Android device.

## Prerequisites

### For Build Machine
- **JDK 17** (required)
- **Android Studio Hedgehog (2023.1.1)** or later
- **Android SDK 34**
- **Git** (optional, for version control)

### For Target Device
- Android 7.0+ (API 24+)
- Root access (Magisk or similar)
- Clash module installed at `/data/adb/modules/Clash/`

---

## Option 1: Build with Android Studio (Recommended)

### Step 1: Import Project

1. Launch Android Studio
2. Select **File → Open**
3. Navigate to the `android-widget/` directory
4. Select the `settings.gradle.kts` file and click **OK**
5. Wait for Gradle sync to complete

### Step 2: Build Release APK

1. Click **Build → Generate Signed Bundle / APK**
2. Select **APK** and click **Next**
3. Create a new keystore or use an existing one (for release builds)
4. Select the **release** build variant
5. Click **Finish**

### Step 3: Locate the APK

The built APK will be at:
```
android-widget/app/build/outputs/apk/release/app-release.apk
```

---

## Option 2: Build with Command Line (Advanced)

### Step 1: Set Up Environment

```bash
# Install JDK 17
# Ubuntu/Debian
sudo apt-get install openjdk-17-jdk

# macOS (with Homebrew)
brew install openjdk@17

# Windows: Download from Oracle or Adoptium
```

### Step 2: Set JAVA_HOME

```bash
# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### Step 3: Build APK

```bash
cd android-widget

# Build debug APK (fast, for testing)
./gradlew assembleDebug

# Build release APK (optimized, for distribution)
./gradlew assembleRelease
```

### Step 4: Verify Build Output

```bash
# Debug APK
ls -lah app/build/outputs/apk/debug/

# Release APK
ls -lah app/build/outputs/apk/release/
```

Expected sizes:
- Debug: ~50-60 KB
- Release: ~30-40 KB (minified, shrunk)

---

## Option 3: Using GitHub Actions (CI/CD)

For automated builds, create `.github/workflows/build.yml`:

```yaml
name: Build Clash Widget

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
        
      - name: Build APK
        run: |
          cd android-widget
          ./gradlew assembleDebug assembleRelease
          
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: clash-widget-apk
          path: android-widget/app/build/outputs/apk/
```

---

## Deploy to Device

### Method 1: ADB Install (Fastest)

```bash
# Connect device via USB or enable ADB over WiFi
adb devices

# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install release APK
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Method 2: Manual Transfer

1. Copy the APK file to your device (via USB, cloud storage, etc.)
2. On your device, navigate to the APK file using a file manager
3. Tap the APK to install
4. Enable "Unknown sources" if prompted
5. Tap "Install"

### Method 3: Send to Device via Email/Messaging

1. Attach the APK to an email or messaging app
2. Send it to yourself
3. Open the message on your device
4. Download and install

---

## Post-Installation Setup

### Step 1: Add Widget to Home Screen

1. Long-press on an empty area of your home screen
2. Tap **Widgets**
3. Scroll to find "Clash Control"
4. Long-press and drag to your home screen
5. Release to place the widget

### Step 2: Grant Root Access

1. Tap the toggle button on the widget
2. Grant root access when prompted by Magisk
3. Tap **Remember** to allow permanent access

### Step 3: Verify Installation

1. Check that the widget shows current status (Running/Stopped)
2. Tap the toggle button and verify Clash starts/stops
3. Tap the refresh button and verify status updates
4. Confirm toast messages appear

---

## Configuration (Optional)

### Custom Script Path

If your Clash module is in a different location:

```kotlin
// This requires building from source with modified code
// OR use adb shell to set preferences (root required)
su -c '
content insert --uri content://settings/system \
  --bind name:s:clash_widget_script_path \
  --bind value:s:/your/custom/path
'
```

### Disable Toast Notifications

```kotlin
// Modify in WidgetConfig.kt or build with custom code
val config = WidgetConfig.getInstance(context)
config.showToast = false
```

---

## Troubleshooting Deployment

### Build Errors

**"Could not find JDK 17"**
```bash
# Verify JAVA_HOME
echo $JAVA_HOME
java -version

# On Ubuntu, set alternatives
sudo update-alternatives --config java
```

**"Gradle sync failed"**
- Check internet connection (Gradle downloads dependencies)
- Delete `.gradle/` directory and re-sync
- Invalidate caches: **File → Invalidate Caches...**

### Installation Errors

**"Parse error" or "App not installed"**
- Check that APK matches device architecture (arm64-v8a, armeabi-v7a)
- Enable "Unknown sources" in Settings → Security
- Try uninstalling any previous version first

**"Permission denied" when toggling**
- Verify root access: `adb shell su -c id`
- Check Magisk SuperUser settings
- Manually test: `adb shell su -c ps -ef | grep clash`

### Widget Issues

**Widget doesn't appear in widget list**
- Ensure the app is installed (not just APK copied)
- Restart launcher/home screen app
- Reboot device

**Widget shows "○ UNKNOWN"**
- Verify Clash module is installed
- Check script path: `ls /data/adb/modules/Clash/Scripts/`
- Manually run check: `su -c ps -ef | grep -i clash`

---

## Verification Checklist

Before deploying to production, verify:

- [ ] APK builds successfully (`./gradlew assembleRelease`)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] APK size is under 50 KB
- [ ] Widget can be added to home screen
- [ ] Root access is granted and works
- [ ] Toggle button starts/stops Clash
- [ ] Refresh button updates status
- [ ] Toast notifications appear (if enabled)
- [ ] Widget survives device reboot
- [ ] No crashes in background

---

## Uninstallation

```bash
# Via ADB
adb uninstall com.clashwidget

# Via device
Settings → Apps → Clash Control → Uninstall

# Also remove widget from home screen first
```

---

## Release Signing (Optional)

For distribution, sign your APK:

```bash
# Generate keystore (once)
keytool -genkey -v -keystore my-release-key.keystore \
  -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000

# Update app/build.gradle.kts with keystore info
# Then build
./gradlew assembleRelease
```

---

## Support

For issues:
1. Check the Troubleshooting section above
2. Verify all prerequisites are met
3. Check logcat: `adb logcat -s ClashWidget:*`
4. Run unit tests: `./gradlew test`
5. Use the Node.js verification: `node test-verify.js`
