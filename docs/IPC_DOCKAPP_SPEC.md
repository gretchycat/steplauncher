# 🚀 StepLauncher IPC & External DockApp Registration Protocol

This document defines the Inter-Process Communication (IPC) protocol enabling external Android applications and widgets to register themselves dynamically as **External DockApps** on StepLauncher.

---

## 1. Overview

External applications can broadcast a `DockAppDescriptor` to StepLauncher to be mounted directly onto one of the User Docks.

- **Broadcast Action**: `com.steplauncher.action.REGISTER_DOCKAPP`
- **Payload Parcelable / Extras**:
  - `extra_id` (String): Unique identifier of the dockapp.
  - `extra_title` (String): Display title.
  - `extra_package` (String): Package name of the hosting app.
  - `extra_target_dock` (String): `"TOP_RIGHT"` (Main Dock) or `"BOTTOM"` (Bottom Dock).
  - `extra_type` (String): `"EXTERNAL"`.

---

## 2. Sample IPC Registration Code (External App Side)

```kotlin
val intent = Intent("com.steplauncher.action.REGISTER_DOCKAPP").apply {
    putExtra("extra_id", "ext_weather_app")
    putExtra("extra_title", "Live Weather")
    putExtra("extra_package", context.packageName)
    putExtra("extra_target_dock", "TOP_RIGHT")
    putExtra("extra_type", "EXTERNAL")
}
context.sendBroadcast(intent)
```

---

## 3. Handling inside StepLauncher

StepLauncher receives the broadcast via [`DockAppReceiver`](file:///data/data/com.termux/files/home/Projects/steplauncher/app-launcher/src/main/java/com/steplauncher/app/DockAppReceiver.kt), unpacks the `DockAppDescriptor`, and registers the tile with `DockManager.registerExternalDockApp(descriptor, context)`. The tile is immediately saved to `SharedPreferences` and rendered on the designated User Dock.
