# Guide: Running ClockWise App on Virtual Device (FeatureDrop Branch)

## Date: January 14, 2026
## Branch: FeatureDrop ✓

## Current Status
✅ App successfully built without errors (BUILD SUCCESSFUL)
✅ On FeatureDrop branch
✅ All recent changes implemented (floating icon, quick actions, theme colors, etc.)

## Issue
No Android Virtual Device (AVD) is currently configured on your system.

---

## Solution: Two Options to Run the App

### **OPTION 1: Quick Run Using Android Studio (RECOMMENDED)**

This is the easiest and fastest method:

1. **Open the Project in Android Studio**
   - Open Android Studio
   - Open the ClockWiseProject folder
   - Wait for Gradle sync to complete

2. **Create/Start an AVD**
   - Click the **Device Manager** icon (phone icon in top right toolbar)
   - If you see existing devices, click the ▶️ (play) button next to one
   - If no devices exist, click **"Create Device"**:
     - Select **"Pixel 5"** or **"Pixel 6"** (recommended for this app)
     - Click **Next**
     - Select **Android 14.0 (API 34)** or **Android 15.0 (API 36)**
     - Click **Next**, then **Finish**
     - Click the ▶️ button to start the emulator

3. **Run the App**
   - Once emulator is running, click the green ▶️ **Run** button in Android Studio toolbar
   - Select your running emulator from the list
   - App will install and launch automatically

---

### **OPTION 2: Manual Setup Using Command Line**

If you prefer to use the command line or Android Studio is unavailable:

#### Step 1: Create an AVD (One-time setup)

Open **Command Prompt** or **PowerShell** and run:

```batch
cd C:\Users\Rome\AndroidStudioProjects\ClockWiseProject
set ANDROID_HOME=C:\Users\Rome\AppData\Local\Android\Sdk
set PATH=%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools;%PATH%

REM Create a Pixel 5 AVD with Android 15
avdmanager create avd -n ClockWise_Pixel5 -k "system-images;android-36;google_apis_playstore;x86_64" -d pixel_5
```

#### Step 2: Start the Emulator

```batch
emulator -avd ClockWise_Pixel5
```

Wait for emulator to fully boot (you'll see the home screen).

#### Step 3: Install and Run the App

Open a **NEW** Command Prompt/PowerShell window (keep emulator running):

```batch
cd C:\Users\Rome\AndroidStudioProjects\ClockWiseProject
run_app.bat
```

This will:
- Build the app
- Install it to the emulator
- Launch the app automatically

---

### **OPTION 3: Use the Pre-built Batch File**

I've created a helper script for you. To use it:

1. **Start an emulator first** (using Android Studio or command line as shown above)

2. **Run the batch file:**
   - Double-click `run_app.bat` in the project root folder
   - OR open Command Prompt and run:
     ```batch
     cd C:\Users\Rome\AndroidStudioProjects\ClockWiseProject
     run_app.bat
     ```

---

## Recommended Device Configuration

For best results testing the ClockWise app features:

- **Device:** Pixel 5 or Pixel 6
- **Android Version:** Android 14.0 (API 34) or Android 15.0 (API 36)
- **RAM:** At least 2 GB (4 GB recommended)
- **Resolution:** 1080x2340 or 1080x2400

These devices provide:
- Good screen size for testing floating icon positioning
- Modern Android version supporting all app features
- Fast emulation performance

---

## What You'll See When the App Runs

After launching, you can test all the recent FeatureDrop changes:

### 1. **Floating Icon**
   - ✅ Appears on screen (system overlay permission may be requested)
   - ✅ Color matches current app theme
   - ✅ Theme color changes instantly when you change theme in settings
   - Click it to see the **Choose Type** dialog

### 2. **Actions Button (Bottom Center)**
   - ✅ Appears at bottom center of main screen
   - ✅ Click it → **Choose Task Type** dialog appears **instantly** (no delay)
   - ✅ Same functionality as floating icon

### 3. **Choose Type Dialog (from Floating Icon)**
   - ✅ **X button** (top right) - closes the dialog
   - ✅ **Add a quick note** button - white clipboard icon ✓
   - ✅ **Add a quick task / focus session** button

### 4. **Task Type Selection Dialog**
   - ✅ **Quick Task** icon - white color ✓
   - ✅ **Focus Task** icon - white color ✓

### 5. **Theme Color Changes**
   - Go to Settings → Select different theme colors
   - ✅ Floating icon color changes **immediately**
   - ✅ Quick Actions icons match theme color
   - ✅ No delay in color updates

### 6. **No Delays**
   - ✅ Actions button → dialog appears instantly
   - ✅ Add note → dialog appears instantly
   - ✅ All interactions are immediate

---

## Troubleshooting

### "No devices found"
- Make sure emulator is fully started (you can see Android home screen)
- Run: `adb devices` - you should see your emulator listed

### "App installation failed"
- Run: `adb uninstall com.example.mainactivity`
- Then run `run_app.bat` again

### "Emulator won't start"
- Check if Intel HAXM or AMD Hypervisor is installed
- Try starting with: `emulator -avd ClockWise_Pixel5 -no-snapshot-load`

### "Permission denied for overlay"
- The app will request "Draw over other apps" permission for the floating icon
- Click "Allow" when prompted
- You can also enable it manually in Settings → Apps → ClockWise → Display over other apps

---

## Next Steps After Running

Once the app is running, test these key features:

1. **Enable floating icon** in Settings
2. **Try different theme colors** - watch floating icon change instantly
3. **Click Actions button** - verify instant response
4. **Click floating icon** - test all quick actions
5. **Test the X button** on dialogs
6. **Verify icon colors** are white (clipboard, quick task, focus task)

---

## Build Information

- **Branch:** FeatureDrop
- **Last Build:** January 14, 2026
- **Build Result:** SUCCESS ✅
- **APK Location:** `app/build/outputs/apk/debug/app-debug.apk`
- **Warnings:** 2 (non-critical, Room constructor selection)

---

## Quick Reference Commands

```batch
# Check connected devices
adb devices

# List available AVDs
emulator -list-avds

# Start specific AVD
emulator -avd ClockWise_Pixel5

# Build app
gradlew clean assembleDebug

# Install app
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch app
adb shell am start -n com.example.mainactivity/.MainActivity

# View app logs
adb logcat | findstr ClockWise
```

---

**Ready to test all your FeatureDrop changes! 🚀**

