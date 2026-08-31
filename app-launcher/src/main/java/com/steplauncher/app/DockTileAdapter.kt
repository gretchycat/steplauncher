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
        holder.tvTitle.text = tile.title
        holder.tvTitle.setTypeface(null, Typeface.BOLD)

        // Dynamic icon sizing based on configuration (DockManager.tileIconSizeDp)
        val iconSizeDp = DockManager.tileIconSizeDp
        val density = holder.itemView.resources.displayMetrics.density
        val iconSizePx = (iconSizeDp * density).toInt()
        val containerSizePx = ((iconSizeDp + 36) * density).toInt()

        val lp = holder.itemView.layoutParams
        if (lp != null) {
            lp.width = containerSizePx
            lp.height = containerSizePx
            holder.itemView.layoutParams = lp
        }

        val ivLp = holder.ivAppIcon.layoutParams
        if (ivLp != null) {
            ivLp.width = iconSizePx
            ivLp.height = iconSizePx
            holder.ivAppIcon.layoutParams = ivLp
        }

        holder.tvIcon.textSize = iconSizeDp * 0.7f

        // Dynamic Accent Color Tinting for Frosted Glass Tile Background
        val accentColorHex = DockManager.accentColorHex
        if (!accentColorHex.isNullOrEmpty() && accentColorHex != "#FFFFFF") {
            try {
                val colorInt = Color.parseColor(accentColorHex)
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
        } else if (tile is DockTile.DockAnchor && (tile.iconSymbol == "📎" || tile.iconSymbol.equals("paperclip", ignoreCase = true) || tile.iconSymbol.isEmpty())) {
            holder.ivAppIcon.setImageResource(R.drawable.ic_dock_anchor_paperclip)
            holder.ivAppIcon.visibility = View.VISIBLE
            holder.tvIcon.visibility = View.GONE
        } else {
            holder.ivAppIcon.visibility = View.GONE
            holder.tvIcon.visibility = View.VISIBLE
            holder.tvIcon.text = tile.iconSymbol
        }

        // Apply Text Accent Color
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
                holder.tvBadge.visibility = View.VISIBLE
                holder.tvBadge.text = "${DockManager.currentWorkspaceIndex + 1}"
                holder.itemView.alpha = 1.0f
            }
            is DockTile.AppShortcut -> {
                holder.tvSubtitle.text = "App"
                holder.tvBadge.visibility = View.GONE
                holder.itemView.alpha = 1.0f
            }
            is DockTile.RunningTask -> {
                holder.tvSubtitle.text = "PID:${tile.processId} ${tile.cpuUsagePercent}%"
                holder.tvBadge.visibility = View.GONE
                holder.itemView.alpha = 1.0f
            }
            is DockTile.VfsCategoryLink -> {
                holder.tvSubtitle.text = "VFS Link"
                holder.tvBadge.visibility = View.GONE
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
                    holder.tvTitle.textSize = iconSizeDp * 0.42f

                    holder.tvSubtitle.text = formattedDate
                    holder.tvSubtitle.textSize = iconSizeDp * 0.22f
                } else if (tile.moduleType.equals("WMBATTERY", ignoreCase = true)) {
                    val bat = com.steplauncher.core.vfs.BatteryUtils.getBatteryStatus(holder.itemView.context)
                    holder.tvTitle.text = "${bat.levelPercent}%"
                    holder.tvSubtitle.text = if (bat.isCharging) "⚡ ${bat.chargePlugStr}" else "🔋 Discharging"
                    holder.tvIcon.text = if (bat.isCharging) "⚡" else if (bat.levelPercent <= 20) "🪫" else "🔋"

                    val batColorHex = when {
                        bat.levelPercent <= 20 -> DockManager.colorHighHex
                        bat.levelPercent <= 40 -> DockManager.colorMedHex
                        else -> DockManager.colorLowHex
                    }
                    try { holder.tvTitle.setTextColor(Color.parseColor(batColorHex)) } catch (e: Exception) {}
                } else if (tile.moduleType.equals("WMMON", ignoreCase = true) || tile.moduleType.equals("TELEMETRY", ignoreCase = true)) {
                    val mode = DockManager.getWmMonMode(tile.id)
                    val density = holder.itemView.resources.displayMetrics.density
                    val graphPx = (iconSizeDp * density).toInt().coerceAtLeast(64)

                    holder.ivAppIcon.visibility = View.VISIBLE
                    holder.tvIcon.visibility = View.GONE

                    when (mode) {
                        0 -> { // CPU Line Graph
                            val cpu = com.steplauncher.core.vfs.SysMonUtils.getCpuMetrics()
                            val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawCpuLineGraph(
                                graphPx, graphPx, com.steplauncher.core.vfs.SysMonUtils.cpuHistory,
                                DockManager.graphicColorHex, DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                            )
                            holder.ivAppIcon.setImageBitmap(bmp)
                            holder.tvTitle.text = "CPU: ${cpu.cpuPercent}%"
                            holder.tvSubtitle.text = "${cpu.numCores} Cores"
                        }
                        1 -> { // Memory Bar Graph
                            val mem = com.steplauncher.core.vfs.SysMonUtils.getMemoryMetrics(holder.itemView.context)
                            val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawMemoryBarGraph(
                                graphPx, graphPx, mem.ramUsagePercent,
                                DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                            )
                            holder.ivAppIcon.setImageBitmap(bmp)
                            holder.tvTitle.text = "RAM: ${mem.ramUsagePercent}%"
                            holder.tvSubtitle.text = "${mem.usedRamMb}M / ${mem.totalRamMb}M"
                        }
                        2 -> { // Storage Gauge Arc Graph
                            val storage = com.steplauncher.core.vfs.SysMonUtils.getStorageMetrics(holder.itemView.context)
                            val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawStorageGaugeGraph(
                                graphPx, graphPx, storage.storagePercentUsed,
                                DockManager.colorHighHex, DockManager.colorMedHex, DockManager.colorLowHex
                            )
                            holder.ivAppIcon.setImageBitmap(bmp)
                            holder.tvTitle.text = "Disk: ${storage.storagePercentUsed}%"
                            holder.tvSubtitle.text = "${String.format(Locale.US, "%.1f", storage.internalFreeGb)}G Free"
                        }
                        3 -> { // Network Wave Graph
                            val net = com.steplauncher.core.vfs.SysMonUtils.getNetworkMetrics(holder.itemView.context)
                            val bmp = com.steplauncher.core.renderer.SparklineGraphRenderer.drawNetworkWaveGraph(
                                graphPx, graphPx,
                                com.steplauncher.core.vfs.SysMonUtils.rxHistory,
                                com.steplauncher.core.vfs.SysMonUtils.txHistory,
                                DockManager.graphicColorHex
                            )
                            holder.ivAppIcon.setImageBitmap(bmp)
                            holder.tvTitle.text = "↓${net.rxRateKbps}K ↑${net.txRateKbps}K"
                            holder.tvSubtitle.text = net.ipAddress
                        }
                    }
                } else {
                    holder.tvSubtitle.text = tile.moduleType
                }
                holder.tvBadge.visibility = View.GONE
                holder.itemView.alpha = 1.0f
            }
            is DockTile.ExternalDockApp -> {
                holder.tvSubtitle.text = "Ext DockApp"
                holder.tvBadge.visibility = View.GONE
                holder.itemView.alpha = 1.0f
            }
            is DockTile.PlaceholderBox -> {
                holder.tvSubtitle.text = tile.subtitle
                holder.tvBadge.visibility = View.GONE
                holder.itemView.alpha = 0.7f
            }
        }

        com.steplauncher.core.renderer.ForgivingTouchHelper.bind(
            view = holder.itemView,
            onClick = { onTileClick(tile) },
            onLongClick = { view ->
                onTileLongClickMenu(tile, view)
                true
            }
        )
    }

    override fun getItemCount(): Int = tiles.size
}
