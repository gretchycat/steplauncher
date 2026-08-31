package com.steplauncher.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.steplauncher.core.vfs.BatteryUtils
import com.steplauncher.core.vfs.SysMonUtils
import com.steplauncher.core.vfs.VfsCategory
import com.steplauncher.widget.WorkspaceWidgetHostManager
import com.steplauncher.widget.WorkspaceWidgetInfo

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var pendingAppWidgetId: Int = -1

    private val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingAppWidgetId != -1) {
            val data = result.data
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId) ?: pendingAppWidgetId
            val providerInfo = WorkspaceWidgetHostManager.getAppWidgetProviderInfo(appWidgetId)
            
            if (providerInfo != null) {
                val widgetInfo = WorkspaceWidgetInfo(
                    id = "widget_${System.currentTimeMillis()}",
                    appWidgetId = appWidgetId,
                    workspaceIndex = DockManager.currentWorkspaceIndex,
                    providerPackageName = providerInfo.provider.packageName,
                    providerClassName = providerInfo.provider.className,
                    xDp = 16,
                    yDp = 16,
                    widthDp = (providerInfo.minWidth / resources.displayMetrics.density).toInt().coerceAtLeast(180),
                    heightDp = (providerInfo.minHeight / resources.displayMetrics.density).toInt().coerceAtLeast(120)
                )
                WorkspaceWidgetHostManager.addWidget(widgetInfo, this)
                renderWorkspaceWidgetCanvas(DockManager.currentWorkspaceIndex)
                Toast.makeText(this, "🧩 Added Android Widget to Workspace ${DockManager.currentWorkspaceIndex + 1}", Toast.LENGTH_SHORT).show()
            }
        } else if (pendingAppWidgetId != -1) {
            WorkspaceWidgetHostManager.deleteAppWidgetId(pendingAppWidgetId, this)
        }
        pendingAppWidgetId = -1
    }

    private val dockChangeListener = {
        runOnUiThread {
            refreshDocks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize default dock configurations and widget host
        DockManager.initializeDefaults(this)
        WorkspaceWidgetHostManager.init(this)

        DockManager.addChangeListener(dockChangeListener)

        setupWindowInsets()
        setupRecyclerViews()
        setupGestureDetectors()
        refreshDocks()
    }

    private val clockTickerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clockTickerRunnable = object : Runnable {
        override fun run() {
            SysMonUtils.sampleTelemetry(this@LauncherActivity)
            refreshDocks()
            clockTickerHandler.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        clockTickerHandler.post(clockTickerRunnable)
    }

    override fun onPause() {
        super.onPause()
        clockTickerHandler.removeCallbacks(clockTickerRunnable)
    }

    override fun onStart() {
        super.onStart()
        WorkspaceWidgetHostManager.startListening()
    }

    override fun onStop() {
        super.onStop()
        WorkspaceWidgetHostManager.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockTickerHandler.removeCallbacks(clockTickerRunnable)
        DockManager.removeChangeListener(dockChangeListener)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 120 && Math.abs(velocityX) > 200) {
                    if (diffX < 0) {
                        DockManager.nextWorkspace(this@LauncherActivity)
                        Toast.makeText(this@LauncherActivity, "❖ Workspace ${DockManager.currentWorkspaceIndex + 1} of ${DockManager.totalWorkspaces}", Toast.LENGTH_SHORT).show()
                    } else {
                        DockManager.prevWorkspace(this@LauncherActivity)
                        Toast.makeText(this@LauncherActivity, "❖ Workspace ${DockManager.currentWorkspaceIndex + 1} of ${DockManager.totalWorkspaces}", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        })

        com.steplauncher.core.renderer.ForgivingTouchHelper.bind(
            view = binding.root,
            onLongClick = {
                showDesktopBackgroundMenu()
                true
            }
        )
    }

    private fun showDesktopBackgroundMenu() {
        val isLocked = DockManager.isLayoutLocked
        val lockLabel = if (isLocked) "🔓 Unlock Layout" else "🔒 Lock Layout"
        val options = mutableListOf<String>()

        if (!isLocked) {
            options.add("🧩 Add Android Widget")
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
                    selected.contains("Add Android Widget") -> launchWidgetPicker()
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

    private fun launchWidgetPicker() {
        if (DockManager.isLayoutLocked) return
        pendingAppWidgetId = WorkspaceWidgetHostManager.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId)
        }
        try {
            widgetPickLauncher.launch(pickIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "AppWidget picker not available on this device", Toast.LENGTH_SHORT).show()
        }
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
            • Background Canvas: Per-Workspace Android AppWidgets
            
            Gestures & Operational Status:
            • Active Workspace: Workspace $currentWs of $totalWs
            • Layout Lock: $lockStatusStr
            • Configured Icon Size: ${DockManager.tileIconSizeDp}dp
            • Pinch Gesture: Zoom Icon & Label Size (Unlocked)
            • Horizontal Swipe: Switch Workspaces (Left/Right)
            • Desktop Long Press: Add Widgets / Add-Remove Workspaces
            • Tile 500ms Long Press: Tile Context Actions
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("ℹ️ About StepLauncher")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupRecyclerViews() {
        val bottomLeftLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true).apply {
            stackFromEnd = true
        }
        binding.rvDockBottomLeft.layoutManager = bottomLeftLayoutManager
        binding.rvDockTopRight.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvDockBottom.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun refreshDocks() {
        // Render active workspace widget canvas
        renderWorkspaceWidgetCanvas(DockManager.currentWorkspaceIndex)

        // Bottom-Left Dock: Running Tasks Stack (Hide container and RUNNING header if empty)
        val hasRunningTasks = DockManager.bottomLeftDockTiles.isNotEmpty()
        binding.dockBottomLeftContainer.visibility = if (hasRunningTasks) View.VISIBLE else View.GONE

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

    private fun renderWorkspaceWidgetCanvas(workspaceIndex: Int) {
        binding.flWorkspaceWidgetCanvas.removeAllViews()
        val widgets = WorkspaceWidgetHostManager.getWidgetsForWorkspace(workspaceIndex)
        val density = resources.displayMetrics.density

        for (info in widgets) {
            val hostView = WorkspaceWidgetHostManager.createHostView(this, info) ?: continue
            val widthPx = (info.widthDp * density).toInt()
            val heightPx = (info.heightDp * density).toInt()
            val xPx = (info.xDp * density).toInt()
            val yPx = (info.yDp * density).toInt()

            val lp = FrameLayout.LayoutParams(widthPx, heightPx).apply {
                leftMargin = xPx
                topMargin = yPx
            }
            hostView.layoutParams = lp

            // Forgiving long-press handler to remove widget from workspace
            com.steplauncher.core.renderer.ForgivingTouchHelper.bind(
                view = hostView,
                onLongClick = {
                    if (!DockManager.isLayoutLocked) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("🧩 Remove Android Widget")
                            .setMessage("Remove this widget from Workspace ${workspaceIndex + 1}?")
                            .setPositiveButton("Remove") { _, _ ->
                                WorkspaceWidgetHostManager.deleteAppWidgetId(info.appWidgetId, this)
                                renderWorkspaceWidgetCanvas(workspaceIndex)
                                Toast.makeText(this, "Widget removed", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                        true
                    } else false
                }
            )

            binding.flWorkspaceWidgetCanvas.addView(hostView)
        }
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
                if (tile.moduleType.equals("WMCLOCK", ignoreCase = true)) {
                    val clockApp = com.steplauncher.core.vfs.DefaultAppResolver.resolveClockApp(this)
                    Toast.makeText(this, "⏰ Opening ${clockApp.label}...", Toast.LENGTH_SHORT).show()
                    try {
                        startActivity(clockApp.launchIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unable to launch Clock application", Toast.LENGTH_SHORT).show()
                    }
                    DockManager.launchAndAddToRunningStack(clockApp.label, "⏰", clockApp.packageName, clockApp.launchIntent)
                } else if (tile.moduleType.equals("WMBATTERY", ignoreCase = true)) {
                    showExtendedBatteryDialog()
                } else if (tile.moduleType.equals("WMMON", ignoreCase = true) || tile.moduleType.equals("TELEMETRY", ignoreCase = true)) {
                    val nextMode = DockManager.cycleWmMonMode(tile.id, this)
                    val modeLabel = when (nextMode) {
                        0 -> "💻 CPU Monitor Graph"
                        1 -> "📊 Memory (RAM) Graph"
                        2 -> "💾 Storage (Disk) Graph"
                        else -> "🌐 Wireless & Network Graph"
                    }
                    Toast.makeText(this, "Switched to $modeLabel", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Dockapp: ${tile.title} [${tile.moduleType}]", Toast.LENGTH_SHORT).show()
                }
            }
            is DockTile.ExternalDockApp -> {
                Toast.makeText(this, "External Dockapp: ${tile.title}", Toast.LENGTH_SHORT).show()
            }
            is DockTile.PlaceholderBox -> {
                showAddDockAppDialog(position)
            }
        }
    }

    private fun showExtendedBatteryDialog() {
        val bat = BatteryUtils.getBatteryStatus(this)
        val sb = StringBuilder()
        sb.append("🔋 Charge Level: ${bat.levelPercent}%\n")
        sb.append("⚡ Power Source: ${if (bat.isCharging) "Charging (${bat.chargePlugStr})" else "Discharging (On Battery)"}\n")
        sb.append("🩺 Battery Health: ${bat.healthStr}\n")
        sb.append("🌡️ Temperature: ${bat.tempCelsius} °C\n")
        sb.append("⚡ Voltage: ${bat.voltageMv} mV (${bat.technology})\n\n")

        sb.append("🎧 Connected Bluetooth Devices:\n")
        if (bat.connectedDeviceBatteries.isNotEmpty()) {
            bat.connectedDeviceBatteries.forEach { (deviceName, level) ->
                sb.append("  • $deviceName: $level%\n")
            }
        } else {
            sb.append("  • No connected battery devices detected\n")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("🔋 Extended Battery Information")
            .setMessage(sb.toString().trimEnd())
            .setPositiveButton("OK", null)
            .setNeutralButton("⚙️ Battery Settings") { _, _ ->
                openSystemBatterySettings()
            }
            .show()
    }

    private fun showWmMonDetailedDialog(tileId: String) {
        val currentMode = DockManager.getWmMonMode(tileId)
        val density = resources.displayMetrics.density
        val graphW = (320 * density).toInt()
        val graphH = (180 * density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val ivGraph = android.widget.ImageView(this).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        val tvInfo = TextView(this).apply {
            textSize = 14f
            setPadding(0, 20, 0, 10)
        }

        when (currentMode) {
            0 -> { // CPU Mode
                val cpu = SysMonUtils.getCpuMetrics()
                val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawDetailedTelemetryGraphWithAxes(
                    graphW, graphH, SysMonUtils.cpuHistory, "CPU Load History (10 Min)", "%",
                    DockManager.graphicColorHex, DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                )
                ivGraph.setImageBitmap(bmp)
                tvInfo.text = "💻 CPU Load: ${cpu.cpuPercent}%\n⚡ Cores: ${cpu.numCores} Active (${cpu.architecture})\n📈 Telemetry: ${SysMonUtils.cpuHistory.size} / 600 Samples"

                layout.addView(ivGraph)
                layout.addView(tvInfo)

                MaterialAlertDialogBuilder(this)
                    .setTitle("💻 CPU Hardware Status")
                    .setView(layout)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("⚙️ CPU Settings") { _, _ -> openSystemCpuSettings() }
                    .show()
            }
            1 -> { // Memory Mode
                val mem = SysMonUtils.getMemoryMetrics(this)
                val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawDetailedTelemetryGraphWithAxes(
                    graphW, graphH, SysMonUtils.memoryHistory, "RAM Usage History (10 Min)", "%",
                    DockManager.graphicColorHex, DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                )
                ivGraph.setImageBitmap(bmp)
                tvInfo.text = "📊 RAM Utilization: ${mem.ramUsagePercent}%\n💾 Allocated: ${mem.usedRamMb} MB / ${mem.totalRamMb} MB\n📈 Telemetry: ${SysMonUtils.memoryHistory.size} / 600 Samples"

                layout.addView(ivGraph)
                layout.addView(tvInfo)

                MaterialAlertDialogBuilder(this)
                    .setTitle("📊 System Memory (RAM) Status")
                    .setView(layout)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("⚙️ Memory Settings") { _, _ -> openSystemCpuSettings() }
                    .show()
            }
            2 -> { // Storage Mode
                val storage = SysMonUtils.getStorageMetrics(this)
                val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawDetailedTelemetryGraphWithAxes(
                    graphW, graphH, SysMonUtils.storageHistory, "Disk Capacity History (10 Min)", "%",
                    DockManager.graphicColorHex, DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                )
                ivGraph.setImageBitmap(bmp)

                val sb = StringBuilder()
                sb.append("💾 Disk Used: ${storage.storagePercentUsed}%\n")
                sb.append("📁 Available Storage Locations:\n")
                storage.storageLocations.forEach { (locName, sizes) ->
                    val (freeGb, totalGb) = sizes
                    sb.append("  • $locName: ${String.format("%.1f", freeGb)} GB Free / ${String.format("%.1f", totalGb)} GB Total\n")
                }
                tvInfo.text = sb.toString().trimEnd()

                layout.addView(ivGraph)
                layout.addView(tvInfo)

                MaterialAlertDialogBuilder(this)
                    .setTitle("💾 System Storage Locations")
                    .setView(layout)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("⚙️ Storage Settings") { _, _ -> openSystemStorageSettings() }
                    .show()
            }
            3 -> { // Network Mode
                val net = SysMonUtils.getNetworkMetrics(this)
                val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawDetailedTelemetryGraphWithAxes(
                    graphW, graphH, SysMonUtils.rxHistory, "Network Downstream (10 Min)", "KB/s",
                    DockManager.graphicColorHex, DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                )
                ivGraph.setImageBitmap(bmp)

                val sb = StringBuilder()
                sb.append("🌐 Primary IP: ${net.ipAddress}\n")
                sb.append("↓ Rx Throughput: ${net.rxRateKbps} KB/s  |  ↑ Tx Throughput: ${net.txRateKbps} KB/s\n")
                sb.append("🔌 Active Interfaces:\n")
                net.activeInterfaces.forEach { iface ->
                    sb.append("  • $iface\n")
                }
                tvInfo.text = sb.toString().trimEnd()

                layout.addView(ivGraph)
                layout.addView(tvInfo)

                MaterialAlertDialogBuilder(this)
                    .setTitle("🌐 Wireless & Network Status")
                    .setView(layout)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("⚙️ Network Settings") { _, _ -> openSystemNetworkSettings() }
                    .show()
            }
        }
    }

    private fun openSystemBatterySettings() {
        try {
            startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            } catch (e2: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (e3: Exception) {
                    Toast.makeText(this, "Unable to launch Battery Settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openSystemStorageSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, "Unable to launch Storage Settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSystemNetworkSettings() {
        try {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (e2: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (e3: Exception) {
                    Toast.makeText(this, "Unable to launch Network Settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openSystemCpuSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, "Unable to launch System Settings", Toast.LENGTH_SHORT).show()
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
            options.add("🧩 Add Android Widget")
            options.add("📱 Add App Launcher Shortcut")
            options.add("📊 Add Hardware Telemetry Graph DockApp")
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
                    selected.contains("Add Android Widget") -> launchWidgetPicker()
                    selected.contains("Add App Launcher") -> showAppPickerAddDialog(targetDock)
                    selected.contains("Add Hardware Telemetry") || selected.contains("Add System Telemetry") -> showAddTelemetryDockAppDialog(targetDock)
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

        when (tile) {
            is DockTile.AppShortcut -> {
                actions.add("🚀 Launch Application")
                if (!isLocked) {
                    actions.add("↔️ Move to Dock...")
                    actions.add("✏️ Edit Title & Icon")
                    actions.add("🗑️ Remove from Dock")
                }
                if (!isInternalSystemPkg(tile.packageName)) {
                    actions.add("ℹ️ System App Info")
                    actions.add("📦 Uninstall Application")
                }
            }
            is DockTile.RunningTask -> {
                actions.add("🔍 Focus Running Process")
                if (!isLocked) {
                    actions.add("📌 Copy to Dock...")
                }
                actions.add("❌ Terminate Process")
                if (!isInternalSystemPkg(tile.packageName)) {
                    actions.add("ℹ️ System App Info")
                    actions.add("📦 Uninstall Application")
                }
            }
            is DockTile.VfsCategoryLink -> {
                actions.add("📂 Open VFS Category")
                if (!isLocked) {
                    actions.add("↔️ Move to Dock...")
                    actions.add("✏️ Edit Title & Icon")
                    actions.add("🗑️ Remove from Dock")
                }
            }
            is DockTile.InternalDockApp -> {
                if (tile.moduleType.equals("WMCLOCK", ignoreCase = true)) {
                    actions.add("🚀 Open Clock Application")
                    actions.add("⚙️ Configure Time & Date Format")
                } else if (tile.moduleType.equals("WMBATTERY", ignoreCase = true)) {
                    actions.add("📊 Extended Battery Information")
                    actions.add("🔋 Open System Battery Settings")
                } else if (tile.moduleType.equals("WMMON", ignoreCase = true) || tile.moduleType.equals("TELEMETRY", ignoreCase = true)) {
                    val currentMode = DockManager.getWmMonMode(tile.id)
                    actions.add("🔄 Cycle Graph Mode (CPU / RAM / Disk / Net)")
                    actions.add("🔍 Detailed Resource Breakdown")
                    when (currentMode) {
                        0 -> actions.add("💻 Open System CPU Settings")
                        1 -> actions.add("📊 Open System Memory Settings")
                        2 -> actions.add("💾 Open System Storage Settings")
                        3 -> actions.add("🌐 Open Wireless & Network Settings")
                    }
                } else {
                    actions.add("⚙️ Configure DockApp")
                }
                if (!isLocked) {
                    actions.add("↔️ Move to Dock...")
                    actions.add("🗑️ Remove from Dock")
                }
            }
            is DockTile.ExternalDockApp -> {
                actions.add("🚀 Open External DockApp")
                if (!isLocked) {
                    actions.add("↔️ Move to Dock...")
                    actions.add("🗑️ Remove from Dock")
                }
                if (!isInternalSystemPkg(tile.descriptor.packageName)) {
                    actions.add("ℹ️ System App Info")
                    actions.add("📦 Uninstall Application")
                }
            }
            else -> {}
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("${tile.iconSymbol} ${tile.title}")
            .setItems(actions.toTypedArray()) { _, which ->
                val selected = actions[which]
                when {
                    selected.contains("Launch Application") -> handleTileClick(tile, currentDock)
                    selected.contains("Open Clock Application") -> handleTileClick(tile, currentDock)
                    selected.contains("Configure Time & Date Format") -> showClockDockAppConfigDialog()
                    selected.contains("Extended Battery Information") -> showExtendedBatteryDialog()
                    selected.contains("Open System Battery Settings") -> openSystemBatterySettings()
                    selected.contains("Cycle Graph Mode") -> {
                        if (tile is DockTile.InternalDockApp) handleTileClick(tile, currentDock)
                    }
                    selected.contains("Detailed Resource Breakdown") -> {
                        if (tile is DockTile.InternalDockApp) showWmMonDetailedDialog(tile.id)
                    }
                    selected.contains("Open System CPU Settings") -> openSystemCpuSettings()
                    selected.contains("Open System Memory Settings") -> openSystemCpuSettings()
                    selected.contains("Open System Storage Settings") -> openSystemStorageSettings()
                    selected.contains("Open Wireless & Network Settings") -> openSystemNetworkSettings()
                    selected.contains("Focus Running Process") -> handleTileClick(tile, currentDock)
                    selected.contains("Open VFS Category") -> handleTileClick(tile, currentDock)
                    selected.contains("Open External DockApp") -> handleTileClick(tile, currentDock)
                    selected.contains("Configure DockApp") -> handleTileClick(tile, currentDock)
                    selected.contains("Move to Dock") -> showMoveTileDialog(tile, currentDock)
                    selected.contains("Copy to Dock") && tile is DockTile.RunningTask -> showCopyTaskDialog(tile)
                    selected.contains("Edit Title") -> showEditTileDialog(tile)
                    selected.contains("Remove from Dock") -> {
                        DockManager.removeTile(tile, this)
                        Toast.makeText(this, "Removed from dock", Toast.LENGTH_SHORT).show()
                    }
                    selected.contains("Terminate Process") -> {
                        DockManager.removeTile(tile, this)
                        Toast.makeText(this, "Terminated process", Toast.LENGTH_SHORT).show()
                    }
                    selected.contains("System App Info") -> openSystemAppInfo(tile)
                    selected.contains("Uninstall Application") -> uninstallApplication(tile)
                }
            }
            .show()
    }

    private fun showClockDockAppConfigDialog() {
        val timePresets = arrayOf(
            "24-Hour (e.g. 22:50)",
            "12-Hour AM/PM (e.g. 10:50 PM)",
            "24-Hour with Seconds (e.g. 22:50:15)",
            "12-Hour with Seconds (e.g. 10:50:15 PM)"
        )
        val timeFmtValues = arrayOf("HH:mm", "hh:mm a", "HH:mm:ss", "hh:mm:ss a")

        val datePresets = arrayOf(
            "Short Date (e.g. Mon, Aug 31)",
            "US Numeric (e.g. 08/31/2026)",
            "ISO Standard (e.g. 2026-08-31)",
            "Full Date (e.g. Monday, August 31)"
        )
        val dateFmtValues = arrayOf("EEE, MMM d", "MM/dd/yyyy", "yyyy-MM-dd", "EEEE, MMMM d")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val tvTimeHeader = TextView(this).apply {
            text = "⏰ Time Format:"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 10, 0, 10)
        }
        val spinnerTime = Spinner(this)
        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timePresets)
        spinnerTime.adapter = timeAdapter
        val currentTimeIdx = timeFmtValues.indexOf(DockManager.clockTimeFormat).coerceAtLeast(0)
        spinnerTime.setSelection(currentTimeIdx)

        val tvDateHeader = TextView(this).apply {
            text = "📅 Date Format:"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 30, 0, 10)
        }
        val spinnerDate = Spinner(this)
        val dateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, datePresets)
        spinnerDate.adapter = dateAdapter
        val currentDateIdx = dateFmtValues.indexOf(DockManager.clockDateFormat).coerceAtLeast(0)
        spinnerDate.setSelection(currentDateIdx)

        layout.addView(tvTimeHeader)
        layout.addView(spinnerTime)
        layout.addView(tvDateHeader)
        layout.addView(spinnerDate)

        MaterialAlertDialogBuilder(this)
            .setTitle("⚙️ Clock DockApp Settings")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val selectedTimeFmt = timeFmtValues[spinnerTime.selectedItemPosition]
                val selectedDateFmt = dateFmtValues[spinnerDate.selectedItemPosition]
                DockManager.updateClockFormats(selectedTimeFmt, selectedDateFmt, this)
                Toast.makeText(this, "Clock DockApp format updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isInternalSystemPkg(pkgName: String): Boolean {
        return pkgName.startsWith("system.") ||
               pkgName.startsWith("module.") ||
               pkgName.startsWith("com.steplauncher")
    }

    private fun openSystemAppInfo(tile: DockTile) {
        val pkgName = when (tile) {
            is DockTile.AppShortcut -> tile.packageName
            is DockTile.RunningTask -> tile.packageName
            is DockTile.ExternalDockApp -> tile.descriptor.packageName
            else -> null
        }
        if (pkgName != null) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", pkgName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open App Info for $pkgName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uninstallApplication(tile: DockTile) {
        val pkgName = when (tile) {
            is DockTile.AppShortcut -> tile.packageName
            is DockTile.RunningTask -> tile.packageName
            is DockTile.ExternalDockApp -> tile.descriptor.packageName
            else -> null
        }
        if (pkgName != null) {
            try {
                DockManager.removeTile(tile, this)
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.fromParts("package", pkgName, null)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to launch uninstaller for $pkgName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMoveTileDialog(tile: DockTile, fromDock: DockPosition) {
        val options = arrayOf("Top Right Workspace Dock", "Bottom Global Dock")
        MaterialAlertDialogBuilder(this)
            .setTitle("Move Tile To:")
            .setItems(options) { _, which ->
                val targetDock = if (which == 0) DockPosition.TOP_RIGHT else DockPosition.BOTTOM
                val fromList = DockManager.getTilesForPosition(fromDock)
                val fromIndex = fromList.indexOf(tile)
                if (fromIndex != -1) {
                    DockManager.moveTileBetweenDocks(fromDock, fromIndex, targetDock, 0, this)
                }
            }
            .show()
    }

    private fun showCopyTaskDialog(runningTask: DockTile.RunningTask) {
        val options = arrayOf("Top Right Workspace Dock", "Bottom Global Dock")
        MaterialAlertDialogBuilder(this)
            .setTitle("Copy Running Task To:")
            .setItems(options) { _, which ->
                val targetDock = if (which == 0) DockPosition.TOP_RIGHT else DockPosition.BOTTOM
                DockManager.createLauncherFromRunningTask(runningTask, targetDock, this)
                Toast.makeText(this, "Permanently added launcher to target dock!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showEditTileDialog(tile: DockTile) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etTitle = EditText(this).apply {
            hint = "Tile Title"
            setText(tile.title)
        }
        val etIcon = EditText(this).apply {
            hint = "Emoji / Icon Symbol"
            setText(tile.iconSymbol)
        }

        layout.addView(etTitle)
        layout.addView(etIcon)

        MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Edit Tile Details")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = etTitle.text.toString().ifEmpty { tile.title }
                val newIcon = etIcon.text.toString().ifEmpty { tile.iconSymbol }
                DockManager.updateTile(tile.id, newTitle, newIcon, this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDockAppDialog(targetDock: DockPosition) {
        val options = arrayOf(
            "📱 App Launcher Shortcut",
            "📊 Hardware Graph Telemetry DockApp (WMMON)",
            "📁 VFS Category Link"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("➕ Add Dock Item")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAppPickerAddDialog(targetDock)
                    1 -> showAddTelemetryDockAppDialog(targetDock)
                    2 -> showAddVfsCategoryDialog(targetDock)
                }
            }
            .show()
    }

    private fun showAppPickerAddDialog(targetDock: DockPosition) {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            .sortedBy { it.loadLabel(pm).toString() }

        val appNames = resolveInfos.map { it.loadLabel(pm).toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select App to Add")
            .setItems(appNames) { _, which ->
                val selected = resolveInfos[which]
                val label = selected.loadLabel(pm).toString()
                val pkgName = selected.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)

                val newTile = DockTile.AppShortcut(
                    id = "app_" + System.currentTimeMillis(),
                    title = label,
                    iconSymbol = "📱",
                    packageName = pkgName,
                    launchIntent = launchIntent
                )

                val targetList = DockManager.getTilesForPosition(targetDock)
                targetList.add(newTile)
                DockManager.notifyChanged(this)
            }
            .show()
    }

    private fun showAddTelemetryDockAppDialog(targetDock: DockPosition) {
        val dockApps = arrayOf(
            "📈 Telemetry Graphs (CPU / Memory / Storage / Network)",
            "⏰ Date & Time DockApp (WMCLOCK)",
            "🔋 Battery Monitor DockApp (WMBATTERY)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Hardware DockApp")
            .setItems(dockApps) { _, which ->
                val (title, moduleType, icon) = when (which) {
                    0 -> Triple("Hardware Telemetry", "WMMON", "📈")
                    1 -> Triple("Date & Time", "WMCLOCK", "⏰")
                    else -> Triple("Battery Mon", "WMBATTERY", "🔋")
                }
                val newTile = DockTile.InternalDockApp(
                    id = "dockapp_" + System.currentTimeMillis(),
                    title = title,
                    iconSymbol = icon,
                    moduleType = moduleType
                )
                val targetList = DockManager.getTilesForPosition(targetDock)
                targetList.add(newTile)
                DockManager.notifyChanged(this)
            }
            .show()
    }

    private fun showAddVfsCategoryDialog(targetDock: DockPosition) {
        val categories = VfsCategory.values()
        val categoryNames = categories.map { "${it.iconSymbol} ${it.displayName}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select VFS Category Link")
            .setItems(categoryNames) { _, which ->
                val cat = categories[which]
                val newTile = DockTile.VfsCategoryLink(
                    id = "vfs_" + System.currentTimeMillis(),
                    title = cat.displayName,
                    iconSymbol = cat.iconSymbol,
                    category = cat
                )
                val targetList = DockManager.getTilesForPosition(targetDock)
                targetList.add(newTile)
                DockManager.notifyChanged(this)
            }
            .show()
    }
}
