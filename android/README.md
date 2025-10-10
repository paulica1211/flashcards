# Patent Quiz - Android WebView App

This is an Android wrapper app that loads your Google Apps Script web app in a WebView.

## Prerequisites

- Android Studio (latest version recommended)
- JDK 8 or higher
- Android SDK (API level 21 or higher)

## Setup Instructions

### Step 1: Deploy Your Google Apps Script as Web App

1. Open your Google Apps Script project at [script.google.com](https://script.google.com)
2. Click **Deploy** → **New deployment**
3. Select type: **Web app**
4. Configure settings:
   - Description: "Patent Quiz Web App"
   - Execute as: **Me**
   - Who has access: **Anyone** (or your preferred access level)
5. Click **Deploy**
6. Copy the **Web app URL** (it will look like: `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec`)

### Step 2: Configure the Android App

1. Open `android/app/src/main/java/com/gasquiz/MainActivity.java`
2. Find the line:
   ```java
   private static final String WEB_APP_URL = "YOUR_APPS_SCRIPT_WEB_APP_URL_HERE";
   ```
3. Replace `YOUR_APPS_SCRIPT_WEB_APP_URL_HERE` with your actual web app URL from Step 1

### Step 3: Add App Icons (Optional)

You'll need to add launcher icons to make your app look professional:

1. Use Android Studio's **Image Asset Studio**:
   - Right-click on `app/src/main/res` folder
   - Select **New** → **Image Asset**
   - Choose your icon image
   - Generate icons for all densities

Or manually add icon files to:
- `app/src/main/res/mipmap-hdpi/ic_launcher.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

### Step 4: Build and Install

#### Option A: Using Android Studio

1. Open Android Studio
2. Open the `android` folder as a project
3. Wait for Gradle sync to complete
4. Connect your Android device or start an emulator
5. Click **Run** (green play button) or press `Shift + F10`

#### Option B: Using Command Line

```bash
cd android
./gradlew assembleDebug
```

The APK will be generated at: `android/app/build/outputs/apk/debug/app-debug.apk`

Install it on your device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 5: Build Release APK (For Distribution)

1. Generate a signing key (first time only):
   ```bash
   keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create `android/keystore.properties`:
   ```properties
   storePassword=your_store_password
   keyPassword=your_key_password
   keyAlias=my-key-alias
   storeFile=../my-release-key.keystore
   ```

3. Update `android/app/build.gradle` to include signing config:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file(keystoreProperties['storeFile'])
               storePassword keystoreProperties['storePassword']
               keyAlias keystoreProperties['keyAlias']
               keyPassword keystoreProperties['keyPassword']
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled false
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

4. Build release APK:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

## Features

- Full WebView implementation with JavaScript enabled
- Back button navigation support
- Proper lifecycle management (onResume, onPause, onDestroy)
- Internet connectivity required
- Caching enabled for better performance
- Responsive design support

## Troubleshooting

### App shows blank screen
- Verify your web app URL is correct in MainActivity.java
- Check that your Google Apps Script is deployed and accessible
- Ensure your device has internet connection
- Check Android Logcat for errors

### JavaScript not working
- JavaScript is enabled by default in the WebView settings
- Check that your Apps Script deployment settings allow access

### Internet permission denied
- The AndroidManifest.xml includes internet permission by default
- Make sure you didn't modify the manifest incorrectly

## Requirements

- **Minimum SDK**: Android 5.0 (API level 21)
- **Target SDK**: Android 13 (API level 33)
- **Internet connection**: Required

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/gasquiz/
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   └── values/
│   │   │       └── strings.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Future Enhancements

Consider adding:
- Offline mode with cached data
- Push notifications
- Native sharing functionality
- Progress indicator while loading
- Error handling for network issues
- Pull-to-refresh functionality

## Support

For issues related to:
- **Android app**: Check Android Studio logcat
- **Web app functionality**: Check Google Apps Script logs
- **Google Sheets connection**: Verify spreadsheet permissions
