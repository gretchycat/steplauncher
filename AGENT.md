### Modular Android Dock & Application System
1. Project Overview & Architectural Vision
This project implements a modular Android environment inspired by classic desktop dock paradigms (such as Window Maker and NeXTSTEP), re-engineered for modern touch interfaces.
Window Maker serves strictly as an interaction and modularity inspiration, not a literal retro skin. The visual language is modernized (clean typography, subtle depth, frosted translucent panels, and structured margins) while preserving core structural principles:
 * The Dock: A pinned, vertical, fixed-width modular rail with variable vertical expansion.
 * Dockapps: Independent, self-contained micro-modules functioning as real-time telemetry monitors, status relays, and launcher anchors.
 * Flag Attachments: Horizontally expandable launcher tabs anchored directly to specific dock slots.
 * Dual Target Strategy: Built from a single unified codebase capable of deploying as either:
   * A full-fledged Android Home Launcher (android.intent.category.HOME).
   * A standalone Android companion widget (AppWidgetProvider), with graceful feature degradation.
2. Core Functional Components
A. The Dock Rail (Mid/Right Screen)
 * Geometry: Fixed horizontal width, dynamic vertical height determined by docked modules.
 * Chassis: Translucent glassmorphism styling with soft edges, housing modular 1:1 or N:1 tiles.
 * Slot Extensions (Flags): Left-protruding tabbed buttons anchored to specific dockapps. These flags provide immediate single-tap launch targets (e.g., Firefox, Signal, Termux) with status badges or unread counters, expandable/collapsible per dock tile.
B. Dockapp Ecosystem
 * Built-in System Telemetry: Compact modules displaying live metrics (CPU load sparklines, network Rx/Tx throughput, battery state, clock/calendar).
 * Decoupled Lifecycle: Dockapps operate as isolated components. In full-launcher mode, they update via high-frequency coroutine flows; in widget mode, they fall back to throttled event broadcasts.
 * Third-Party Extensibility: Standardized IPC interface allowing standalone dockapp APKs to bind to the host and share the same theme specification.
C. The Program Organizer & VFS Engine
The core application manager operates as a specialized, high-priority dockapp backed by a Virtual Filesystem (VFS) engine:
 * Initial Bootstrap Routine:
   * Interrogates the Android PackageManager for all launchable activities (Intent.ACTION_MAIN, CATEGORY_LAUNCHER).
   * Reads system metadata (ApplicationInfo.category) and maps each package into standard category buckets (e.g., Audio, Video, Games, Communication, Productivity, Development, System, Unsorted).
   * Automatically generates a virtual directory tree, populating each category directory with executable link nodes pointing to the corresponding Android ComponentName.
 * Tree Manipulation:
   * Supports runtime graft/prune operations (re-categorization, custom folders, symlink-style multi-placement).
   * Storage backed by an exportable, human-readable plain JSON/config tree structure.
 * Presentation Modes:
   * Tapping the VFS dockapp displays an organized tree/cascading column browser (Miller Columns) for drill-down navigation without falling back to an unsorted flat alphabet drawer.
D. Ancillary Canvas Elements
 * Bottom Launcher Bar: Minimalist horizontal dock housing primary core categories (Phone, Web, Camera, Media, and VFS root trigger).
 * Running Tasks Stack: Subtle vertical stack rising from the bottom-left corner indicating active background processes and active sessions.
 * Open Canvas: Open desktop area capable of hosting standard Android system widgets (AppWidgetHost) alongside custom wallpaper without intersecting the dock's safe area.
3. Codebase Architecture & Build Target Strategy
The system is architected as a multi-module Kotlin project driven by a single shared core.
Module Breakdown
 * :core-vfs: App discovery, category classification engine, and JSON/SQLite tree model.
 * :core-telemetry: Hardware and OS sensor polling (/proc/stat, BatteryManager, ConnectivityManager).
 * :core-renderer: Common Canvas drawing routines, glass shaders, sparkline math, and typography.
 * :core-ipc: AIDL contracts and parcelables for external dockapp discovery and communication.
 * :app-launcher: Home Activity, gesture recognizers, window manager coordination, and AppWidgetHost.
 * :app-widget: AppWidgetProvider, RemoteViews factories, and broadcast receivers.
Dual-Target Interface Abstraction
                  ┌──────────────────────────────┐
                  │    Core State & Data Flow    │
                  │ (VFS Tree, Telemetry, Flags) │
                  └──────────────┬───────────────┘
                                 │
                  ┌──────────────▼──────────────┐
                  │     DockTileRenderer        │
                  │   (Pure Android Canvas)     │
                  └──────────────┬───────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         │                                               │
         ▼                                               ▼
┌─────────────────────────────┐        ┌───────────────────────────────┐
│ HostSurface.LAUNCHER_PROPER │        │  HostSurface.STANDALONE_WIDGET │
├─────────────────────────────┤        ├───────────────────────────────┤
│ • Direct Hardware Canvas    │        │ • Off-screen Bitmap Buffer    │
│ • 60 FPS gesture handling   │        │ • RemoteViews Serializer      │
│ • Real-time (1-5 Hz) poll   │        │ • Event/Wake-throttled update │
│ • Native window overlays    │        │ • PendingIntent action routes │
└─────────────────────────────┘        └───────────────────────────────┘

4. Immediate Development Milestones
 * VFS Initializer:
   * Build the PackageScanner class to read installed apps, resolve categories, and construct the initial JSON directory hierarchy on initial launch.
 * Canvas Render Pipeline:
   * Implement the fixed-width/variable-height Dock renderer and test flag protrusion geometry.
 * Core Dockapps:
   * Build native wmclock, wmbattery, and wmmon (sparkline canvas) modules.
 * Target Bridge:
   * Connect the shared Canvas renderer to both a test Activity view and a RemoteViews widget update routine.

