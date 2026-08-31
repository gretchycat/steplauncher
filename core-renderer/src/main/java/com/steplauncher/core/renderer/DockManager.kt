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
    private const val KEY_NUM_WORKSPACES = "key_num_workspaces"
    private const val KEY_WORKSPACE_PREFIX = "key_workspace_tiles_"
    private const val KEY_ACCENT_COLOR = "key_accent_color"
    private const val KEY_ATTENTION_COLOR = "key_attention_color"
    private const val KEY_TEXT_COLOR = "key_text_color"
    private const val KEY_GRAPHIC_COLOR = "key_graphic_color"
    private const val KEY_COLOR_HIGH = "key_color_high"
    private const val KEY_COLOR_MED = "key_color_med"
    private const val KEY_COLOR_LOW = "key_color_low"
    private const val KEY_CLOCK_TIME_FMT = "key_clock_time_fmt"
    private const val KEY_CLOCK_DATE_FMT = "key_clock_date_fmt"

    // Default icon size set to 56dp (twice as big as previous 28dp/30dp)
    var tileIconSizeDp: Int = 56
    var isLayoutLocked: Boolean = false
    var currentWorkspaceIndex: Int = 0
    var accentColorHex: String = "#FFFFFF"      // Default Frosted White
    var attentionColorHex: String = "#FF3D00"   // Attention Request Tint (Flashing Amber / Neon Red)
    var textColorHex: String = "#FFFFFF"        // Text Accent Color
    var graphicColorHex: String = "#00E5FF"     // Graphic Accent Color
    var colorHighHex: String = "#FF5252"        // High Threshold Alert Color
    var colorMedHex: String = "#FFD700"         // Medium Threshold Color
    var colorLowHex: String = "#00E676"         // Low Threshold Normal Color
    var clockTimeFormat: String = "HH:mm"
    var clockDateFormat: String = "EEE, MMM d"

    private val listeners = mutableListOf<() -> Unit>()

    // Workspace Docks: Dynamic list of workspace dock lists
    val workspaceDocks = mutableListOf<MutableList<DockTile>>()

    val totalWorkspaces: Int get() = workspaceDocks.size

    // Dynamic getter returning active workspace tiles
    val topRightDockTiles: MutableList<DockTile>
        get() {
            if (workspaceDocks.isEmpty()) {
                workspaceDocks.add(mutableListOf())
            }
            return workspaceDocks[currentWorkspaceIndex.coerceIn(0, workspaceDocks.size - 1)]
        }

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
        if (workspaceDocks.isEmpty()) return
        val targetIndex = index.coerceIn(0, workspaceDocks.size - 1)
        if (currentWorkspaceIndex != targetIndex) {
            currentWorkspaceIndex = targetIndex
            notifyChanged(context)
        }
    }

    fun nextWorkspace(context: Context? = null) {
        if (workspaceDocks.isEmpty()) return
        switchToWorkspace((currentWorkspaceIndex + 1) % workspaceDocks.size, context)
    }

    fun prevWorkspace(context: Context? = null) {
        if (workspaceDocks.isEmpty()) return
        switchToWorkspace((currentWorkspaceIndex - 1 + workspaceDocks.size) % workspaceDocks.size, context)
    }

    /**
     * Adds a new Workspace and switches to it.
     */
    fun addWorkspace(context: Context? = null): Int {
        if (isLayoutLocked) return currentWorkspaceIndex
        val newIndex = workspaceDocks.size
        val newWorkspace = mutableListOf<DockTile>()
        newWorkspace.add(
            DockTile.DockAnchor(
                id = "dock_anchor_ws_${newIndex + 1}_${System.currentTimeMillis()}",
                title = "Workspace ${newIndex + 1}",
                iconSymbol = "📎"
            )
        )
        workspaceDocks.add(newWorkspace)
        currentWorkspaceIndex = newIndex
        notifyChanged(context)
        return newIndex
    }

    /**
     * Removes the current workspace.
     * Rules: Workspace 1 (index 0) can NEVER be removed!
     */
    fun removeCurrentWorkspace(context: Context? = null): Boolean {
        if (isLayoutLocked || currentWorkspaceIndex == 0 || workspaceDocks.size <= 1) {
            return false // First workspace can NEVER be removed!
        }
        workspaceDocks.removeAt(currentWorkspaceIndex)
        currentWorkspaceIndex = (currentWorkspaceIndex - 1).coerceAtLeast(0)
        notifyChanged(context)
        return true
    }

    fun updateIconSize(newSizeDp: Int, context: Context) {
        if (isLayoutLocked) return
        tileIconSizeDp = newSizeDp.coerceIn(24, 96)
        notifyChanged(context)
    }

    fun updateAccentColor(colorHex: String, context: Context) {
        accentColorHex = colorHex
        saveState(context)
        notifyChanged(context)
    }

    fun updateThemeColors(
        accentHex: String = accentColorHex,
        attentionHex: String = attentionColorHex,
        textHex: String = textColorHex,
        graphicHex: String = graphicColorHex,
        highHex: String = colorHighHex,
        medHex: String = colorMedHex,
        lowHex: String = colorLowHex,
        context: Context
    ) {
        accentColorHex = accentHex
        attentionColorHex = attentionHex
        textColorHex = textHex
        graphicColorHex = graphicHex
        colorHighHex = highHex
        colorMedHex = medHex
        colorLowHex = lowHex
        saveState(context)
        notifyChanged(context)
    }

    fun requestAttention(tileIdOrPkg: String, context: Context? = null) {
        var found = false
        val allLists = workspaceDocks + listOf(bottomLeftDockTiles, bottomDockTiles)
        allLists.forEach { list ->
            list.forEach { tile ->
                val matches = tile.id == tileIdOrPkg ||
                        (tile is DockTile.RunningTask && tile.packageName == tileIdOrPkg) ||
                        (tile is DockTile.AppShortcut && tile.packageName == tileIdOrPkg)
                if (matches) {
                    tile.isAttentionRequested = true
                    found = true
                }
            }
        }
        if (found) {
            notifyChanged(context)
        }
    }

    fun clearAttention(tileIdOrPkg: String, context: Context? = null) {
        var found = false
        val allLists = workspaceDocks + listOf(bottomLeftDockTiles, bottomDockTiles)
        allLists.forEach { list ->
            list.forEach { tile ->
                val matches = tile.id == tileIdOrPkg ||
                        (tile is DockTile.RunningTask && tile.packageName == tileIdOrPkg) ||
                        (tile is DockTile.AppShortcut && tile.packageName == tileIdOrPkg)
                if (matches && tile.isAttentionRequested) {
                    tile.isAttentionRequested = false
                    found = true
                }
            }
        }
        if (found) {
            notifyChanged(context)
        }
    }

    fun scaleIconSize(scaleFactor: Float, context: Context? = null) {
        if (isLayoutLocked) return
        val newSize = (tileIconSizeDp * scaleFactor).toInt().coerceIn(24, 96)
        if (newSize != tileIconSizeDp) {
            tileIconSizeDp = newSize
            notifyChanged(context)
        }
    }

    fun updateClockFormats(timeFmt: String, dateFmt: String, context: Context) {
        clockTimeFormat = timeFmt
        clockDateFormat = dateFmt
        notifyChanged(context)
    }

    /**
     * Synchronously resets all dock tiles to factory defaults, clears SharedPreferences,
     * removes all extra added items, and repopulates initial default handlers.
     */
    fun resetDocksToDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        workspaceDocks.clear()
        bottomLeftDockTiles.clear()
        bottomDockTiles.clear()

        tileIconSizeDp = 56 // Reset to 56dp (2x default)
        isLayoutLocked = false
        currentWorkspaceIndex = 0
        accentColorHex = "#FFFFFF"
        clockTimeFormat = "HH:mm"
        clockDateFormat = "EEE, MMM d"
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
        accentColorHex = prefs.getString(KEY_ACCENT_COLOR, "#FFFFFF") ?: "#FFFFFF"

        val savedBottom = prefs.getString(KEY_BOTTOM, null)
        val savedWorkspace0 = prefs.getString(KEY_WORKSPACE_PREFIX + "0", null)

        if (savedBottom != null && savedWorkspace0 != null) {
            restoreState(context, prefs)
            currentWorkspaceIndex = prefs.getInt(KEY_WORKSPACE_INDEX, 0).coerceIn(0, workspaceDocks.size - 1)
            initRunningStackDefaults()
            notifyChanged()
            return
        }

        populateDefaultTiles(context)
        saveState(context)
        notifyChanged()
    }

    private fun populateDefaultTiles(context: Context) {
        workspaceDocks.clear()

        // WORKSPACE 1 DEFAULT DOCK
        val ws1 = mutableListOf<DockTile>()
        ws1.add(DockTile.DockAnchor(id = "dock_anchor_ws1", title = "Workspace 1", iconSymbol = "📎"))
        ws1.add(DockTile.InternalDockApp(id = "wmclock", title = "Clock / Cal", iconSymbol = "⏰", moduleType = "WMCLOCK"))
        ws1.add(DockTile.InternalDockApp(id = "wmbattery", title = "Battery Mon", iconSymbol = "⚡", moduleType = "WMBATTERY"))
        ws1.add(DockTile.InternalDockApp(id = "wmmon", title = "CPU & Net", iconSymbol = "📊", moduleType = "WMMON"))
        workspaceDocks.add(ws1)

        // WORKSPACE 2 DEFAULT DOCK
        val ws2 = mutableListOf<DockTile>()
        ws2.add(DockTile.DockAnchor(id = "dock_anchor_ws2", title = "Workspace 2", iconSymbol = "📎"))
        ws2.add(DockTile.VfsCategoryLink(id = "ws2_dev", title = "Development", iconSymbol = "⚡", category = VfsCategory.DEVELOPMENT))
        ws2.add(DockTile.VfsCategoryLink(id = "ws2_prod", title = "Productivity", iconSymbol = "💼", category = VfsCategory.PRODUCTIVITY))
        workspaceDocks.add(ws2)

        // WORKSPACE 3 DEFAULT DOCK
        val ws3 = mutableListOf<DockTile>()
        ws3.add(DockTile.DockAnchor(id = "dock_anchor_ws3", title = "Workspace 3", iconSymbol = "📎"))
        ws3.add(DockTile.VfsCategoryLink(id = "ws3_games", title = "Games", iconSymbol = "🎮", category = VfsCategory.MULTIMEDIA))
        workspaceDocks.add(ws3)

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

    private val wmMonDisplayModes = mutableMapOf<String, Int>()

    fun getWmMonMode(tileId: String): Int {
        return wmMonDisplayModes[tileId] ?: 0
    }

    fun cycleWmMonMode(tileId: String, context: Context? = null): Int {
        val current = getWmMonMode(tileId)
        val next = (current + 1) % 4
        wmMonDisplayModes[tileId] = next
        notifyChanged(context)
        return next
    }

    private fun initRunningStackDefaults() {
        bottomLeftDockTiles.clear()
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
            editor.putInt(KEY_NUM_WORKSPACES, workspaceDocks.size)
            editor.putString(KEY_ACCENT_COLOR, accentColorHex)
            editor.putString(KEY_ATTENTION_COLOR, attentionColorHex)
            editor.putString(KEY_TEXT_COLOR, textColorHex)
            editor.putString(KEY_GRAPHIC_COLOR, graphicColorHex)
            editor.putString(KEY_COLOR_HIGH, colorHighHex)
            editor.putString(KEY_COLOR_MED, colorMedHex)
            editor.putString(KEY_COLOR_LOW, colorLowHex)
            editor.putString(KEY_CLOCK_TIME_FMT, clockTimeFormat)
            editor.putString(KEY_CLOCK_DATE_FMT, clockDateFormat)

            for (w in 0 until workspaceDocks.size) {
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
        attentionColorHex = prefs.getString(KEY_ATTENTION_COLOR, "#FF3D00") ?: "#FF3D00"
        textColorHex = prefs.getString(KEY_TEXT_COLOR, "#FFFFFF") ?: "#FFFFFF"
        graphicColorHex = prefs.getString(KEY_GRAPHIC_COLOR, "#00E5FF") ?: "#00E5FF"
        colorHighHex = prefs.getString(KEY_COLOR_HIGH, "#FF5252") ?: "#FF5252"
        colorMedHex = prefs.getString(KEY_COLOR_MED, "#FFD700") ?: "#FFD700"
        colorLowHex = prefs.getString(KEY_COLOR_LOW, "#00E676") ?: "#00E676"
        clockTimeFormat = prefs.getString(KEY_CLOCK_TIME_FMT, "HH:mm") ?: "HH:mm"
        clockDateFormat = prefs.getString(KEY_CLOCK_DATE_FMT, "EEE, MMM d") ?: "EEE, MMM d"
        val numWorkspaces = prefs.getInt(KEY_NUM_WORKSPACES, 3).coerceAtLeast(1)

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

        workspaceDocks.clear()
        for (w in 0 until numWorkspaces) {
            val list = mutableListOf<DockTile>()
            val wsJson = prefs.getString(KEY_WORKSPACE_PREFIX + w, null)
            if (wsJson != null) {
                parseList(wsJson, list)
            }
            workspaceDocks.add(list)
        }

        val bottomJson = prefs.getString(KEY_BOTTOM, null)
        if (bottomJson != null) {
            parseList(bottomJson, bottomDockTiles)
        }
    }
}
