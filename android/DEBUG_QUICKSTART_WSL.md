# 🚀 VS Code Debugging - WSL Quick Start

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
```bash
adb devices
```
✅ You should see your device listed

---

## 🐛 Debug Your App

### Method 1: Using VS Code Tasks (Easiest)

1. **Build APK**
   - Press `Ctrl+Shift+B`
   - Or press `Ctrl+Shift+P` → type "Tasks: Run Build Task"
   - Wait for build to complete

2. **Install APK**
   - Press `Ctrl+Shift+P`
   - Type: "Tasks: Run Task"
   - Select: **"Install Debug APK"**

3. **Launch App**
   - Press `Ctrl+Shift+P`
   - Type: "Tasks: Run Task"
   - Select: **"Launch App"**
   - App should open on your phone

4. **Get Process ID**
   ```bash
   adb shell ps | grep com.gasquiz
   ```
   Look at the **2nd column** (e.g., 15242) - this is your PID

5. **Forward Debug Port**
   ```bash
   # Replace 15242 with YOUR actual PID
   adb forward tcp:5005 jdwp:15242
   ```

6. **Attach Debugger**
   - Press `Ctrl+Shift+D` (Open Run and Debug panel)
   - Select: **"Attach to Android Process"** from dropdown
   - Press `F5`
   - ✅ Debugger attached!

7. **Set Breakpoints**
   - Open any `.java` file (e.g., `QuizActivity.java`)
   - Click the **left margin** (gutter) next to line numbers
   - Red dot = breakpoint set
   - Use the app on your phone - VS Code will pause when code hits breakpoint

---

### Method 2: Using Terminal Commands

```bash
# 1. Build
bash build.sh

# 2. Install
bash install.sh

# 3. Launch
bash launch.sh

# 4. Get PID
adb shell ps | grep com.gasquiz
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

Open a new terminal and run:
```bash
adb logcat -s MainActivity:* QuizActivity:* SettingsActivity:* okhttp.OkHttpClient:*
```

You'll see:
- App startup logs
- Question loading
- Network requests/responses
- Settings changes

---

## 🔧 Troubleshooting

### "bash: build.sh: No such file or directory"

Make sure you're in the right directory:
```bash
cd /mnt/c/src/projects/gasquiz/android
bash build.sh
```

### "Cannot attach debugger"

**Solution**: Make sure port forwarding is correct
```bash
# Get the correct PID
adb shell ps | grep com.gasquiz

# Forward with YOUR PID (replace 15242)
adb forward tcp:5005 jdwp:15242

# Then attach in VS Code
```

### "Breakpoint not hit"

1. Rebuild the app: `bash build.sh`
2. Reinstall: `bash install.sh`
3. Make sure breakpoint has a **red dot** (not gray)

### "Device not found"

```bash
# Check device connection
adb devices

# If empty, check:
# - USB debugging enabled on phone
# - USB cable connected
# - Accept "Allow USB debugging" on phone
```

### "Process not found"

Make sure the app is actually running:
```bash
# Launch app first
bash launch.sh

# Then get PID
adb shell ps | grep com.gasquiz
```

---

## 💡 Pro Tips

### 1. View Network Calls
```bash
# See all API requests and responses
adb logcat -s okhttp.OkHttpClient:*
```

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

1. Open: `app/src/main/java/com/gasquiz/ui/QuizActivity.java`
2. Click left margin at **line 177** (red dot appears)
3. Run commands:
   ```bash
   bash build.sh
   bash install.sh
   bash launch.sh
   adb shell ps | grep com.gasquiz  # Get PID
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

- [ ] Build: `bash build.sh`
- [ ] Install: `bash install.sh`
- [ ] Launch: `bash launch.sh`
- [ ] Get PID: `adb shell ps | grep com.gasquiz`
- [ ] Forward port: `adb forward tcp:5005 jdwp:YOUR_PID`
- [ ] Set breakpoints in code
- [ ] Attach debugger: `Ctrl+Shift+D` → "Attach to Android Process" → `F5`
- [ ] Use app on phone
- [ ] VS Code pauses at breakpoint
- [ ] Inspect variables, step through code
- [ ] Press `F5` to continue

---

## 🆘 Need Help?

Full detailed guide: **[DEBUG_GUIDE.md](DEBUG_GUIDE.md)**

Common issues:
- Can't build? Make sure you're in `/mnt/c/src/projects/gasquiz/android`
- Can't attach? Check PID is correct and port forwarding worked
- Device not found? Enable USB debugging and reconnect

---

**Ready to debug?** Start with Step 1️⃣ above! 🚀
