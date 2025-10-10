# Quick Deploy Guide - Command Line

## Step 1: Update Web App URL

Edit `android/app/src/main/java/com/gasquiz/MainActivity.java` line 15:
```java
private static final String WEB_APP_URL = "YOUR_APPS_SCRIPT_WEB_APP_URL_HERE";
```

Replace with your actual Google Apps Script web app URL.

## Step 2: Build APK (Windows)

Open Command Prompt and run:

```cmd
cd android
gradlew.bat assembleDebug
```

The APK will be created at:
```
android\app\build\outputs\apk\debug\app-debug.apk
```

## Step 3: Install on Device

### Option A: Using ADB (Device connected via USB)
```cmd
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Option B: Manual Install
1. Copy `app-debug.apk` to your phone
2. Open the file on your phone
3. Allow installation from unknown sources if prompted
4. Install

## Common Issues

### "gradlew.bat is not recognized"
Make sure you're in the `android` directory:
```cmd
cd c:\src\projects\gasquiz\android
```

### "JAVA_HOME is not set"
Install JDK and set JAVA_HOME:
```cmd
setx JAVA_HOME "C:\Program Files\Java\jdk-XX"
```

### "SDK location not found"
Create `android/local.properties`:
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

Replace `YourUsername` with your actual Windows username.

## Alternative: Build Release APK

For a production-ready APK:

```cmd
gradlew.bat assembleRelease
```

Output: `android\app\build\outputs\apk\release\app-release.apk`

Note: Release builds require signing configuration (see main README.md)
