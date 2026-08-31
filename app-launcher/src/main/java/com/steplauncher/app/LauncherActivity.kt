package com.steplauncher.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.steplauncher.app.databinding.ActivityLauncherBinding
import com.steplauncher.core.renderer.DockManager
import com.steplauncher.core.renderer.DockPosition
import com.steplauncher.core.renderer.DockTile
import com.steplauncher.core.vfs.VfsCategory

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private val dockChangeListener = {
        runOnUiThread {
            refreshDocks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize default dock configurations
        DockManager.initializeDefaults(this)
        DockManager.addChangeListener(dockChangeListener)

        setupWindowInsets()
        setupRecyclerViews()
        setupGestureDetectors()
        refreshDocks()
    }

    override fun onDestroy() {
        super.onDestroy()
        DockManager.removeChangeListener(dockChangeListener)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val topPadding = maxOf(statusBarTop, cutoutTop)
            val bottomPadding = navBarBottom

            view.setPadding(
                systemBars.left,
                topPadding,
                systemBars.right,
                bottomPadding
            )
            insets
        }
    }

    private fun setupGestureDetectors() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!DockManager.isLayoutLocked) {
                    DockManager.scaleIconSize(detector.scaleFactor, this@LauncherActivity)
                    return true
                }
                return false
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showDesktopBackgroundMenu()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX < 0) {
                        // Swipe Left -> Next Workspace
                        DockManager.nextWorkspace(this@LauncherActivity)
                        Toast.makeText(this@LauncherActivity, "❖ Switched to Workspace ${DockManager.currentWorkspaceIndex + 1}", Toast.LENGTH_SHORT).show()
                    } else {
                        // Swipe Right -> Prev Workspace
                        DockManager.prevWorkspace(this@LauncherActivity)
                        Toast.makeText(this@LauncherActivity, "❖ Switched to Workspace ${DockManager.currentWorkspaceIndex + 1}", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        })

        binding.root.setOnTouchListener { _, event ->
            val scaleHandled = scaleGestureDetector.onTouchEvent(event)
            val gestureHandled = gestureDetector.onTouchEvent(event)
            scaleHandled || gestureHandled
        }
    }

    private fun showDesktopBackgroundMenu() {
        val isLocked = DockManager.isLayoutLocked
        val lockLabel = if (isLocked) "🔓 Unlock Layout" else "🔒 Lock Layout"
        val options = mutableListOf<String>()

        if (!isLocked) {
            options.add("➕ Add Workspace")
            if (DockManager.currentWorkspaceIndex > 0) {
                options.add("🗑️ Remove Current Workspace")
            }
        }
        options.add("ℹ️ About StepLauncher")
        options.add("⚙️ Dock Settings")
        options.add(lockLabel)

        MaterialAlertDialogBuilder(this)
            .setTitle("❖ Desktop Surface Options")
            .setItems(options.toTypedArray()) { _, which ->
                val selected = options[which]
                when {
                    selected.contains("Add Workspace") -> {
                        val newIdx = DockManager.addWorkspace(this)
                        Toast.makeText(this, "❖ Created & Switched to Workspace ${newIdx + 1}", Toast.LENGTH_SHORT).show()
                    }
                    selected.contains("Remove Current Workspace") -> {
                        val removedIndex = DockManager.currentWorkspaceIndex + 1
                        if (DockManager.removeCurrentWorkspace(this)) {
                            Toast.makeText(this, "🗑️ Removed Workspace $removedIndex. Switched to Workspace ${DockManager.currentWorkspaceIndex + 1}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Workspace 1 can never be removed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    selected.contains("About StepLauncher") -> showAboutDialog()
                    selected.contains("Dock Settings") -> startActivity(Intent(this, SettingsActivity::class.java))
                    selected.contains("Lock Layout") || selected.contains("Unlock Layout") -> {
                        val locked = DockManager.toggleLayoutLock(this)
                        Toast.makeText(this, if (locked) "🔒 Layout Locked" else "🔓 Layout Unlocked", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showAboutDialog() {
        val lockStatusStr = if (DockManager.isLayoutLocked) "🔒 Locked" else "🔓 Unlocked"
        val currentWs = DockManager.currentWorkspaceIndex + 1
        val totalWs = DockManager.totalWorkspaces
        val message = """
            ❖ StepLauncher v2.0
            
            Unix-Inspired Modular Android Desktop System
            
            • Bottom Dock: Global Dock (All Workspaces)
            • Right Dock: Workspace Dock (Workspace $currentWs of $totalWs)
            • Left Stack: Running Processes Meta-Dock
            
            Gestures & Operational Status:
            • Active Workspace: Workspace $currentWs of $totalWs
            • Layout Lock: $lockStatusStr
            • Configured Icon Size: ${DockManager.tileIconSizeDp}dp
            • Pinch Gesture: Zoom Icon & Label Size (Unlocked)
            • Horizontal Swipe: Switch Workspaces (Left/Right)
            • Desktop Long Press: Surface Context Menu (Add/Remove Workspaces)
            • Tile 500ms Long Press: Tile Actions
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("ℹ️ About StepLauncher")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupRecyclerViews() {
        // Bottom-Left Dock: Running Tasks Stack (Vertical, Grows UPWARDS from bottom left)
        val bottomLeftLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true).apply {
            stackFromEnd = true
        }
        binding.rvDockBottomLeft.layoutManager = bottomLeftLayoutManager

        // Top-Right Dock: Workspace Dock (Vertical)
        binding.rvDockTopRight.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Bottom Main Dock: Global Dock (Horizontal)
        binding.rvDockBottom.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun refreshDocks() {
        // Bottom-Left Dock: Running Tasks Stack
        binding.rvDockBottomLeft.adapter = DockTileAdapter(
            tiles = DockManager.bottomLeftDockTiles,
            onTileClick = { tile -> handleTileClick(tile, DockPosition.BOTTOM_LEFT) },
            onTileLongClickMenu = { tile, _ -> handleTileLongClickMenu(tile, DockPosition.BOTTOM_LEFT) }
        )

        // Top-Right Dock: Active Workspace Dock
        binding.rvDockTopRight.adapter = DockTileAdapter(
            tiles = DockManager.topRightDockTiles,
            onTileClick = { tile -> handleTileClick(tile, DockPosition.TOP_RIGHT) },
            onTileLongClickMenu = { tile, _ -> handleTileLongClickMenu(tile, DockPosition.TOP_RIGHT) }
        )

        // Bottom Main Dock: Global Dock
        binding.rvDockBottom.adapter = DockTileAdapter(
            tiles = DockManager.bottomDockTiles,
            onTileClick = { tile -> handleTileClick(tile, DockPosition.BOTTOM) },
            onTileLongClickMenu = { tile, _ -> handleTileLongClickMenu(tile, DockPosition.BOTTOM) }
        )
    }

    private fun handleTileClick(tile: DockTile, position: DockPosition) {
        when (tile) {
            is DockTile.DockAnchor -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            is DockTile.AppShortcut -> {
                Toast.makeText(this, "Launching ${tile.title}...", Toast.LENGTH_SHORT).show()
                if (tile.launchIntent != null) {
                    try { startActivity(tile.launchIntent) } catch (e: Exception) {}
                }
                DockManager.launchAndAddToRunningStack(tile.title, tile.iconSymbol, tile.packageName, tile.launchIntent)
            }
            is DockTile.RunningTask -> {
                Toast.makeText(this, "Focusing running task: ${tile.title} (PID: ${tile.processId})", Toast.LENGTH_SHORT).show()
                if (tile.launchIntent != null) {
                    try { startActivity(tile.launchIntent) } catch (e: Exception) {}
                }
            }
            is DockTile.VfsCategoryLink -> {
                Toast.makeText(this, "VFS Category: ${tile.title}", Toast.LENGTH_SHORT).show()
            }
            is DockTile.InternalDockApp -> {
                Toast.makeText(this, "Dockapp: ${tile.title} [${tile.moduleType}]", Toast.LENGTH_SHORT).show()
            }
            is DockTile.ExternalDockApp -> {
                Toast.makeText(this, "External Dockapp: ${tile.title}", Toast.LENGTH_SHORT).show()
            }
            is DockTile.PlaceholderBox -> {
                showAddDockAppDialog(position)
            }
        }
    }

    private fun handleTileLongClickMenu(tile: DockTile, position: DockPosition) {
        if (tile is DockTile.DockAnchor) {
            showDockAnchorLongPressMenu(position)
            return
        }
        showTileContextMenu(tile, position)
    }

    private fun showDockAnchorLongPressMenu(targetDock: DockPosition) {
        val isLocked = DockManager.isLayoutLocked
        val lockLabel = if (isLocked) "🔓 Unlock Layout" else "🔒 Lock Layout"
        val options = mutableListOf<String>()

        if (!isLocked) {
            options.add("✏️ Edit Title & Icon")
            options.add("📱 Add App Launcher Shortcut")
            options.add("📊 Add System Telemetry DockApp")
            options.add("📁 Add VFS Category Link")
            options.add("➕ Add Workspace")
            if (DockManager.currentWorkspaceIndex > 0) {
                options.add("🗑️ Remove Current Workspace")
            }
            options.add("🔄 Reset Docks to Defaults")
        }
        options.add(lockLabel)
        options.add("ℹ️ About StepLauncher")
        options.add("⚙️ Open Dock Settings")

        val anchorTile = DockManager.getTilesForPosition(targetDock).firstOrNull { it is DockTile.DockAnchor }

        MaterialAlertDialogBuilder(this)
            .setTitle(anchorTile?.let { "${it.iconSymbol} ${it.title}" } ?: "❖ Workspace Anchor")
            .setItems(options.toTypedArray()) { _, which ->
                val selected = options[which]
                when {
                    selected.contains("Edit Title") && anchorTile != null -> showEditTileDialog(anchorTile)
                    selected.contains("Add App Launcher") -> showAppPickerAddDialog(targetDock)
                    selected.contains("Add System Telemetry") -> showAddTelemetryDockAppDialog(targetDock)
                    selected.contains("Add VFS Category") -> showAddVfsCategoryDialog(targetDock)
                    selected.contains("Add Workspace") -> {
                        val newIdx = DockManager.addWorkspace(this)
                        Toast.makeText(this, "❖ Created & Switched to Workspace ${newIdx + 1}", Toast.LENGTH_SHORT).show()
                    }
                    selected.contains("Remove Current Workspace") -> {
                        val removedIndex = DockManager.currentWorkspaceIndex + 1
                        if (DockManager.removeCurrentWorkspace(this)) {
                            Toast.makeText(this, "🗑️ Removed Workspace $removedIndex. Switched to Workspace ${DockManager.currentWorkspaceIndex + 1}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Workspace 1 can never be removed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    selected.contains("Reset Docks") -> {
                        DockManager.resetDocksToDefaults(this)
                        Toast.makeText(this, "Docks reset to default layout", Toast.LENGTH_SHORT).show()
                    }
                    selected.contains("Lock Layout") || selected.contains("Unlock Layout") -> {
                        val locked = DockManager.toggleLayoutLock(this)
                        Toast.makeText(this, if (locked) "🔒 Layout Locked" else "🔓 Layout Unlocked", Toast.LENGTH_SHORT).show()
                    }
                    selected.contains("About StepLauncher") -> showAboutDialog()
                    selected.contains("Open Dock Settings") -> startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            .show()
    }

    private fun showTileContextMenu(tile: DockTile, currentDock: DockPosition) {
        val isLocked = DockManager.isLayoutLocked
        val actions = mutableListOf<String>()

        // 1. Top-level launch / focus actions
        when (tile) {
            is DockTile.AppShortcut -> {
                actions.add("▶️ Launch Application")
                if (!isLocked) {
                    actions.add("✏️ Edit Title & Icon")
                    actions.add("↔️ Move to Dock...")
                }
            }
            is DockTile.RunningTask -> {
                actions.add("▶️ Switch / Focus Task")
                actions.add("⏹️ Close / Terminate Task")
                if (!isLocked) {
                    actions.add("📌 Copy to Dock...")
                    actions.add("✏️ Edit Title & Icon")
                }
            }
            else -> {
                if (!isLocked) {
                    actions.add("✏️ Edit Title & Icon")
                    actions.add("↔️ Move to Dock...")
                }
            }
        }

        // 2. System App Info and Uninstall Application ONLY apply to installed external apps
        val externalPkgName = when (tile) {
            is DockTile.AppShortcut -> tile.packageName
            is DockTile.ExternalDockApp -> tile.descriptor.packageName
            else -> null
        }

        val isInternalSystemPkg = externalPkgName == null ||
                externalPkgName.startsWith("system.") ||
                externalPkgName.startsWith("module.") ||
                externalPkgName.startsWith("com.steplauncher")

        if ((tile is DockTile.AppShortcut || tile is DockTile.ExternalDockApp) &&
            !externalPkgName.isNullOrEmpty() &&
            externalPkgName.contains(".") &&
            !isInternalSystemPkg
        ) {
            actions.add("ℹ️ System App Info")
            actions.add("📦 Uninstall Application") // Uninstall automatically deletes tile whether locked or not!
        }

        // 3. Remove option for non-running tiles (only available when unlocked)
        if (tile !is DockTile.RunningTask && !isLocked) {
            actions.add("🗑️ Remove from Dock")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("${tile.iconSymbol} ${tile.title}")
            .setItems(actions.toTypedArray()) { _, which ->
                val selected = actions[which]
                when {
                    selected.contains("Launch Application") && tile is DockTile.AppShortcut -> {
                        if (tile.launchIntent != null) {
                            try { startActivity(tile.launchIntent) } catch (e: Exception) {}
                        }
                        DockManager.launchAndAddToRunningStack(tile.title, tile.iconSymbol, tile.packageName, tile.launchIntent)
                    }

                    selected.contains("Switch / Focus Task") && tile is DockTile.RunningTask -> {
                        if (tile.launchIntent != null) {
                            try { startActivity(tile.launchIntent) } catch (e: Exception) {}
                        }
                        Toast.makeText(this, "Focused task: ${tile.title}", Toast.LENGTH_SHORT).show()
                    }

                    selected.contains("Close / Terminate Task") && tile is DockTile.RunningTask -> {
                        DockManager.removeTile(tile, this)
                        Toast.makeText(this, "Terminated task: ${tile.title}", Toast.LENGTH_SHORT).show()
                    }

                    selected.contains("Copy to Dock...") && tile is DockTile.RunningTask -> {
                        showCopyToDockListerDialog(tile)
                    }

                    selected.contains("Move to Dock...") -> {
                        showMoveToDockListerDialog(tile, currentDock)
                    }

                    selected.contains("Edit Title") -> showEditTileDialog(tile)

                    selected.contains("Remove from Dock") -> {
                        DockManager.removeTile(tile, this)
                        Toast.makeText(this, "Removed ${tile.title}", Toast.LENGTH_SHORT).show()
                    }

                    selected.contains("Uninstall") && externalPkgName != null -> {
                        DockManager.removeTile(tile, this) // Automatically removes from dock whether locked or not!
                        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$externalPkgName"))
                        startActivity(intent)
                    }

                    selected.contains("App Info") && externalPkgName != null -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$externalPkgName")
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun showMoveToDockListerDialog(tile: DockTile, currentDock: DockPosition) {
        if (DockManager.isLayoutLocked) return
        val dockOptions = mutableListOf<Pair<String, DockPosition>>()

        if (currentDock != DockPosition.TOP_RIGHT) {
            dockOptions.add(Pair("❖ Workspace Dock", DockPosition.TOP_RIGHT))
        }
        if (currentDock != DockPosition.BOTTOM) {
            dockOptions.add(Pair("❖ Global Bottom Dock", DockPosition.BOTTOM))
        }

        val labels = dockOptions.map { it.first }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Move '${tile.title}' to Dock...")
            .setItems(labels) { _, which ->
                val targetDock = dockOptions[which].second
                val sourceList = DockManager.getTilesForPosition(currentDock)
                val fromIndex = sourceList.indexOf(tile)
                if (fromIndex != -1) {
                    DockManager.moveTileBetweenDocks(currentDock, fromIndex, targetDock, 999, this)
                    Toast.makeText(this, "Moved ${tile.title} to ${targetDock.title}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCopyToDockListerDialog(runningTask: DockTile.RunningTask) {
        if (DockManager.isLayoutLocked) return
        val dockOptions = arrayOf(
            Pair("❖ Workspace Dock", DockPosition.TOP_RIGHT),
            Pair("❖ Global Bottom Dock", DockPosition.BOTTOM)
        )

        val labels = dockOptions.map { it.first }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Copy '${runningTask.title}' Launcher to...")
            .setItems(labels) { _, which ->
                val targetDock = dockOptions[which].second
                DockManager.createLauncherFromRunningTask(runningTask, targetDock, this)
                Toast.makeText(this, "Permanently added ${runningTask.title} launcher to ${targetDock.title}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppPickerAddDialog(targetDock: DockPosition) {
        if (DockManager.isLayoutLocked) return
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
            .sortedBy { it.loadLabel(pm).toString() }

        val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Add App Launcher Shortcut")
            .setItems(labels) { _, which ->
                val resolveInfo = apps[which]
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                val launchIntent = pm.getLaunchIntentForPackage(pkg)

                val newTile = DockTile.AppShortcut(
                    id = "app_" + System.currentTimeMillis(),
                    title = label,
                    iconSymbol = "📱",
                    packageName = pkg,
                    launchIntent = launchIntent
                )

                DockManager.getTilesForPosition(targetDock).add(newTile)
                DockManager.notifyChanged(this)
                Toast.makeText(this, "Added $label to ${targetDock.title}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTelemetryDockAppDialog(targetDock: DockPosition) {
        if (DockManager.isLayoutLocked) return
        val options = arrayOf("⏰ wmclock (Clock & Cal)", "⚡ wmbattery (Battery Mon)", "📊 wmmon (CPU & Net)", "🌐 wmnet (Throughput)")
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Telemetry DockApp")
            .setItems(options) { _, which ->
                val (id, title, icon, type) = when (which) {
                    0 -> Quad("wmclock_${System.currentTimeMillis()}", "wmclock", "⏰", "WMCLOCK")
                    1 -> Quad("wmbattery_${System.currentTimeMillis()}", "wmbattery", "⚡", "WMBATTERY")
                    2 -> Quad("wmmon_${System.currentTimeMillis()}", "wmmon", "📊", "WMMON")
                    else -> Quad("wmnet_${System.currentTimeMillis()}", "wmnet", "🌐", "WMNET")
                }
                val newTile = DockTile.InternalDockApp(id, title, icon, type)
                DockManager.getTilesForPosition(targetDock).add(newTile)
                DockManager.notifyChanged(this)
            }
            .show()
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun showAddVfsCategoryDialog(targetDock: DockPosition) {
        if (DockManager.isLayoutLocked) return
        val categories = VfsCategory.values()
        val labels = categories.map { "${it.iconSymbol} ${it.displayName}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Add VFS Category Link")
            .setItems(labels) { _, which ->
                val cat = categories[which]
                val newTile = DockTile.VfsCategoryLink(
                    id = "vfs_${cat.name}_${System.currentTimeMillis()}",
                    title = cat.displayName,
                    iconSymbol = cat.iconSymbol,
                    category = cat
                )
                DockManager.getTilesForPosition(targetDock).add(newTile)
                DockManager.notifyChanged(this)
            }
            .show()
    }

    private fun showAddDockAppDialog(targetDock: DockPosition) {
        if (DockManager.isLayoutLocked) return
        showDockAnchorLongPressMenu(targetDock)
    }

    private fun showEditTileDialog(tile: DockTile) {
        if (DockManager.isLayoutLocked) return
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etTitle = EditText(this).apply {
            hint = "Title"
            setText(tile.title)
        }
        val etIcon = EditText(this).apply {
            hint = "Icon Symbol (Emoji / Text)"
            setText(tile.iconSymbol)
        }

        layout.addView(etTitle)
        layout.addView(etIcon)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Tile: ${tile.title}")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newIcon = etIcon.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    DockManager.updateTile(tile.id, newTitle, newIcon.ifEmpty { "📱" }, this)
                    Toast.makeText(this, "Tile updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
