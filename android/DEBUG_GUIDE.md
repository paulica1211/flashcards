# 🐛 VS Code Android Debugging Guide

## Prerequisites

### 1. Install Required VS Code Extensions

VS Code should prompt you to install recommended extensions when you open this project. If not, install these manually:

1. **Extension Pack for Java** (vscjava.vscode-java-pack)
2. **Debugger for Java** (vscjava.vscode-java-debug)
3. **Language Support for Java** (redhat.java)

To install:
- Press `Ctrl+Shift+X` to open Extensions
- Search for "Extension Pack for Java"
- Click Install

### 2. Enable USB Debugging on Your Android Device

1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times to enable Developer Options
3. Go to **Settings** → **Developer Options**
4. Enable **USB Debugging**
5. Connect your device via USB
6. Allow USB debugging when prompted

### 3. Verify Device Connection

```bash
adb devices
```

You should see your device listed.

---

## Method 1: Simple Debugging (Recommended)

This is the easiest way to debug using VS Code's built-in Java debugger.

### Step 1: Build and Install Debug APK

**Option A: Using VS Code Tasks**
1. Press `Ctrl+Shift+B` (or `Cmd+Shift+B` on Mac)
2. Select **"Build Debug APK"**
3. Wait for build to complete

**Option B: Using Terminal (WSL)**
```bash
cd /mnt/c/src/projects/gasquiz/android
bash build.sh
```

### Step 2: Install on Device

**Option A: Using VS Code Task**
- Press `Ctrl+Shift+P`
- Type: "Tasks: Run Task"
- Select: **"Install Debug APK"**

**Option B: Using Terminal (WSL)**
```bash
bash install.sh
```

### Step 3: Get Process ID

Once the app is running on your device:

```bash
# Find the app's process ID
adb shell ps | grep com.gasquiz
```

Note the second column number (PID).

### Step 4: Setup Port Forwarding

Replace `<PID>` with your actual process ID:

```bash
adb forward tcp:5005 jdwp:<PID>
```

For example:
```bash
adb forward tcp:5005 jdwp:12345
```

### Step 5: Start Debugging

1. Open **Run and Debug** panel (`Ctrl+Shift+D`)
2. Select **"Attach to Android Process"** from dropdown
3. Press `F5` or click the green play button
4. Set breakpoints in your Java files by clicking left margin

---

## Method 2: Automated Debugging (One-Click)

This method tries to automate all steps, but may require manual intervention.

### Step 1: Set Breakpoints First

Before starting debug:
1. Open the file you want to debug (e.g., `QuizActivity.java`)
2. Click the left margin on the line where you want to pause
3. A red dot will appear (breakpoint)

### Step 2: Start Debug Session

1. Open **Run and Debug** panel (`Ctrl+Shift+D`)
2. Select **"Debug Android App"** from dropdown
3. Press `F5`

The debugger will:
- Build the APK
- Install on device
- Launch the app
- Try to attach debugger

**Note**: This method may fail at the port forwarding step. If it does, use Method 1 instead.

---

## Setting Breakpoints

### Where to Set Breakpoints:

**QuizActivity.java** (`app/src/main/java/com/gasquiz/ui/QuizActivity.java`)
- Line 150: `currentSheetName = info.getSheetName();` - When quiz starts
- Line 177: `currentQuestion = response.body();` - When question loads
- Line 265: `boolean isCorrect = answer.equals(currentQuestion.getAnswer());` - When answer is checked

**SettingsActivity.java** (`app/src/main/java/com/gasquiz/ui/SettingsActivity.java`)
- Line 117: `sheetNames = response.body().getSheetNames();` - When loading quiz sets
- Line 101: `Log.d(TAG, "Saving settings: "...` - When saving settings

### Setting Breakpoints:

1. Open the Java file
2. Click in the **left margin** (gutter) next to the line number
3. A **red dot** appears = breakpoint set
4. Click again to remove breakpoint

---

## Debugging Controls

Once the debugger is attached and hits a breakpoint:

