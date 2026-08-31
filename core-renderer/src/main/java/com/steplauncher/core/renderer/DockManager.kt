package com.steplauncher.core.renderer

import android.content.Context
import android.content.Intent
import com.steplauncher.core.ipc.DockAppDescriptor
import com.steplauncher.core.vfs.DefaultAppResolver
import com.steplauncher.core.vfs.VfsCategory
import org.json.JSONArray
import org.json.JSONObject

object DockManager {

    private const val PREFS_NAME = "steplauncher_docks_prefs"
    private const val KEY_BOTTOM = "key_bottom_tiles"
    private const val KEY_ICON_SIZE = "key_tile_icon_size_dp"
    private const val KEY_LAYOUT_LOCKED = "key_layout_locked"
    private const val KEY_WORKSPACE_INDEX = "key_workspace_index"
    private const val KEY_WORKSPACE_PREFIX = "key_workspace_tiles_"
    private const val KEY_ACCENT_COLOR = "key_accent_color"

    const val NUM_WORKSPACES = 3

    // Default icon size set to 56dp (twice as big as previous 28dp/30dp)
    var tileIconSizeDp: Int = 56
    var isLayoutLocked: Boolean = false
    var currentWorkspaceIndex: Int = 0
    var accentColorHex: String = "#FFFFFF" // Default Frosted White

    private val listeners = mutableListOf<() -> Unit>()

    // Workspace Docks: Each workspace has its own independent Right-Handed Workspace Dock
    val workspaceDocks = MutableList(NUM_WORKSPACES) { mutableListOf<DockTile>() }

    // Dynamic getter returning active workspace tiles
    val topRightDockTiles: MutableList<DockTile>
        get() = workspaceDocks[currentWorkspaceIndex]

    val bottomLeftDockTiles = mutableListOf<DockTile>() // Running Tasks Stack (grows up)
    val bottomDockTiles = mutableListOf<DockTile>()     // Bottom Dock = Global Dock

    private var isInitialized = false

    fun addChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyChanged(context: Context? = null) {
        if (context != null) {
            saveState(context)
        }
        listeners.forEach { it.invoke() }
    }

    fun toggleLayoutLock(context: Context? = null): Boolean {
        isLayoutLocked = !isLayoutLocked
        notifyChanged(context)
        return isLayoutLocked
    }

    fun switchToWorkspace(index: Int, context: Context? = null) {
        val targetIndex = index.coerceIn(0, NUM_WORKSPACES - 1)
        if (currentWorkspaceIndex != targetIndex) {
            currentWorkspaceIndex = targetIndex
            notifyChanged(context)
        }
    }

    fun nextWorkspace(context: Context? = null) {
        switchToWorkspace((currentWorkspaceIndex + 1) % NUM_WORKSPACES, context)
    }

    fun prevWorkspace(context: Context? = null) {
        switchToWorkspace((currentWorkspaceIndex - 1 + NUM_WORKSPACES) % NUM_WORKSPACES, context)
    }

    fun updateIconSize(newSizeDp: Int, context: Context) {
        if (isLayoutLocked) return
        tileIconSizeDp = newSizeDp.coerceIn(24, 96)
        notifyChanged(context)
    }

    fun updateAccentColor(colorHex: String, context: Context) {
        accentColorHex = colorHex
        notifyChanged(context)
    }

    fun scaleIconSize(scaleFactor: Float, context: Context? = null) {
        if (isLayoutLocked) return
        val newSize = (tileIconSizeDp * scaleFactor).toInt().coerceIn(24, 96)
        if (newSize != tileIconSizeDp) {
            tileIconSizeDp = newSize
            notifyChanged(context)
        }
    }

    /**
     * Synchronously resets all dock tiles to factory defaults, clears SharedPreferences,
     * removes all extra added items, and repopulates initial default handlers.
     */
    fun resetDocksToDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        workspaceDocks.forEach { it.clear() }
        bottomLeftDockTiles.clear()
        bottomDockTiles.clear()

        tileIconSizeDp = 56 // Reset to 56dp (2x default)
        isLayoutLocked = false
        currentWorkspaceIndex = 0
        accentColorHex = "#FFFFFF"
        isInitialized = true

