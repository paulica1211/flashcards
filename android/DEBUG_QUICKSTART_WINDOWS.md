# 🚀 VS Code Debugging - Windows Quick Start

## ⚡ First Time Setup (3 Steps)

### 1️⃣ Install Java Extensions in VS Code
- Press `Ctrl+Shift+X`
- Search: **"Extension Pack for Java"**
- Click **Install**
- Reload VS Code when prompted

### 2️⃣ Enable USB Debugging on Your Phone
1. Settings → About Phone → Tap "Build Number" **7 times**
2. Settings → Developer Options → Enable **"USB Debugging"**
3. Connect phone via USB
4. Allow USB debugging on phone when prompted

### 3️⃣ Verify Connection
Open PowerShell or CMD terminal in VS Code and run:
```powershell
adb devices
```
✅ You should see your device listed

---

## 🐛 Debug Your App - Simple Method

### Method 1: Using VS Code Tasks (Easiest) ⭐

1. **Build APK**
   - Press `Ctrl+Shift+B`
   - Wait for "BUILD SUCCESSFUL"

2. **Install & Launch**
   - Press `Ctrl+Shift+P`
   - Type: "Tasks: Run Task"
   - Select: **"Build and Install App"**
   - App will build, install, and launch on your phone

3. **Get Process ID**
   Open terminal in VS Code:
   ```powershell
   adb shell ps | findstr com.gasquiz
   ```
   Look at the **2nd column** (e.g., 15242) - this is your PID

4. **Forward Debug Port**
   ```powershell
   # Replace 15242 with YOUR actual PID
   adb forward tcp:5005 jdwp:15242
   ```

5. **Attach Debugger**
   - Press `Ctrl+Shift+D` (Open Run and Debug panel)
   - Select: **"Attach to Android Process"** from dropdown
   - Press `F5`
   - ✅ Debugger attached!

6. **Set Breakpoints**
   - Open any `.java` file (e.g., `QuizActivity.java`)
   - Click the **left margin** (gutter) next to line numbers
   - Red dot = breakpoint set
   - Use the app on your phone - VS Code will pause when code hits breakpoint

---

### Method 2: Using PowerShell Commands

Open PowerShell terminal in VS Code (`Ctrl+\``) and run:

```powershell
# 1. Build
.\gradlew.bat assembleDebug

# 2. Install
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 3. Launch
adb shell am start -n com.gasquiz/.MainActivity

# 4. Get PID
adb shell ps | findstr com.gasquiz
# Note the 2nd column number (e.g., 15242)

# 5. Forward port (replace 15242 with your PID)
adb forward tcp:5005 jdwp:15242

# 6. In VS Code:
# - Press Ctrl+Shift+D
# - Select "Attach to Android Process"
# - Press F5
```

---

## 📍 Good Places to Set Breakpoints

### QuizActivity.java
**Line 150**: When quiz starts
```java
currentSheetName = info.getSheetName();
```

**Line 177**: When question loads
```java
currentQuestion = response.body();
```

**Line 265**: When checking answer
```java
boolean isCorrect = answer.equals(currentQuestion.getAnswer());
```

### SettingsActivity.java
**Line 117**: When loading quiz sets
```java
sheetNames = response.body().getSheetNames();
```

---

## 🎮 Debugging Controls

Once stopped at a breakpoint:

| Key | Action |
|-----|--------|
| `F5` | Continue |
| `F10` | Step Over (next line) |
| `F11` | Step Into (enter function) |
| `Shift+F11` | Step Out |
| `Shift+F5` | Stop Debugging |

### View Values:
- **Variables Panel** (left) - All current variables
- **Watch Panel** - Add expressions to monitor
- **Debug Console** (bottom) - Type expressions to evaluate
- **Hover** over variables in code to see values

---

## 📱 View Logs While Debugging

### Option 1: Using VS Code Task
- Press `Ctrl+Shift+P`
- Type: "Tasks: Run Task"
- Select: **"Start Logcat"**

### Option 2: Using Terminal
```powershell
adb logcat -s MainActivity:* QuizActivity:* SettingsActivity:* okhttp.OkHttpClient:*
```

You'll see:
- App startup logs
- Question loading
- Network requests/responses
- Settings changes

---