- **F5** - Continue (run until next breakpoint)
- **F10** - Step Over (execute current line, don't enter functions)
- **F11** - Step Into (enter function calls)
- **Shift+F11** - Step Out (exit current function)
- **Ctrl+Shift+F5** - Restart debugging
- **Shift+F5** - Stop debugging

### Debug Panel Views:

- **Variables** - See all local variables and their values
- **Watch** - Add expressions to monitor (e.g., `currentQuestion.getAnswer()`)
- **Call Stack** - See the sequence of function calls that led here
- **Breakpoints** - Manage all your breakpoints

---

## Viewing Variables

When stopped at a breakpoint:

1. **Variables Panel** (left side) shows all current variables
2. **Hover** over variables in code to see their values
3. **Debug Console** (bottom) - type expressions to evaluate:
   ```java
   currentQuestion.getQuestion()
   currentSheetName
   sheetNames.size()
   ```

---

## Monitoring Network Calls

To see API requests/responses:

### Option 1: Logcat in Terminal
```bash
adb logcat -s okhttp.OkHttpClient:*
```

### Option 2: VS Code Task
1. Press `Ctrl+Shift+P`
2. Type: "Tasks: Run Task"
3. Select: **"Start Logcat"**

You'll see all HTTP requests and responses with full details.

---

## Common Debugging Scenarios

### Debug App Startup

1. Set breakpoint in `MainActivity.java` line 73: `setContentView(R.layout.activity_main);`
2. Start debugger
3. App will pause right at startup

### Debug Question Loading

1. Set breakpoint in `QuizActivity.java` line 177: `currentQuestion = response.body();`
2. Start debugger
3. Open the app
4. When question loads, debugger will pause
5. Inspect `response.body()` to see the JSON data

### Debug Settings Save

1. Set breakpoint in `SettingsActivity.java` line 101: `Log.d(TAG, "Saving settings:...`
2. Start debugger
3. Open Settings, change values, tap Save
4. Debugger pauses - inspect `selectedSheetName` and `questionNumber`

### Debug API Errors

1. Set breakpoint in `QuizActivity.java` line 187: `Log.e(TAG, "Error loading question", t);`
2. Start debugger
3. If there's a network error, debugger will pause
4. Inspect the `t` (Throwable) variable to see the error details

---

## Troubleshooting

### "Unable to attach debugger"

**Solution 1**: Manually forward port
```bash
# Get PID
adb shell ps | grep com.gasquiz

# Forward port (replace 12345 with actual PID)
adb forward tcp:5005 jdwp:12345
```

Then use **"Attach to Android Process"** configuration.

**Solution 2**: Restart ADB
```bash
adb kill-server
adb start-server
adb devices
```

### "Breakpoint not hit"

1. Make sure you're running the **Debug** build (not Release)
2. Verify breakpoint has a red dot (not gray)
3. Make sure the code is actually being executed
4. Try rebuilding: `Ctrl+Shift+B` → "Build Debug APK"

### "Source code doesn't match"

Rebuild and reinstall:
```bash
# In terminal
cmd.exe /c gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Device Not Found

```bash
# Check connection
adb devices

# If no device, reconnect USB and check USB debugging is enabled
# On device: Settings → Developer Options → USB Debugging
```

---

## Quick Reference

### Build & Deploy Commands (WSL)

```bash
# Make sure you're in the android directory
cd /mnt/c/src/projects/gasquiz/android

# Build
bash build.sh

# Install
bash install.sh

# Launch
bash launch.sh

# View logs
adb logcat -s MainActivity:* QuizActivity:* SettingsActivity:* okhttp.OkHttpClient:*
```

### VS Code Shortcuts

- `Ctrl+Shift+D` - Open Run and Debug panel
- `F5` - Start debugging / Continue
- `F9` - Toggle breakpoint
- `F10` - Step over
- `F11` - Step into
- `Ctrl+Shift+B` - Build task

---

## Advanced: Conditional Breakpoints

Right-click on a breakpoint → **Edit Breakpoint** → Add condition

Examples:
- `questionNumber == 44` - Only pause when question 44 is loaded
- `sheetName.equals("特許")` - Only pause for Patent quiz
- `isCorrect == false` - Only pause on wrong answers

---

## Tips for Effective Debugging

1. **Start with Logcat** - Often logs tell you the problem without debugging
2. **Set breakpoints early** - At the start of functions you're investigating
3. **Use Watch expressions** - Monitor specific values across multiple steps
4. **Check Call Stack** - Understand how you got to the current line
5. **Evaluate in Console** - Test expressions before adding them to code
6. **Step carefully** - Use Step Over (F10) most of the time, Step Into (F11) when you need details

---

## Next Steps

1. ✅ Install Java extensions in VS Code
2. ✅ Enable USB debugging on device
3. ✅ Connect device and verify with `adb devices`
4. ✅ Set a breakpoint in `QuizActivity.java` line 177
5. ✅ Use **"Attach to Android Process"** to debug
6. 🎉 Happy debugging!

---

**Need help?** Check the error messages in:
- VS Code Debug Console (bottom panel)
- Terminal running logcat
- VS Code Problems panel (`Ctrl+Shift+M`)
