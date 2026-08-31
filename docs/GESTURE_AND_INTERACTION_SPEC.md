# 🎨 StepLauncher Gesture & Interaction Specification

This document details the touch gesture handling, context menu sub-dialog flows, icon rendering pipeline, bold text styling, configurable icon sizing, and safe-area window inset integration in StepLauncher.

---

## 1. Touch & Long-Press Gesture Specification

To ensure 100% reliable touch interactions and eliminate gesture interference, StepLauncher employs a **dedicated 500ms Handler long-press timer** in [`DockTileAdapter.kt`](file:///data/data/com.termux/files/home/Projects/steplauncher/app-launcher/src/main/java/com/steplauncher/app/DockTileAdapter.kt).

### Gesture Timing & State Flow
- **Single Tap (`< 500ms`)**:
  - Immediately launches the shortcut, focuses the running task, opens VFS category, or opens Settings if triggered on the Dock Anchor.
- **500ms Long Press (`>= 500ms`)**:
  - Triggers device haptic feedback (`HapticFeedbackConstants.LONG_PRESS`).
  - Opens the context menu dialog cleanly without starting any unwanted drag/move operations.
- **Slop Protection (`> 30px` movement)**:
  - If touch movement exceeds `30px` before 500ms, the long-press timer is cancelled to prevent accidental triggers during scroll/swipe gestures.

---

## 2. Configurable Icon Sizing & Typography Pipeline

```
                       ┌────────────────────────────┐
                       │   Dock Tile Binding Flow   │
                       └──────────────┬─────────────┘
                                      │
                       Fetch Configured tileIconSizeDp (Default 56dp)
                                      │
                       Dynamic Layout Calculation (92dp x 92dp container)
                                      │
                       Is App Shortcut or External App?
                                ┌─────┴─────┐
                               YES          NO
                                │           │
              Try PackageManager.getApplicationIcon(pkg)   Use Emoji / Text Symbol
                                ┌─────┴─────┐
                             SUCCESS      FAIL
                                │           │
                       Show ImageView (56dp) Show Emoji (40sp)
```

### Visual Standards:
1. **Double-Size Default Icons (`56dp`)**:
   - The default icon size is **56dp** (twice as big as the previous 28dp/30dp size).
   - Graphic app icons display at `56dp x 56dp`, text emoji symbols display at `40sp`, and tile container boxes expand to `92dp x 92dp`.
2. **Definable Configuration**:
   - Icon sizes are fully definable in Settings (`SettingsActivity`) with presets for **28dp**, **40dp**, **56dp (2x Default)**, and **72dp**.
   - Sizing configurations are persisted in `SharedPreferences` (`key_tile_icon_size_dp`) and dynamically applied in `DockTileAdapter`.
3. **Bold Title Labels**:
   - All tile title labels enforce bold formatting (`Typeface.BOLD` / `android:textStyle="bold"`) for crisp readability over transparent glass backgrounds.

---

## 3. Sub-Dialog Context Menu Flows

### A. `"↔️ Move to Dock..."` Sub-Dialog
- **Exposed For**: User Dock tiles (`AppShortcut`, `InternalDockApp`, `ExternalDockApp`, `VfsCategoryLink`).
- **Destinations**:
  - ❖ Top-Right Main Dock
  - ❖ Bottom Main Dock
- *(Note: Running Tasks Stack is strictly excluded as a move target).*

### B. `"📌 Copy to Dock..."` Sub-Dialog
- **Exposed For**: Running Tasks (`RunningTask`).
- **Destinations**:
  - ❖ Top-Right Main Dock
  - ❖ Bottom Main Dock
- **Result**: Creates a permanent, persistent launcher shortcut (`AppShortcut`) on the target User Dock.

---

## 4. Window Insets & Display Cutout Protection

StepLauncher extends behind the system bars with a transparent background (`android:windowShowWallpaper = true`, `android:windowBackground = @android:color/transparent`), while protecting dock elements using `ViewCompat.setOnApplyWindowInsetsListener`:

- **Top Padding**: `maxOf(statusBarTop, displayCutoutTop)` ensures the **Top-Right Main Dock** never overlaps camera cutouts, notches, or status bar icons.
- **Bottom Padding**: `navBarBottom` ensures the **Bottom Main Dock** and **Bottom-Left Running Stack** sit flush above Android navigation gesture bars and 3-button nav bars.
