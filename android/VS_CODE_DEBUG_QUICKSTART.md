# 🚀 VS Code Debugging - Quick Start

## ⚡ 3-Step Setup (First Time Only)

### 1️⃣ Install Java Extensions
- Open VS Code
- Press `Ctrl+Shift+X`
- Search: **"Extension Pack for Java"**
- Click **Install**
- Reload VS Code

### 2️⃣ Enable USB Debugging on Phone
- Settings → About Phone → Tap "Build Number" 7 times
- Settings → Developer Options → Enable "USB Debugging"
- Connect USB cable
- Allow debugging prompt on phone

### 3️⃣ Verify Device
```bash
adb devices
```
You should see your device listed.

---

## 🐛 Debug Your App (Every Time)

### Method A: Simple Attach (Recommended)

1. **Build and Install**
   ```bash
   # In terminal (inside android folder)
   cmd.exe /c gradlew.bat assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Launch App on Device**
   - Tap the Patent Quiz icon on your phone

3. **Get Process ID**
   ```bash
   adb shell ps | grep com.gasquiz
   ```
   Note the number in the 2nd column (e.g., 12345)

4. **Setup Port Forwarding**
   ```bash
   # Replace 12345 with your actual PID
   adb forward tcp:5005 jdwp:12345
   ```

5. **Attach Debugger**
   - Open VS Code Run and Debug panel (`Ctrl+Shift+D`)
   - Select: **"Attach to Android Process"**
   - Press `F5`
   - Set breakpoints by clicking left margin in Java files

---

### Method B: One-Click (May Need Manual Steps)

1. Set breakpoints in your code first
2. Open Run and Debug panel (`Ctrl+Shift+D`)
3. Select: **"Debug Android App"**
4. Press `F5`

If it fails, use Method A.

---

## 📍 Where to Set Breakpoints

**QuizActivity.java** - Line 177: When question loads
```java
currentQuestion = response.body();
```

**SettingsActivity.java** - Line 117: When loading quiz sets
```java
sheetNames = response.body().getSheetNames();
```

Click the left margin (gutter) to set/remove breakpoints.

---

## 🎮 Debug Controls

Once stopped at breakpoint:
- **F5** = Continue
- **F10** = Step Over (next line)
- **F11** = Step Into (enter function)
- **Shift+F5** = Stop debugging

View **Variables** panel on left to see all values!

---

## 📱 View Logs

While debugging, open a new terminal:
```bash
adb logcat -s MainActivity:* QuizActivity:* SettingsActivity:* okhttp.OkHttpClient:*
```

See all app logs and network requests in real-time!

---

## ❓ Troubleshooting

**"Cannot attach debugger"**
- Make sure app is running on device first
- Check PID is correct: `adb shell ps | grep com.gasquiz`
- Redo port forwarding with correct PID

**"Breakpoint not hit"**
- Rebuild: `cmd.exe /c gradlew.bat assembleDebug`
- Reinstall: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

**"Device not found"**
- Check USB debugging is enabled
- Run: `adb devices`
- Reconnect USB cable if needed

---

## 📚 Full Guide

For detailed instructions, see: **[DEBUG_GUIDE.md](DEBUG_GUIDE.md)**

---

## 🎯 Quick Debugging Example

Want to see what question was loaded?

1. Open `app/src/main/java/com/gasquiz/ui/QuizActivity.java`
2. Click left margin at line 177 (red dot appears)
3. Follow Method A above to attach debugger
4. Open app on phone and let a question load
5. VS Code will pause at your breakpoint
6. Look at **Variables** panel → find `currentQuestion`
7. Expand it to see question text, answer, etc.
8. Press F5 to continue

**That's it!** You're debugging! 🎉
