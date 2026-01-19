# 📱 LAUNCH EMULATOR - Step-by-Step Guide

## ✅ Status: Android Studio is Open

I've opened Android Studio with your ClockWise project and started an **auto-installer** script that's waiting for an emulator to start.

---

## 🚀 Quick Steps to Launch Emulator (2 minutes)

### **Step 1: Open Device Manager**
In Android Studio window:
- Look at the **top-right toolbar**
- Find the **phone icon** 📱 (Device Manager)
- Click it

### **Step 2: Create a New Virtual Device**

**If you see existing devices:**
- Click the ▶️ (Play button) next to any device
- Skip to Step 4

**If no devices exist:**
1. Click the **"+"** or **"Create Device"** button
2. In the "Select Hardware" screen:
   - Choose **"Phone"** category
   - Select **"Pixel 5"** or **"Pixel 6"**
   - Click **"Next"**

3. In the "System Image" screen:
   - Select **"Android 15.0"** (API 36) or **"Android 14.0"** (API 34)
   - If you see a **"Download"** link, click it and wait for download
   - Click **"Next"**

4. In the "Verify Configuration" screen:
   - Leave settings as default (or change name if you want)
   - Click **"Finish"**

### **Step 3: Start the Emulator**
- You'll see your new device in the Device Manager list
- Click the **▶️ (Play button)** next to it
- Wait for emulator window to open (30-60 seconds)

### **Step 4: Wait for Boot**
- The emulator will show "Android" logo
- Wait until you see the **home screen** with app icons
- This takes 1-2 minutes on first launch

---

## 🎯 What Happens Next (Automatic!)

Once the emulator reaches the home screen:
- The **auto-installer script** I started will detect it
- It will **automatically**:
  1. Build the app
  2. Install it to the emulator
  3. Launch it
  
**You don't need to do anything else!**

---

## 📋 Visual Reference

### Where is Device Manager?
```
Android Studio Window:
┌─────────────────────────────────────────────────┐
│ File  Edit  View  ... Run  Tools  ┃ [≡] [📱] [▶] │  ← Top right
└─────────────────────────────────────────────────┘
                                          ^
                                          │
                                  Device Manager icon
```

### Device Manager Panel:
```
┌─────────────────────────────┐
│ Device Manager              │
│                             │
│  [+ Create Device]          │  ← Click if no devices
│                             │
│  OR                         │
│                             │
│  📱 Pixel 5 API 36    [▶]  │  ← Click ▶ to start
│  📱 Pixel 6 API 34    [▶]  │
│                             │
└─────────────────────────────┘
```

---

## ⚡ Alternative: If Android Studio Isn't Open

1. **Open Android Studio**
2. **Open Project**: `C:\Users\Rome\AndroidStudioProjects\ClockWiseProject`
3. Wait for Gradle sync
4. Follow steps above

Then run the auto-installer manually:
```powershell
cd C:\Users\Rome\AndroidStudioProjects\ClockWiseProject
.\auto_install_when_ready.ps1
```

---

## 🎉 Expected Result

After emulator boots, within 10-20 seconds you'll see:
- ✅ "Device detected!" message
- ✅ "Building app..." message
- ✅ "Installing app..." message
- ✅ "Launching app..." message
- ✅ **ClockWise app opens on the emulator!**

Then you can test all the FeatureDrop features:
- Floating icon (grant permission when asked)
- Actions button
- Theme colors
- Quick tasks
- All the instant responses with no delays!

---

## 🆘 Troubleshooting

**"Can't find Device Manager icon"**
- Try: Tools → Device Manager from menu bar

**"No system images available"**
- Tools → SDK Manager → SDK Platforms
- Check Android 14.0 or 15.0
- Click OK to download

**"Emulator won't start"**
- Make sure virtualization is enabled in BIOS (VT-x or AMD-V)
- Try creating a different device (Pixel 6 instead of Pixel 5)

**"Auto-installer didn't work"**
- Check if emulator is fully booted (see home screen)
- Run manually: `.\auto_install_when_ready.ps1`

---

## 📊 Current Status

✅ **Android Studio:** Opened  
✅ **Project Loaded:** ClockWiseProject on FeatureDrop branch  
✅ **App Built:** Successfully (no errors)  
✅ **Auto-Installer:** Running in background, waiting for device  
⏳ **Emulator:** Needs to be created/started (your action)  
⏳ **App Launch:** Will happen automatically once emulator is ready  

---

**You're just one click away from running your app! 🚀**

Create and start the emulator, and everything else will happen automatically!

