package com.steplauncher.core.renderer

import android.content.Intent
import com.steplauncher.core.ipc.DockAppDescriptor
import com.steplauncher.core.vfs.VfsCategory

sealed class DockTile(
    open val id: String,
    open val title: String,
    open val iconSymbol: String,
    open var isAttentionRequested: Boolean = false,
    open var attentionBadgeText: String? = null
) {
    data class DockAnchor(
        override val id: String = "dock_anchor_main",
        override val title: String = "Settings",
        override val iconSymbol: String = "⚙️",
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)

    data class AppShortcut(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val packageName: String,
        val launchIntent: Intent?,
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)

    data class RunningTask(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val packageName: String,
        val processId: Int = 0,
        val cpuUsagePercent: Int = 0,
        val launchIntent: Intent?,
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)

    data class VfsCategoryLink(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val category: VfsCategory,
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)

    data class InternalDockApp(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val moduleType: String, // e.g. "WMCLOCK", "WMBATTERY", "WMMON"
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)

    data class ExternalDockApp(
        override val id: String,
        override val title: String,
        override val iconSymbol: String,
        val descriptor: DockAppDescriptor,
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)

    data class PlaceholderBox(
        override val id: String,
        override val title: String,
        override val iconSymbol: String = "📦",
        val subtitle: String = "VFS / Dock App Slot",
        override var isAttentionRequested: Boolean = false,
        override var attentionBadgeText: String? = null
    ) : DockTile(id, title, iconSymbol, isAttentionRequested, attentionBadgeText)
}