## 🔧 Troubleshooting

### "gradlew.bat is not recognized"

Make sure you're in the right directory:
```powershell
cd C:\src\projects\gasquiz\android
.\gradlew.bat assembleDebug
```

Or use relative path with `.\`:
```powershell
.\gradlew.bat assembleDebug
```

### "Cannot attach debugger"

**Solution**: Make sure port forwarding is correct
```powershell
# Get the correct PID
adb shell ps | findstr com.gasquiz

# Forward with YOUR PID (replace 15242)
adb forward tcp:5005 jdwp:15242

# Then attach in VS Code
```

### "Breakpoint not hit"

1. Rebuild the app: Press `Ctrl+Shift+B`
2. Reinstall: `Ctrl+Shift+P` → "Tasks: Run Task" → "Install Debug APK"
3. Make sure breakpoint has a **red dot** (not gray)

### "Device not found"

```powershell
# Check device connection
adb devices

# If empty, check:
# - USB debugging enabled on phone
# - USB cable connected
# - Accept "Allow USB debugging" on phone
```

### "ADB not recognized"

You need to add ADB to your PATH, or use the full path:
```powershell
# Check where ADB is
where adb

# If not found, use full path:
C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

Or add to PATH:
1. Windows Search → "Environment Variables"
2. Edit PATH
3. Add: `C:\Users\hp\AppData\Local\Android\Sdk\platform-tools`
4. Restart VS Code

---

## 💡 Pro Tips

### 1. View Network Calls
In terminal:
```powershell
adb logcat -s okhttp.OkHttpClient:*
```
See all API requests and responses!

### 2. Conditional Breakpoints
- Right-click on breakpoint → **Edit Breakpoint**
- Add condition: `questionNumber == 44`
- Debugger only pauses when condition is true

### 3. Evaluate Expressions
In Debug Console (bottom panel), type:
```java
currentQuestion.getAnswer()
sheetNames.size()
currentSheetName
```

### 4. Watch Variables
- Right-click any variable → **Add to Watch**
- See it update in real-time as you step through code

---

## 🎯 Quick Example: Debug Question Loading

Want to see what data comes from the API?

1. Open: `app\src\main\java\com\gasquiz\ui\QuizActivity.java`
2. Click left margin at **line 177** (red dot appears)
3. In VS Code terminal (PowerShell):
   ```powershell
   .\gradlew.bat assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   adb shell am start -n com.gasquiz/.MainActivity
   adb shell ps | findstr com.gasquiz  # Get PID
   adb forward tcp:5005 jdwp:YOUR_PID  # Replace YOUR_PID
   ```
4. In VS Code: `Ctrl+Shift+D` → Select "Attach to Android Process" → `F5`
5. On phone: Let a question load
6. VS Code pauses at line 177
7. Look at **Variables panel** → Expand `response` → See all the JSON data!
8. Press `F5` to continue

**You're debugging!** 🎉

---

## 📋 Complete Workflow Checklist

- [ ] Install Java extensions in VS Code
- [ ] Enable USB debugging on phone
- [ ] Connect phone via USB
- [ ] Build: `.\gradlew.bat assembleDebug` OR Press `Ctrl+Shift+B`
- [ ] Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- [ ] Launch: `adb shell am start -n com.gasquiz/.MainActivity`
- [ ] Get PID: `adb shell ps | findstr com.gasquiz`
- [ ] Forward port: `adb forward tcp:5005 jdwp:YOUR_PID`
- [ ] Set breakpoints in code
- [ ] Attach debugger: `Ctrl+Shift+D` → "Attach to Android Process" → `F5`
- [ ] Use app on phone
- [ ] VS Code pauses at breakpoint
- [ ] Inspect variables, step through code
- [ ] Press `F5` to continue

---

## 🎬 Alternative: Use CMD Instead of PowerShell

If you prefer CMD, the commands are the same but without `.\`:

```cmd
gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.gasquiz/.MainActivity
```

---

## 🆘 Need Help?

Full detailed guide: **[DEBUG_GUIDE.md](DEBUG_GUIDE.md)**

---

**Ready to debug?** Start with Step 1️⃣ above! 🚀

**Tip**: Use VS Code tasks (`Ctrl+Shift+B`) instead of typing commands - it's much easier!
