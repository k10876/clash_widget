#!/bin/bash

# Clash Widget Deployment Helper
# This script helps with the deployment process

set -e

echo "=========================================="
echo "  Clash Widget Deployment Helper"
echo "=========================================="
echo ""

# Configuration
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_OUTPUT_DIR="$PROJECT_DIR/app/build/outputs/apk"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check Java
check_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo -e "${GREEN}✓ Java found: $JAVA_VERSION${NC}"
        return 0
    else
        echo -e "${RED}✗ Java not found${NC}"
        echo ""
        echo "Please install JDK 17:"
        echo "  Ubuntu/Debian: sudo apt-get install openjdk-17-jdk"
        echo "  macOS: brew install openjdk@17"
        echo "  Windows: Download from Adoptium.net"
        return 1
    fi
}

# Check Gradle wrapper
check_gradle() {
    if [ -f "$PROJECT_DIR/gradlew" ]; then
        echo -e "${GREEN}✓ Gradle wrapper found${NC}"
        chmod +x "$PROJECT_DIR/gradlew" 2>/dev/null || true
        return 0
    else
        echo -e "${RED}✗ Gradle wrapper not found${NC}"
        return 1
    fi
}

# Build APK
build_apk() {
    echo ""
    echo "Choose build type:"
    echo "  1) Debug (fast, for testing)"
    echo "  2) Release (optimized, for distribution)"
    echo "  3) Both"
    read -p "Enter choice [1]: " choice
    choice=${choice:-1}

    cd "$PROJECT_DIR"

    case $choice in
        1)
            echo ""
            echo "Building debug APK..."
            ./gradlew assembleDebug
            ;;
        2)
            echo ""
            echo "Building release APK..."
            ./gradlew assembleRelease
            ;;
        3)
            echo ""
            echo "Building debug and release APKs..."
            ./gradlew assembleDebug assembleRelease
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            return 1
            ;;
    esac

    echo ""
    echo -e "${GREEN}Build complete!${NC}"
    echo ""
    list_apks
}

# List APKs
list_apks() {
    echo "Built APK files:"
    echo "------------------"
    
    if [ -d "$APK_OUTPUT_DIR/debug" ]; then
        DEBUG_APK=$(ls -1 "$APK_OUTPUT_DIR"/debug/*.apk 2>/dev/null | head -1)
        if [ -n "$DEBUG_APK" ]; then
            DEBUG_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
            echo -e "  ${GREEN}Debug:${NC} $DEBUG_APK ($DEBUG_SIZE)"
        fi
    fi
    
    if [ -d "$APK_OUTPUT_DIR/release" ]; then
        RELEASE_APK=$(ls -1 "$APK_OUTPUT_DIR"/release/*.apk 2>/dev/null | head -1)
        if [ -n "$RELEASE_APK" ]; then
            RELEASE_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
            echo -e "  ${GREEN}Release:${NC} $RELEASE_APK ($RELEASE_SIZE)"
        fi
    fi
}

# Install via ADB
install_adb() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}✗ adb not found${NC}"
        echo "Please install Android SDK platform tools"
        return 1
    fi

    echo ""
    echo "Checking for connected devices..."
    if ! adb devices | grep -q "device$"; then
        echo -e "${YELLOW}No device connected. Please connect a device via USB or enable ADB over WiFi.${NC}"
        return 1
    fi

    echo ""
    echo "Choose APK to install:"
    echo "  1) Debug"
    echo "  2) Release"
    read -p "Enter choice [1]: " choice
    choice=${choice:-1}

    APK_PATH=""
    case $choice in
        1)
            APK_PATH=$(ls -1 "$APK_OUTPUT_DIR"/debug/*.apk 2>/dev/null | head -1)
            ;;
        2)
            APK_PATH=$(ls -1 "$APK_OUTPUT_DIR"/release/*.apk 2>/dev/null | head -1)
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            return 1
            ;;
    esac

    if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}APK not found. Build first!${NC}"
        return 1
    fi

    echo ""
    echo "Installing $APK_PATH..."
    adb install -r "$APK_PATH"
    
    echo -e "${GREEN}✓ Installation complete!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Add the widget to your home screen"
    echo "  2. Grant root access when prompted"
    echo "  3. Tap the toggle button to test"
}

# Run tests
run_tests() {
    echo ""
    echo "Choose test type:"
    echo "  1) Node.js verification (quick)"
    echo "  2) Unit tests (requires Java)"
    echo "  3) Both"
    read -p "Enter choice [1]: " choice
    choice=${choice:-1}

    cd "$PROJECT_DIR"

    case $choice in
        1)
            if command -v node &> /dev/null; then
                node test-verify.js
            else
                echo -e "${RED}Node.js not found${NC}"
            fi
            ;;
        2)
            echo "Running unit tests..."
            ./gradlew test
            ;;
        3)
            if command -v node &> /dev/null; then
                node test-verify.js
            fi
            echo ""
            echo "Running unit tests..."
            ./gradlew test
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            ;;
    esac
}

# Clean build
clean_build() {
    echo ""
    read -p "Clean build artifacts? [y/N]: " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        cd "$PROJECT_DIR"
        ./gradlew clean
        echo -e "${GREEN}✓ Clean complete${NC}"
    fi
}

# Main menu
main_menu() {
    echo ""
    echo "Main Menu"
    echo "---------"
    echo "  1) Check environment"
    echo "  2) Build APK"
    echo "  3) Install via ADB"
    echo "  4) Run tests"
    echo "  5) List APKs"
    echo "  6) Clean build"
    echo "  7) Exit"
    echo ""
    read -p "Enter choice [1]: " choice
    choice=${choice:-1}

    case $choice in
        1)
            check_java || true
            check_gradle || true
            ;;
        2)
            if check_java && check_gradle; then
                build_apk
            fi
            ;;
        3)
            install_adb
            ;;
        4)
            run_tests
            ;;
        5)
            list_apks
            ;;
        6)
            clean_build
            ;;
        7)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            ;;
    esac

    echo ""
    read -p "Press Enter to continue..."
    main_menu
}

# Start
echo -e "${YELLOW}Note: This script is a helper. Full deployment guide in DEPLOYMENT.md${NC}"
echo ""
main_menu
