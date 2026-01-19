# Floating Icon Complete Reference

**Date**: January 9, 2026  
**Branch**: FeatureDrop  
**Status**: ✅ ALL ICONS CONFIGURED CORRECTLY

## Overview

The floating button system has multiple icons across different screens. All icons are properly configured with theme-aware colors.

---

## 1. Main Floating Button (FAB)
**Location**: Always visible on screen when floating button is enabled  
**File**: `floating_button_layout.xml`

### Configuration
```xml
<ImageView
    android:id="@+id/floatingActionButton"
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:src="@drawable/ic_flash"
    android:background="@drawable/floating_button_background"
    app:tint="@color/white"  ← Icon is WHITE
    android:contentDescription="@string/quick_actions" />
```

### Visual
```
  ╔═══════╗
  ║  ⚡   ║  ← White flash icon
  ╚═══════╝
  Theme-colored circle background
```

**Icon**: ⚡ Flash (`ic_flash`)  
**Icon Color**: WHITE  
**Background**: Theme color (`?attr/colorPrimary`)

---

## 2. Quick Actions Menu
**Location**: Appears when FAB is tapped  
**File**: `floating_menu_layout.xml`

### Menu Items

#### A. Quick Task/Focus Session
```xml
<ImageView
    android:src="@drawable/ic_flash"
    app:tint="@color/white"  ← Icon is WHITE
```
- **Icon**: ⚡ Flash
- **Icon Color**: WHITE
- **Background Circle**: Theme color

#### B. Quick Note
```xml
<ImageView
    android:src="@drawable/ic_notepad"
    app:tint="@color/white"  ← Icon is WHITE
```
- **Icon**: 📋 Notepad/Clipboard
- **Icon Color**: WHITE
- **Background Circle**: Theme color

### Visual
```
┌─────────────────────────────┐
│     Quick Actions     ✕     │
├─────────────────────────────┤
│  ⚡  Quick Task/Focus   →   │  ← WHITE icon on theme circle
│  📋  Quick Note         →   │  ← WHITE icon on theme circle
└─────────────────────────────┘
```

---

## 3. Task Type Selection Menu
**Location**: Appears when "Quick Task/Focus" is tapped  
**File**: `floating_task_type_menu.xml`

### Menu Items

#### A. Quick Task (Reminder)
```xml
<ImageView
    android:src="@drawable/ic_reminder"
    app:tint="@color/white"  ← Icon is WHITE
```
- **Icon**: 🔔 Reminder/Bell
- **Icon Color**: WHITE
- **Background Circle**: Theme color

#### B. Focus Session
```xml
<ImageView
    android:src="@drawable/ic_focus"
    app:tint="@color/white"  ← Icon is WHITE
```
- **Icon**: 🎯 Focus target
- **Icon Color**: WHITE
- **Background Circle**: Theme color

### Visual
```
┌─────────────────────────────┐
│  ← Choose Type          ✕   │
├─────────────────────────────┤
│  🔔  Quick Task         →   │  ← WHITE icon on theme circle
│  🎯  Focus Session      →   │  ← WHITE icon on theme circle
└─────────────────────────────┘
```

---

## Complete Icon Color Summary

| Screen | Icon | Description | Icon Color | Background |
|--------|------|-------------|------------|------------|
| Main Screen | ⚡ Flash | Main FAB | ✅ White | Theme Color |
| Quick Actions | ⚡ Flash | Quick Task/Focus | ✅ White | Theme Color |
| Quick Actions | 📋 Notepad | Quick Note | ✅ White | Theme Color |
| Task Type | 🔔 Bell | Quick Task | ✅ White | Theme Color |
| Task Type | 🎯 Target | Focus Session | ✅ White | Theme Color |

---

## Theme Integration Details

### How It Works
1. **Icon Backgrounds**: All use `@drawable/icon_background_circle` which contains:
   ```xml
   <solid android:color="?attr/colorPrimary" />
   ```
   This automatically uses the selected theme color.

2. **Icon Symbols**: All use `app:tint="@color/white"` for maximum contrast.

3. **Dynamic Updates**: When theme changes:
   - `FloatingButtonService` receives broadcast (`ACTION_THEME_CHANGED`)
   - Resets `themedContext` to null
   - Calls `recreateFloatingViews()` to rebuild with new theme
   - All icons maintain white color, backgrounds change to new theme

### Available Theme Colors
- 🔵 Default (Blue)
- 🔵 Cyan
- 🟢 Green
- 🟣 Purple
- 🟠 Orange

All icon backgrounds will match whichever theme is selected!

---

## Where to Find the Floating Icon

### To Enable/Disable
1. Open ClockWise app
2. Go to **Settings** (gear icon)
3. Find **"Floating Button"** section
4. Toggle the switch ON
5. Grant overlay permission when prompted

### When Enabled
- The main floating button (⚡ with theme-colored background) appears on screen
- You can drag it to any edge of the screen
- Tap it to open the Quick Actions menu
- All icons will have white symbols on theme-colored backgrounds

---

## Summary

✅ **All 5 floating icons are correctly configured:**
1. Main FAB - White flash icon on theme background
2. Quick Task/Focus - White flash icon on theme circle
3. Quick Note - White notepad icon on theme circle  
4. Quick Task - White bell icon on theme circle
5. Focus Session - White target icon on theme circle

**All icons use white color for the symbols and theme color for the backgrounds!**

