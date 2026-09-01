package com.steplauncher.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.steplauncher.core.renderer.DockManager
import com.steplauncher.core.renderer.DockTile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DockTileAdapter(
    val tiles: MutableList<DockTile>,
    var isBottomDock: Boolean = false,
    var isRightDock: Boolean = false,
    var isPortrait: Boolean = true,
    var parentContainerWidthPx: Int = 0,
    var parentContainerHeightPx: Int = 0,
    private val onTileClick: (DockTile) -> Unit,
    private val onTileLongClickMenu: (DockTile, view: View) -> Unit
) : RecyclerView.Adapter<DockTileAdapter.TileViewHolder>() {

    class TileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAppIcon: ImageView = view.findViewById(R.id.iv_tile_app_icon)
        val tvIcon: TextView = view.findViewById(R.id.tv_tile_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_tile_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_tile_subtitle)
        val tvBadge: TextView = view.findViewById(R.id.tv_tile_workspace_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        val context = holder.itemView.context
        holder.tvTitle.text = tile.title
        holder.tvTitle.setTypeface(null, Typeface.BOLD)

        val density = holder.itemView.resources.displayMetrics.density
        val tileCount = tiles.size.coerceAtLeast(1)

        var targetContainerWidthPx = ((DockManager.tileIconSizeDp + 26) * density).toInt()
        var targetContainerHeightPx = ((DockManager.tileIconSizeDp + 26) * density).toInt()

        if (isBottomDock && isPortrait && parentContainerWidthPx > 0) {
            // Portrait mode: Bottom menu scales to fill the complete width
            val calculatedWidth = (parentContainerWidthPx - (8 * density).toInt()) / tileCount
            val maxAllowedWidth = (72 * density).toInt()
            val minAllowedWidth = (40 * density).toInt()
            targetContainerWidthPx = calculatedWidth.coerceIn(minAllowedWidth, maxAllowedWidth)
            targetContainerHeightPx = (58 * density).toInt()
        } else if (isRightDock && !isPortrait && parentContainerHeightPx > 0) {
            // Landscape mode: Right dock resizes items into the number of items in the right dock
            val calculatedHeight = (parentContainerHeightPx - (16 * density).toInt()) / tileCount
            val maxAllowedHeight = (68 * density).toInt()
            val minAllowedHeight = (36 * density).toInt()
            targetContainerWidthPx = (58 * density).toInt()
            targetContainerHeightPx = calculatedHeight.coerceIn(minAllowedHeight, maxAllowedHeight)
        }

        val lp = holder.itemView.layoutParams
        if (lp != null) {
            lp.width = targetContainerWidthPx
            lp.height = targetContainerHeightPx
            holder.itemView.layoutParams = lp
        }

        // Dynamically scale icon inside according to available container size
        val dynamicIconPx = (minOf(targetContainerWidthPx, targetContainerHeightPx) * 0.52).toInt()
            .coerceIn((20 * density).toInt(), (42 * density).toInt())

        val ivLp = holder.ivAppIcon.layoutParams
        if (ivLp != null) {
            ivLp.width = dynamicIconPx
            ivLp.height = dynamicIconPx
            holder.ivAppIcon.layoutParams = ivLp
        }

        val tvLp = holder.tvIcon.layoutParams
        if (tvLp != null) {
            tvLp.width = dynamicIconPx
            tvLp.height = dynamicIconPx
            holder.tvIcon.layoutParams = tvLp
            holder.tvIcon.textSize = dynamicIconPx / density * 0.55f
        }

        // Apply Tile Background Accent Color & Attention Color
        val tileBgHex = if (tile.isAttentionRequested) DockManager.attentionColorHex else DockManager.accentColorHex
        if (!tileBgHex.isNullOrEmpty() && tileBgHex != "#FFFFFF") {
            try {
                val colorInt = Color.parseColor(tileBgHex)
                holder.itemView.background?.mutate()?.setTint(colorInt)
            } catch (e: Exception) {
                holder.itemView.background?.mutate()?.clearColorFilter()
            }
        } else {
            holder.itemView.background?.mutate()?.clearColorFilter()
        }

        // Try loading real application icon from PackageManager for App Shortcuts & Running Tasks
        val pkgName = when (tile) {
            is DockTile.AppShortcut -> tile.packageName
            is DockTile.RunningTask -> tile.packageName
            is DockTile.ExternalDockApp -> tile.descriptor.packageName
            else -> null
        }

        val customIconUri = tile.iconSymbol
        var isCustomImage = false

        if (customIconUri.startsWith("content://") || customIconUri.startsWith("file://") || customIconUri.startsWith("/")) {
            try {
                val uri = android.net.Uri.parse(customIconUri)
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(holder.itemView.context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(holder.itemView.context.contentResolver, uri)
                }
                holder.ivAppIcon.setImageBitmap(bitmap)
                holder.ivAppIcon.visibility = View.VISIBLE
                holder.tvIcon.visibility = View.GONE
                isCustomImage = true
            } catch (e: Exception) {
                isCustomImage = false
            }
        }

        if (!isCustomImage) {
            var appDrawable: Drawable? = null
            if (pkgName != null && pkgName.contains(".")) {
                try {
                    appDrawable = holder.itemView.context.packageManager.getApplicationIcon(pkgName)
                } catch (e: Exception) {
                    appDrawable = null
                }
            }

            if (appDrawable != null) {
                holder.ivAppIcon.setImageDrawable(appDrawable)
                holder.ivAppIcon.visibility = View.VISIBLE
                holder.tvIcon.visibility = View.GONE
                holder.ivAppIcon.clearColorFilter()
            } else {
                val resId = holder.itemView.context.resources.getIdentifier(customIconUri, "drawable", holder.itemView.context.packageName)
                if (resId != 0) {
                    holder.ivAppIcon.setImageResource(resId)
                    holder.ivAppIcon.visibility = View.VISIBLE
                    holder.tvIcon.visibility = View.GONE
                } else if (tile is DockTile.DockAnchor && (tile.iconSymbol == "📎" || tile.iconSymbol.equals("paperclip", ignoreCase = true) || tile.iconSymbol.isEmpty())) {
                    holder.ivAppIcon.setImageResource(R.drawable.ic_dock_anchor_paperclip)
                    holder.ivAppIcon.visibility = View.VISIBLE
                    holder.tvIcon.visibility = View.GONE
                } else {
                    holder.ivAppIcon.setImageResource(R.drawable.ic_cat_app)
                    holder.ivAppIcon.visibility = View.VISIBLE
                    holder.tvIcon.visibility = View.GONE
                }

                // Apply Image Accent Color (graphicColorHex) to built-in vector drawables
                if (!DockManager.graphicColorHex.isNullOrEmpty() && DockManager.graphicColorHex != "#FFFFFF") {
                    try {
                        val graphicColorInt = Color.parseColor(DockManager.graphicColorHex)
                        holder.ivAppIcon.setColorFilter(graphicColorInt)
                    } catch (e: Exception) {
                        holder.ivAppIcon.clearColorFilter()
                    }
                } else {
                    holder.ivAppIcon.clearColorFilter()
                }
            }
        } else {
            holder.ivAppIcon.clearColorFilter()
        }

        // Apply Text Accent Color (textColorHex)
        val textHex = DockManager.textColorHex
        if (!textHex.isNullOrEmpty()) {
            try {
                val colorInt = Color.parseColor(textHex)
                holder.tvTitle.setTextColor(colorInt)
                holder.tvSubtitle.setTextColor(colorInt)
            } catch (e: Exception) {}
        }

        when (tile) {
            is DockTile.DockAnchor -> {
                holder.tvSubtitle.text = "Anchor"
                holder.itemView.alpha = 1.0f
            }
            is DockTile.AppShortcut -> {
                holder.tvSubtitle.text = "App"
                holder.itemView.alpha = 1.0f
            }
            is DockTile.RunningTask -> {
                holder.tvSubtitle.text = "PID:${tile.processId} ${tile.cpuUsagePercent}%"
                holder.itemView.alpha = 1.0f
            }
            is DockTile.VfsCategoryLink -> {
                holder.tvSubtitle.text = "VFS Link"
                holder.itemView.alpha = 0.95f
            }
            is DockTile.InternalDockApp -> {
                if (tile.moduleType.equals("WMCLOCK", ignoreCase = true)) {
                    val timeFmtStr = DockManager.clockTimeFormat
                    val dateFmtStr = DockManager.clockDateFormat
                    val now = Date()

                    val formattedTime = try {
                        SimpleDateFormat(timeFmtStr, Locale.getDefault()).format(now)
                    } catch (e: Exception) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                    }

                    val formattedDate = try {
                        SimpleDateFormat(dateFmtStr, Locale.getDefault()).format(now)
                    } catch (e: Exception) {
                        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
                    }

                    holder.ivAppIcon.visibility = View.GONE
                    holder.tvIcon.visibility = View.GONE

                    holder.tvTitle.text = formattedTime
                    holder.tvTitle.textSize = dynamicIconPx / density * 0.42f

                    holder.tvSubtitle.text = formattedDate
                    holder.tvSubtitle.textSize = dynamicIconPx / density * 0.22f
                } else if (tile.moduleType.equals("WMBATTERY", ignoreCase = true)) {
                    val bat = com.steplauncher.core.vfs.BatteryUtils.getBatteryStatus(holder.itemView.context)
                    holder.tvTitle.text = "${bat.levelPercent}%"
                    holder.tvSubtitle.text = if (bat.isCharging) "⚡ ${bat.chargePlugStr}" else "Discharging"
                    holder.ivAppIcon.setImageResource(R.drawable.ic_cat_system)
                    holder.ivAppIcon.visibility = View.VISIBLE
                    holder.tvIcon.visibility = View.GONE

                    val batColorHex = when {
                        bat.levelPercent <= 20 -> DockManager.colorHighHex
                        bat.levelPercent <= 50 -> DockManager.colorMedHex
                        else -> DockManager.colorLowHex
                    }
                    try {
                        val colorInt = Color.parseColor(batColorHex)
                        holder.ivAppIcon.setColorFilter(colorInt)
                    } catch (e: Exception) {}
                } else if (tile.moduleType.equals("WMMON", ignoreCase = true) || tile.moduleType.equals("TELEMETRY", ignoreCase = true)) {
                    val mode = DockManager.getWmMonMode(tile.id)
                    val graphBmp = when (mode) {
                        0 -> { // CPU Sparkline Line Graph
                            holder.tvSubtitle.text = "CPU Load"
                            com.steplauncher.core.renderer.SparklineGraphRenderer.drawCpuLineGraph(
                                dynamicIconPx, dynamicIconPx, com.steplauncher.core.vfs.SysMonUtils.cpuHistory,
                                DockManager.graphicColorHex, DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                            )
                        }
                        1 -> { // RAM Usage Segmented Bar Graph
                            val mem = com.steplauncher.core.vfs.SysMonUtils.getMemoryMetrics(holder.itemView.context)
                            holder.tvSubtitle.text = "RAM ${mem.ramUsagePercent}%"
                            com.steplauncher.core.renderer.SparklineGraphRenderer.drawMemoryBarGraph(
                                dynamicIconPx, dynamicIconPx, mem.ramUsagePercent,
                                DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                            )
                        }
                        2 -> { // Storage Capacity Gauge Arc Graph
                            val storage = com.steplauncher.core.vfs.SysMonUtils.getStorageMetrics(holder.itemView.context)
                            holder.tvSubtitle.text = "Disk ${storage.storagePercentUsed}%"
                            com.steplauncher.core.renderer.SparklineGraphRenderer.drawStorageGaugeGraph(
                                dynamicIconPx, dynamicIconPx, storage.storagePercentUsed,
                                DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                            )
                        }
                        else -> { // Network Bandwidth Dual Wave Graph
                            val net = com.steplauncher.core.vfs.SysMonUtils.getNetworkMetrics(holder.itemView.context)
                            holder.tvSubtitle.text = "Net ${net.rxRateKbps}K/s"
                            com.steplauncher.core.renderer.SparklineGraphRenderer.drawNetworkWaveGraph(
                                dynamicIconPx, dynamicIconPx, com.steplauncher.core.vfs.SysMonUtils.rxHistory, com.steplauncher.core.vfs.SysMonUtils.txHistory,
                                DockManager.graphicColorHex
                            )
                        }
                    }

                    holder.ivAppIcon.setImageBitmap(graphBmp)
                    holder.ivAppIcon.visibility = View.VISIBLE
                    holder.tvIcon.visibility = View.GONE
                    holder.itemView.alpha = 1.0f
                }
            }
            is DockTile.ExternalDockApp -> {
                holder.tvSubtitle.text = "Plugin"
                holder.itemView.alpha = 1.0f
            }
            is DockTile.PlaceholderBox -> {
                holder.tvSubtitle.text = tile.subtitle
                holder.itemView.alpha = 0.7f
            }
        }

        // Resolve Unread Badges for Email, Messaging, Phone, Category Links, System Alerts, and Dock Anchors
        val packageForBadge = when (tile) {
            is DockTile.AppShortcut -> tile.packageName
            is DockTile.RunningTask -> tile.packageName
            is DockTile.ExternalDockApp -> tile.descriptor.packageName
            else -> null
        }

        val resolvedBadgeText = if (tile.isAttentionRequested && !tile.attentionBadgeText.isNullOrEmpty()) {
            tile.attentionBadgeText
        } else if (packageForBadge != null) {
            com.steplauncher.core.telemetry.UnreadBadgeResolver.getBadgeTextForPackage(holder.itemView.context, packageForBadge)
        } else if (tile is DockTile.VfsCategoryLink) {
            when (tile.category) {
                com.steplauncher.core.vfs.VfsCategory.PHONE -> com.steplauncher.core.telemetry.UnreadBadgeResolver.getMissedCallCount(holder.itemView.context).takeIf { it > 0 }?.toString()
                com.steplauncher.core.vfs.VfsCategory.SOCIAL_MEDIA -> com.steplauncher.core.telemetry.UnreadBadgeResolver.getUnreadSmsCount(holder.itemView.context).takeIf { it > 0 }?.toString()
                com.steplauncher.core.vfs.VfsCategory.PRODUCTIVITY -> com.steplauncher.core.telemetry.UnreadBadgeResolver.getUnreadEmailCount(holder.itemView.context).takeIf { it > 0 }?.toString()
                else -> null
            }
        } else if (tile is DockTile.DockAnchor) {
            "${DockManager.currentWorkspaceIndex + 1}"
        } else {
            null
        }

        if (!resolvedBadgeText.isNullOrEmpty()) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = resolvedBadgeText
            try {
                val bgInt = Color.parseColor(DockManager.badgeBgColorHex)
                val textInt = Color.parseColor(DockManager.badgeTextColorHex)
                holder.tvBadge.background?.mutate()?.setTint(bgInt)
                holder.tvBadge.setTextColor(textInt)
            } catch (e: Exception) {
                holder.tvBadge.setTextColor(Color.WHITE)
            }
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        com.steplauncher.core.renderer.ForgivingTouchHelper.bind(
            view = holder.itemView,
            onClick = {
                if (tile.isAttentionRequested) {
                    DockManager.clearAttention(tile.id, holder.itemView.context)
                }
                onTileClick(tile)
            },
            onLongClick = { view ->
                onTileLongClickMenu(tile, view)
                true
            }
        )
    }

    override fun getItemCount(): Int = tiles.size
}
