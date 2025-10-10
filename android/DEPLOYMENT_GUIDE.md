# 🚀 Native Android App - Deployment Guide

## ✅ Native Conversion Complete!

Your app has been converted from WebView to **Native Android** with full debugging capabilities!

---

## 📋 What Was Created:

### 1. **Data Models** (`models/`)
- `Question.java` - Complete question data model
- `StartingInfo.java` - Initial quiz configuration

### 2. **Network Layer** (`network/`)
- `QuizApiService.java` - Retrofit API interface
- `ApiClient.java` - HTTP client with logging

### 3. **UI Layer** (`ui/`)
- `QuizActivity.java` - Native quiz implementation (400+ lines)

### 4. **Layouts** (`res/layout/`)
- `activity_quiz.xml` - Material Design quiz UI
- `layout_result.xml` - Result/explanation screen

### 5. **MainActivity**
- Switched from WebView to native launcher
- Original backed up as `MainActivity_WEBVIEW_BACKUP.java`

---

## 🔧 BEFORE Building - Deploy Google Apps Script API:

### Step 1: Update Your Google Apps Script

1. Open your Google Apps Script project
2. Create new file named `API.js`
3. Copy content from `/gasquiz/API.js` to the new file
4. **Deploy as Web App**:
   - Click **Deploy** → **New deployment**
   - Type: **Web app**
   - Execute as: **Me**
   - Who has access: **Anyone** (required for app to work)
   - Click **Deploy**

5. **Copy the deployment URL** (looks like):
   ```
   https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec
   ```

### Step 2: Update Android App URL

1. Open: `android/app/src/main/java/com/gasquiz/network/ApiClient.java`
2. Find line:
   ```java
   private static final String BASE_URL = "https://script.google.com/...";
   ```
3. Replace with your NEW deployment URL from Step 1

---

## 🏗️ Build & Deploy:

### Quick Build (VS Code Task):
```bash
# Press Ctrl+Shift+P
# Type: "Tasks: Run Task"
# Select: "Build and Deploy"
```

### Manual Build:
```bash
cd /mnt/c/src/projects/gasquiz/android
bash build.sh
bash install.sh
```

---

## 🐛 Debugging - The Power of Native!

### In Android Studio:

1. **Open Project**:
   ```bash
   File → Open → /mnt/c/src/projects/gasquiz/android
   ```

2. **Set Breakpoints**:
   - Open `QuizActivity.java`
   - Click left margin on any line
   - Red dot appears = breakpoint set

3. **Start Debugging**:
   ```bash
   Run → Debug 'app'
   ```

4. **When App Hits Breakpoint**:
   - ✅ **See all variables** - currentQuestion, currentSheetName, etc.
   - ✅ **Step through code** - F8 (step over), F7 (step into)
   - ✅ **Evaluate expressions** - Alt+F8
   - ✅ **Watch variables** - Add to watch list
   - ✅ **See call stack** - How you got here
   - ✅ **Modify values** - Change variables on the fly!

### Logcat (Real-time Logs):

```bash
# In terminal or VS Code:
adb logcat -s QuizActivity:* GasQuiz:*

# You'll see:
# I/MainActivity: 🚀 App Started - Patent Quiz (NATIVE)
# D/QuizActivity: Starting: Sheet=特許, Q=1
# D/QuizActivity: Question loaded successfully
# D/QuizActivity: Answer recorded
```

### Network Debugging:

The app logs ALL API calls:
```
--> GET https://script.google.com/.../exec?action=getStartingInfo
<-- 200 OK (1234ms)
{
  "questionNumber": 1,
  "sheetName": "特許"
}
```

---

## 🎯 Key Features:

### Native UI:
- ✅ Material Design components
- ✅ Smooth animations
- ✅ Touch-optimized buttons (48dp minimum)
- ✅ Card-based layout
- ✅ Proper spacing and typography

### Debugging:
- ✅ **Breakpoints** - Pause execution anywhere
- ✅ **Variable inspection** - See everything
- ✅ **Network logging** - Track all API calls
- ✅ **Stack traces** - Know exactly where errors occur
- ✅ **Step debugging** - Execute line by line

### Performance:
- ✅ No WebView overhead
- ✅ Native rendering
- ✅ Better memory usage
- ✅ Faster startup

---

## 📱 Testing:

### Test Checklist:
- [ ] App launches and shows first question
- [ ] True/False buttons work
- [ ] Next/Back navigation works
- [ ] Answer recording works
- [ ] Explanation shows correctly
- [ ] Links open in browser
- [ ] Importance buttons work
- [ ] HTML formatting displays correctly

---

## 🔄 Switching Back to WebView (if needed):

```bash
cd /mnt/c/src/projects/gasquiz/android/app/src/main/java/com/gasquiz
mv MainActivity.java MainActivity_NATIVE.java
mv MainActivity_WEBVIEW_BACKUP.java MainActivity.java
# Then rebuild
```

---

## 🐞 Common Issues & Solutions:

### Issue: "Failed to load starting info"
**Solution**:
1. Check Google Apps Script is deployed
2. Verify BASE_URL in ApiClient.java
3. Check internet permission in AndroidManifest.xml

### Issue: "Network error"
**Solution**:
1. Enable internet on device
2. Check logcat for detailed error
3. Verify Apps Script allows "Anyone" access

### Issue: App crashes on start
**Solution**:
1. Check logcat: `adb logcat`
2. Look for crash stack trace
3. Common: Missing API URL update

### Issue: HTML formatting not showing
**Solution**:
- This is expected - HTML.fromHtml() has limitations
- Complex HTML might need custom rendering
- Most basic formatting (bold, italic, colors) works

---

## 📊 Architecture Diagram:

```
┌─────────────────────────────────────────┐
│   Native Android App                    │
│                                         │
│   ┌─────────────────────────────────┐  │
│   │  QuizActivity.java              │  │
│   │  - Displays questions natively  │  │
│   │  - Handles user input           │  │
│   │  - Manages UI state             │  │
│   └─────────────────────────────────┘  │
│              ↓                          │
│   ┌─────────────────────────────────┐  │
│   │  ApiClient (Retrofit + OkHttp)  │  │
│   │  - Makes HTTP requests          │  │
│   │  - Logs all network traffic     │  │
│   │  - Parses JSON with Gson        │  │
│   └─────────────────────────────────┘  │
└─────────────────────────────────────────┘
                 ↓ HTTPS
┌─────────────────────────────────────────┐
│   Google Apps Script (API.js)           │
│   - Receives requests                   │
│   - Queries spreadsheet                 │
│   - Returns JSON responses              │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│   Google Spreadsheet                    │
│   - Your question data                  │
│   - Edit directly anytime               │
│   - Changes reflect immediately         │
└─────────────────────────────────────────┘
```

---

## 🎉 You Now Have:

✅ **Native Android App** with Material Design UI
✅ **Full Debugging** - Set breakpoints anywhere
✅ **Network Logging** - See all API calls
✅ **Better Performance** - No WebView overhead
✅ **Same Data Source** - Still using Google Sheets
✅ **Direct Editing** - Edit spreadsheet anytime

**Ready to build and test!**
