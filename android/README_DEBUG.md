# VS Code Debugging Guide for GasQuiz

## Prerequisites

Install these VS Code extensions:
1. **Android iOS Emulator** (adelphes.android-dev-ext)
2. **Debugger for Chrome** (msjsdiag.debugger-for-chrome)
3. **Java Extension Pack** (vscjava.vscode-java-pack)
4. **Gradle for Java** (vscjava.vscode-gradle)

VS Code will prompt you to install these when you open the project.

## Quick Start

### Method 1: Using VS Code Tasks (Easiest)

1. **Press** `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
2. **Type** "Tasks: Run Task"
3. **Select** one of these tasks:
   - **"Build and Deploy"** - Builds APK, installs, and launches app
   - **"Start Logcat"** - View real-time logs
   - **"Build Debug APK"** - Just build the APK
   - **"Install APK on Device"** - Just install
   - **"Launch App on Device"** - Just launch
   - **"Check Connected Devices"** - Check device connection

### Method 2: Using Debug Panel

1. **Open** Debug panel (`Ctrl+Shift+D` or click bug icon on left)
2. **Select** debug configuration from dropdown:
   - **"Android: Launch App"** - Build and launch with debugger
   - **"Android: Attach Debugger"** - Attach to running app
   - **"Chrome: Attach to WebView"** - Debug JavaScript in WebView
3. **Press** `F5` or click green play button

## Debugging Options

### 1. Java/Android Debugging

**Launch Configuration:**
- Select "Android: Launch App" in debug panel
- Press `F5`
- This will build, install, and attach debugger
- Set breakpoints in `.java` files

**Attach to Running App:**
- Launch app manually on device
- Select "Android: Attach Debugger"
- Choose the `com.gasquiz` process
- Debugger attaches to running app

### 2. JavaScript/WebView Debugging

**Two Options:**

**Option A: Chrome DevTools (Recommended)**
1. Run app on device
2. Open Chrome browser
3. Navigate to `chrome://inspect/#devices`
4. Click "inspect" next to the WebView

**Option B: VS Code Chrome Debugger**
1. Ensure app is running on device
2. Select "Chrome: Attach to WebView"
3. Press `F5`
4. Set breakpoints in JavaScript code

### 3. Logcat Viewing

**In VS Code Terminal:**
- Run task: "Start Logcat"
- View real-time logs in dedicated terminal
- Filter shows only GasQuiz logs

**Clear Logs:**
- Run task: "Clear Logcat"

## Log Output Explanation

### App Lifecycle
- `🚀 App Started` - App launched
- `▶️ App Resumed` - App brought to foreground
- `⏸️ App Paused` - App backgrounded
- `🛑 App Destroyed` - App closed

### Page Loading
- `🔄 Page Started Loading` - Page begins loading
- `⏳ Loading Progress: X%` - Loading progress
- `✅ Page Finished Loading` - Page loaded
- `📦 Resource Loading` - Individual resources

### Navigation
- `📱 Navigation` - URL navigation
- `📍 Navigation: pushState/replaceState` - History API
- `📍 Hash Change` - Hash navigation
- `🎯 Screen Change Tracker Initialized` - Tracker ready

### JavaScript Console
- `ℹ️ LOG` - console.log()
- `🐛 DEBUG` - console.debug()
- `⚠️ WARN` - console.warn()
- `❌ ERROR` - console.error()

### Reload Events
- `🔄 Reload Triggered` - Shows reload reason

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Start Debugging | `F5` |
| Stop Debugging | `Shift+F5` |
| Restart Debugging | `Ctrl+Shift+F5` |
| Run Task | `Ctrl+Shift+P` → "Tasks: Run Task" |
| Open Debug Panel | `Ctrl+Shift+D` |
| Toggle Breakpoint | `F9` |
| Step Over | `F10` |
| Step Into | `F11` |
| Step Out | `Shift+F11` |

## Hot Reload Features

The app includes Flutter-like hot reload:

1. **Shake Device** - Reload instantly
2. **Volume Down** - Quick reload
3. **Pull Down** - Swipe to refresh
4. **Switch Away & Back** - Auto-reload on resume

## Development Workflow

### Standard Development Flow:
1. Make code changes
2. Run "Build and Deploy" task (`Ctrl+Shift+P`)
3. App automatically rebuilds, reinstalls, and launches
4. Use hot reload features for quick testing

### Debugging Flow:
1. Set breakpoints in Java code
2. Press `F5` with "Android: Launch App" selected
3. App launches with debugger attached
4. Execution pauses at breakpoints

### JavaScript Debugging Flow:
1. Open Chrome: `chrome://inspect/#devices`
2. Click "inspect" on WebView
3. Use Chrome DevTools Console/Debugger
4. Or use VS Code "Chrome: Attach to WebView"

## Troubleshooting

### Device Not Found
- Run task: "Check Connected Devices"
- Enable USB debugging on device
- Reconnect USB cable

### Build Fails
- Check Java is installed
- Ensure ANDROID_HOME is set
- Run `gradlew clean` then rebuild

### Logcat Not Working
- Ensure adb path is correct in settings
- Run `adb devices` to verify connection
- Restart adb: `adb kill-server && adb start-server`

### Chrome DevTools Not Showing WebView
- Ensure app is running
- Check WebView debugging is enabled (line 27 in MainActivity.java)
- Refresh `chrome://inspect` page

## Tips

1. **Keep Logcat Running** - Start logcat task before launching app
2. **Use Chrome DevTools** - Best for JavaScript debugging
3. **Hot Reload** - Use shake/volume for quick changes
4. **Clear Cache** - App clears cache on each reload in DEV_MODE
5. **Disable DEV_MODE** - Set to `false` for production builds

## File Locations

- Launch Config: `.vscode/launch.json`
- Tasks: `.vscode/tasks.json`
- Settings: `.vscode/settings.json`
- Main Activity: `android/app/src/main/java/com/gasquiz/MainActivity.java`
- APK Output: `android/app/build/outputs/apk/debug/app-debug.apk`

## Need Help?

- Check logs: Run "Start Logcat" task
- Verify device: Run "Check Connected Devices" task
- Chrome DevTools: Open `chrome://inspect/#devices`
- Check ADB: Open terminal and run `adb devices`
