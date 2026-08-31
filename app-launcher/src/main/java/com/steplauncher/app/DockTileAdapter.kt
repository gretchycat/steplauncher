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
        } else {
            holder.ivAppIcon.visibility = View.GONE
            holder.tvIcon.visibility = View.VISIBLE
            holder.tvIcon.text = tile.iconSymbol
        }

        when (tile) {
            is DockTile.DockAnchor -> {
                holder.tvSubtitle.text = "Dock Main"
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
                holder.tvSubtitle.text = tile.moduleType
                holder.itemView.alpha = 1.0f
            }
            is DockTile.ExternalDockApp -> {
                holder.tvSubtitle.text = "Ext DockApp"
                holder.itemView.alpha = 1.0f
            }
            is DockTile.PlaceholderBox -> {
                holder.tvSubtitle.text = tile.subtitle
                holder.itemView.alpha = 0.7f
            }
        }

        holder.itemView.isClickable = true
        holder.itemView.isLongClickable = true

        holder.itemView.setOnClickListener {
            onTileClick(tile)
        }

        holder.itemView.setOnLongClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onTileLongClickMenu(tile, view)
            true
        }
    }

    override fun getItemCount(): Int = tiles.size
}
