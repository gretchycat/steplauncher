# ❖ StepLauncher Architecture Specification: Global Bottom Dock, Workspace Docks & Layout Lock

This document formally defines the architectural distinction, state management, operational semantics, multi-workspace model, and layout locking rules in StepLauncher.

---

## 1. Executive Summary & Core Definitions

StepLauncher categorizes desktop surfaces into three distinct dock domains:

| Dock Domain | Location | Scope & Behavior | Statefulness & Persistence |
| :--- | :--- | :--- | :--- |
| **Global Bottom Dock** | **Bottom Dock** (`BOTTOM`) | Common baseline launch bar holding primary handlers (Phone, Browser, Camera, Social Media, Multimedia). Shared across **ALL** workspaces. | **Stateful & Persistent** |
| **Workspace Docks** | **Right-Handed Main Dock** (`TOP_RIGHT`) | Independent workspace docks specific to the active workspace (Workspace 1, Workspace 2, Workspace 3). | **Stateful & Persistent** per Workspace |
| **Process Dock** | **Bottom-Left Running Stack** (`BOTTOM_LEFT`) | Dynamic meta-dock displaying active running processes (PIDs, CPU metrics). | **Dynamic & Transient** (In-Memory) |

---

## 2. Multi-Workspace Architecture & Navigation

StepLauncher supports multiple virtual Workspaces (Workspace 1, Workspace 2, Workspace 3):

- **Workspace Switching**:
  - **Horizontal Swipe Left (`←`)**: Switches to the next workspace (e.g. Workspace 1 → Workspace 2).
  - **Horizontal Swipe Right (`→`)**: Switches to the previous workspace (e.g. Workspace 2 → Workspace 1).
- **Workspace Creation & Deletion**:
  - **`"➕ Add Workspace"`**: Instantiates a new workspace with a default paperclip Anchor (`Workspace N`) and switches to it immediately.
  - **`"🗑️ Remove Current Workspace"`**: Removes the active workspace (if `index > 0`) and returns to the preceding workspace.
  - **Workspace 1 Immutable Constraint**: **`Workspace 1` (index 0) can NEVER be removed.** The removal action is suppressed for Workspace 1.
- **Workspace Docks**:
  - The right-handed dock (`topRightDockTiles`) dynamically renders tiles for the active workspace.
  - Adding or modifying tiles on Workspace 2 affects Workspace 2 only.
- **Global Bottom Dock**:
  - Remains constant across all workspace transitions.

---

## 3. Desktop Surface Context Menu & Layout Lock

Long-pressing on the open desktop wallpaper background (or tapping Dock Anchor options) opens the **Desktop Surface Options**:

1. ℹ️ **About StepLauncher**: Displays About Dialog detailing version, active workspace, layout lock status, and gesture specs.
2. ⚙️ **Dock Settings**: Launches `SettingsActivity`.
3. 🔒 **Lock Layout** / 🔓 **Unlock Layout**: Toggles the `isLayoutLocked` boolean state.

```
                   ┌────────────────────────────────────────┐
                   │ Desktop Surface Long Press / Anchor    │
                   └───────────────────┬────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
│  ℹ️ About Page  │           │ ⚙️ Dock Settings │           │ 🔒/🔓 Lock Layout│
└─────────────────┘           └─────────────────┘           └─────────────────┘
```

---

## 4. Layout Lock Enforcement (`isLayoutLocked`)

When **Layout Lock** is enabled (`isLayoutLocked == true`):

| Operation / Feature | Layout Unlocked (`false`) | Layout Locked (`true`) | Notes |
| :--- | :---: | :---: | :--- |
| **Pinch Zoom Resizing** | **Enabled** | **Disabled** | Pinch gestures resize icons (24dp - 96dp) only when unlocked. |
| **↔️ Move to Dock...** | **Enabled** | **Disabled** | Hidden from tile context menus when locked. |
| **📌 Copy to Dock...** | **Enabled** | **Disabled** | Hidden from tile context menus when locked. |
| **✏️ Edit Title & Icon** | **Enabled** | **Disabled** | Hidden from tile context menus when locked. |
| **🗑️ Remove from Dock** | **Enabled** | **Disabled** | Hidden from tile context menus when locked. |
| **▶️ Launch / Focus** | **Enabled** | **Enabled** | Always functional. |
| **⏹️ Close / Terminate** | **Enabled** | **Enabled** | Always functional for running tasks. |
| **📦 Uninstall App** | **Enabled** | **Enabled** | **Automatically deletes tile whether locked or unlocked.** |

---

## 5. Summary Operations Matrix

| Tile Type | Launch/Focus | Close Task | Copy to Dock* | Move to Dock* | Edit Title* | Remove* | Uninstall |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `AppShortcut` | Yes | No | No | Yes | Yes | Yes | **Yes (Deletes Tile)** |
| `RunningTask` | Yes | **Yes** | Yes | No | Yes | No (Use Close) | No |
| `ExternalDockApp` | Yes | No | No | Yes | Yes | Yes | **Yes (Deletes Tile)** |
| `InternalDockApp` | Yes | No | No | Yes | Yes | Yes | No |
| `VfsCategoryLink` | Yes | No | No | Yes | Yes | Yes | No |

*\* Operations marked with an asterisk are suppressed when `isLayoutLocked == true`.*