        populateDefaultTiles(context)
        saveState(context)
        notifyChanged()
    }

    /**
     * Initializes default dock layouts and restores saved permanent launchers.
     */
    fun initializeDefaults(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        tileIconSizeDp = prefs.getInt(KEY_ICON_SIZE, 56)
        isLayoutLocked = prefs.getBoolean(KEY_LAYOUT_LOCKED, false)
        currentWorkspaceIndex = prefs.getInt(KEY_WORKSPACE_INDEX, 0).coerceIn(0, NUM_WORKSPACES - 1)
        accentColorHex = prefs.getString(KEY_ACCENT_COLOR, "#FFFFFF") ?: "#FFFFFF"

        val savedBottom = prefs.getString(KEY_BOTTOM, null)
        val savedWorkspace0 = prefs.getString(KEY_WORKSPACE_PREFIX + "0", null)

        if (savedBottom != null && savedWorkspace0 != null) {
            restoreState(context, prefs)
            initRunningStackDefaults()
            notifyChanged()
            return
        }

        populateDefaultTiles(context)
        saveState(context)
        notifyChanged()
    }

    private fun populateDefaultTiles(context: Context) {
        workspaceDocks.forEach { it.clear() }

        // WORKSPACE 1 DEFAULT DOCK
        workspaceDocks[0].add(DockTile.DockAnchor(id = "dock_anchor_main", title = "Settings", iconSymbol = "⚙️"))
        workspaceDocks[0].add(DockTile.InternalDockApp(id = "wmclock", title = "Clock / Cal", iconSymbol = "⏰", moduleType = "WMCLOCK"))
        workspaceDocks[0].add(DockTile.InternalDockApp(id = "wmbattery", title = "Battery Mon", iconSymbol = "⚡", moduleType = "WMBATTERY"))
        workspaceDocks[0].add(DockTile.InternalDockApp(id = "wmmon", title = "CPU & Net", iconSymbol = "📊", moduleType = "WMMON"))

        // WORKSPACE 2 DEFAULT DOCK
        workspaceDocks[1].add(DockTile.DockAnchor(id = "dock_anchor_ws2", title = "Workspace 2", iconSymbol = "❖"))
        workspaceDocks[1].add(DockTile.VfsCategoryLink(id = "ws2_dev", title = "Development", iconSymbol = "⚡", category = VfsCategory.DEVELOPMENT))
        workspaceDocks[1].add(DockTile.VfsCategoryLink(id = "ws2_prod", title = "Productivity", iconSymbol = "💼", category = VfsCategory.PRODUCTIVITY))

        // WORKSPACE 3 DEFAULT DOCK
        workspaceDocks[2].add(DockTile.DockAnchor(id = "dock_anchor_ws3", title = "Workspace 3", iconSymbol = "❖"))
        workspaceDocks[2].add(DockTile.VfsCategoryLink(id = "ws3_games", title = "Games", iconSymbol = "🎮", category = VfsCategory.MULTIMEDIA))

        // 2. BOTTOM LEFT DOCK (Running Tasks Stack - Grows Upwards)
        initRunningStackDefaults()

        // 3. BOTTOM DOCK (Global Dock - Defaults to Phone, Browser, Camera, Social Media, Multimedia)
        bottomDockTiles.clear()

        // Resolve Phone
        val phoneApp = DefaultAppResolver.resolvePhoneApp(context)
        bottomDockTiles.add(
            DockTile.AppShortcut(
                id = "bottom_phone",
                title = phoneApp.label,
                iconSymbol = phoneApp.iconSymbol,
                packageName = phoneApp.packageName,
                launchIntent = phoneApp.launchIntent
            )
        )

        // Resolve Browser
        val browserApp = DefaultAppResolver.resolveBrowserApp(context)
        bottomDockTiles.add(
            DockTile.AppShortcut(
                id = "bottom_browser",
                title = browserApp.label,
                iconSymbol = browserApp.iconSymbol,
                packageName = browserApp.packageName,
                launchIntent = browserApp.launchIntent
            )
        )

        // Resolve Camera
        val cameraApp = DefaultAppResolver.resolveCameraApp(context)
        bottomDockTiles.add(
            DockTile.AppShortcut(
                id = "bottom_camera",
                title = cameraApp.label,
                iconSymbol = cameraApp.iconSymbol,
                packageName = cameraApp.packageName,
                launchIntent = cameraApp.launchIntent
            )
        )

        // Social Media Link
        bottomDockTiles.add(
            DockTile.VfsCategoryLink(
                id = "bottom_social_media",
                title = VfsCategory.SOCIAL_MEDIA.displayName,
                iconSymbol = VfsCategory.SOCIAL_MEDIA.iconSymbol,
                category = VfsCategory.SOCIAL_MEDIA
            )
        )

        // Multimedia Link
        bottomDockTiles.add(
            DockTile.VfsCategoryLink(
                id = "bottom_multimedia",
                title = VfsCategory.MULTIMEDIA.displayName,
                iconSymbol = VfsCategory.MULTIMEDIA.iconSymbol,
                category = VfsCategory.MULTIMEDIA
            )
        )
    }

    private fun initRunningStackDefaults() {
        bottomLeftDockTiles.clear()
        bottomLeftDockTiles.add(
            DockTile.RunningTask(
                id = "running_sys_telemetry",
                title = "System Telemetry",
                iconSymbol = "⚡",
                packageName = "system.telemetry",
                processId = 1042,
                cpuUsagePercent = 12,
                launchIntent = null
            )
        )
        bottomLeftDockTiles.add(
            DockTile.RunningTask(
                id = "running_vfs_organizer",
                title = "VFS Organizer",
                iconSymbol = "📁",
                packageName = "system.vfs",
                processId = 1088,
                cpuUsagePercent = 4,
                launchIntent = null
            )
        )
    }

    fun registerExternalDockApp(descriptor: DockAppDescriptor, context: Context? = null) {
        val tile = DockTile.ExternalDockApp(
            id = descriptor.id,
            title = descriptor.title,
            iconSymbol = "🚀",
            descriptor = descriptor
        )

        val targetDock = descriptor.targetDock.uppercase()
        when (targetDock) {
            "BOTTOM_LEFT" -> bottomLeftDockTiles.add(tile)
            DockAppDescriptor.TARGET_DOCK_BOTTOM -> bottomDockTiles.add(tile)
            else -> topRightDockTiles.add(tile)
        }
        notifyChanged(context)
    }

    fun getTilesForPosition(position: DockPosition): MutableList<DockTile> {
        return when (position) {
            DockPosition.TOP_RIGHT -> topRightDockTiles
            DockPosition.BOTTOM_LEFT -> bottomLeftDockTiles
            DockPosition.BOTTOM -> bottomDockTiles
        }
    }

    fun removeTile(tile: DockTile, context: Context? = null) {
        workspaceDocks.forEach { it.remove(tile) }
        bottomLeftDockTiles.remove(tile)
        bottomDockTiles.remove(tile)
        notifyChanged(context)
    }

    fun reorderTile(position: DockPosition, fromPosition: Int, toPosition: Int, context: Context? = null) {
        if (isLayoutLocked) return
        val list = getTilesForPosition(position)
        if (fromPosition in list.indices && toPosition in list.indices) {
            val movedItem = list.removeAt(fromPosition)
            list.add(toPosition, movedItem)
            notifyChanged(context)
        }
    }

    fun launchAndAddToRunningStack(
        title: String,
        iconSymbol: String,
        packageName: String,
        launchIntent: Intent?
    ) {
        val existingIndex = bottomLeftDockTiles.indexOfFirst {
            (it is DockTile.RunningTask && it.packageName == packageName)
        }

        if (existingIndex == -1) {
            val pid = (1000..9999).random()
            val cpu = (2..18).random()
            val newRunningTask = DockTile.RunningTask(
                id = "running_" + System.currentTimeMillis(),
                title = title,
                iconSymbol = iconSymbol,
                packageName = packageName,
                processId = pid,
                cpuUsagePercent = cpu,
                launchIntent = launchIntent
            )
            bottomLeftDockTiles.add(newRunningTask)
            notifyChanged()
        }
    }

    fun createLauncherFromRunningTask(
        runningTask: DockTile.RunningTask,
        targetDock: DockPosition,
        context: Context? = null
    ) {
        if (isLayoutLocked) return
        val targetList = getTilesForPosition(targetDock)
        val launcherTile = DockTile.AppShortcut(
            id = "app_perm_" + System.currentTimeMillis(),
            title = runningTask.title,
            iconSymbol = runningTask.iconSymbol,
            packageName = runningTask.packageName,
            launchIntent = runningTask.launchIntent
        )
        targetList.add(launcherTile)
        notifyChanged(context)
    }

    fun moveTileBetweenDocks(
        fromDock: DockPosition,
        fromIndex: Int,
        toDock: DockPosition,
        toIndex: Int,
        context: Context? = null
    ) {
        if (isLayoutLocked) return
        val sourceList = getTilesForPosition(fromDock)
        val targetList = getTilesForPosition(toDock)

        if (fromIndex in sourceList.indices) {
            val item = sourceList.removeAt(fromIndex)
            val insertIndex = toIndex.coerceIn(0, targetList.size)
            targetList.add(insertIndex, item)
            notifyChanged(context)
        }
    }

    fun updateTile(tileId: String, newTitle: String, newIcon: String, context: Context? = null) {
        if (isLayoutLocked) return
        fun updateList(list: MutableList<DockTile>) {
            val index = list.indexOfFirst { it.id == tileId }
            if (index != -1) {
                val current = list[index]
                val updated = when (current) {
                    is DockTile.AppShortcut -> current.copy(title = newTitle, iconSymbol = newIcon)
                    is DockTile.RunningTask -> current.copy(title = newTitle, iconSymbol = newIcon)
                    is DockTile.InternalDockApp -> current.copy(title = newTitle, iconSymbol = newIcon)
                    is DockTile.ExternalDockApp -> current.copy(title = newTitle, iconSymbol = newIcon)
                    is DockTile.VfsCategoryLink -> current.copy(title = newTitle, iconSymbol = newIcon)
                    is DockTile.PlaceholderBox -> current.copy(title = newTitle, iconSymbol = newIcon)
                    is DockTile.DockAnchor -> current.copy(title = newTitle, iconSymbol = newIcon)
                }
                list[index] = updated
            }
        }

        workspaceDocks.forEach { updateList(it) }
        updateList(bottomLeftDockTiles)
        updateList(bottomDockTiles)
        notifyChanged(context)
    }

    private fun saveState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            editor.putInt(KEY_ICON_SIZE, tileIconSizeDp)
            editor.putBoolean(KEY_LAYOUT_LOCKED, isLayoutLocked)
            editor.putInt(KEY_WORKSPACE_INDEX, currentWorkspaceIndex)
            editor.putString(KEY_ACCENT_COLOR, accentColorHex)

            for (w in 0 until NUM_WORKSPACES) {
                val jsonWs = JSONArray()
                workspaceDocks[w].forEach { jsonWs.put(serializeTile(it)) }
                editor.putString(KEY_WORKSPACE_PREFIX + w, jsonWs.toString())
            }

            val jsonBottom = JSONArray()
            bottomDockTiles.forEach { jsonBottom.put(serializeTile(it)) }
            editor.putString(KEY_BOTTOM, jsonBottom.toString())

            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeTile(tile: DockTile): JSONObject {
        val json = JSONObject()
        json.put("id", tile.id)
        json.put("title", tile.title)
        json.put("iconSymbol", tile.iconSymbol)

        when (tile) {
            is DockTile.DockAnchor -> json.put("type", "ANCHOR")
            is DockTile.AppShortcut -> {
                json.put("type", "APP")
                json.put("packageName", tile.packageName)
            }
            is DockTile.InternalDockApp -> {
                json.put("type", "INTERNAL")
                json.put("moduleType", tile.moduleType)
            }
            is DockTile.VfsCategoryLink -> {
                json.put("type", "VFS")
                json.put("category", tile.category.name)
            }
            is DockTile.ExternalDockApp -> {
                json.put("type", "EXTERNAL")
                json.put("packageName", tile.descriptor.packageName)
            }
            else -> json.put("type", "OTHER")
        }
        return json
    }

    private fun restoreState(context: Context, prefs: android.content.SharedPreferences) {
        val pm = context.packageManager
        accentColorHex = prefs.getString(KEY_ACCENT_COLOR, "#FFFFFF") ?: "#FFFFFF"

        fun parseList(jsonStr: String, targetList: MutableList<DockTile>) {
            targetList.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = obj.optString("type")
                val id = obj.optString("id")
                val title = obj.optString("title")
                val icon = obj.optString("iconSymbol")

                when (type) {
                    "ANCHOR" -> targetList.add(DockTile.DockAnchor(id, title, icon))
                    "APP" -> {
                        val pkg = obj.optString("packageName")
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                        targetList.add(DockTile.AppShortcut(id, title, icon, pkg, launchIntent))
                    }
                    "INTERNAL" -> {
                        val mod = obj.optString("moduleType", "WMCLOCK")
                        targetList.add(DockTile.InternalDockApp(id, title, icon, mod))
                    }
                    "VFS" -> {
                        val catStr = obj.optString("category", "SOCIAL_MEDIA")
                        val cat = try { VfsCategory.valueOf(catStr) } catch (e: Exception) { VfsCategory.SOCIAL_MEDIA }
                        targetList.add(DockTile.VfsCategoryLink(id, title, icon, cat))
                    }
                    "EXTERNAL" -> {
                        val pkg = obj.optString("packageName")
                        val descriptor = DockAppDescriptor(
                            id = id,
                            title = title,
                            packageName = pkg,
                            targetDock = "TOP_RIGHT",
                            type = DockAppDescriptor.TYPE_EXTERNAL
                        )
                        targetList.add(DockTile.ExternalDockApp(id, title, icon, descriptor))
                    }
                    else -> {
                        val pkg = obj.optString("packageName")
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                        targetList.add(DockTile.AppShortcut(id, title, icon, pkg, launchIntent))
                    }
                }
            }
        }

        for (w in 0 until NUM_WORKSPACES) {
            val wsJson = prefs.getString(KEY_WORKSPACE_PREFIX + w, null)
            if (wsJson != null) {
                parseList(wsJson, workspaceDocks[w])
            }
        }

        val bottomJson = prefs.getString(KEY_BOTTOM, null)
        if (bottomJson != null) {
            parseList(bottomJson, bottomDockTiles)
        }
    }
}
