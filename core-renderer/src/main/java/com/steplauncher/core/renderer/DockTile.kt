package com.steplauncher.core.renderer

import android.content.Intent
import com.steplauncher.core.ipc.DockAppDescriptor
import com.steplauncher.core.vfs.VfsCategory

sealed class DockTile(
    open val id: String,
    open val title: String,
    open val iconSymbol: String
) {
    data class DockAnchor(
        override val id: String = "dock_anchor_main",
        override val title: String = "Settings",
        override val iconSymbol: String = "⚙️"
    ) : DockTile(id, title, iconSymbol)

    data class AppShortcut(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val packageName: String,
        val launchIntent: Intent?
    ) : DockTile(id, title, iconSymbol)

    data class RunningTask(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val packageName: String,
        val processId: Int = 0,
        val cpuUsagePercent: Int = 0,
        val launchIntent: Intent?
    ) : DockTile(id, title, iconSymbol)

    data class VfsCategoryLink(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val category: VfsCategory
    ) : DockTile(id, title, iconSymbol)

    data class InternalDockApp(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val moduleType: String // e.g. "WMCLOCK", "WMBATTERY", "WMMON"
    ) : DockTile(id, title, iconSymbol)

    data class ExternalDockApp(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val descriptor: DockAppDescriptor
    ) : DockTile(id, title, iconSymbol)

    data class PlaceholderBox(
        override val id: String,
        override val title: String,
        override val iconSymbol: String = "📦",
        val subtitle: String = "VFS / Dock App Slot"
    ) : DockTile(id, title, iconSymbol)
}
