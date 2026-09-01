package com.steplauncher.app

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.steplauncher.core.renderer.DockManager
import com.steplauncher.core.renderer.ForgivingTouchHelper
import com.steplauncher.core.vfs.VfsNode

/**
 * RecyclerView Adapter for displaying VFS Program Manager items in a scrollable grid with real application icons.
 */
class VfsGridAdapter(
    private val items: List<VfsNode>,
    private val onItemClick: (VfsNode) -> Unit,
    private val onItemLongClick: (VfsNode, View) -> Unit
) : RecyclerView.Adapter<VfsGridAdapter.VfsViewHolder>() {

    class VfsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_vfs_icon)
        val tvSymbol: TextView = itemView.findViewById(R.id.tv_vfs_symbol)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_vfs_title)
        val tvBadge: TextView = itemView.findViewById(R.id.tv_vfs_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VfsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vfs_grid, parent, false)
        return VfsViewHolder(view)
    }

    override fun onBindViewHolder(holder: VfsViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val pm = context.packageManager

        holder.tvTitle.text = item.name

        var appDrawable: Drawable? = null
        if (!item.isDirectory && !item.targetPackage.isNullOrEmpty()) {
            try {
                appDrawable = pm.getApplicationIcon(item.targetPackage!!)
            } catch (e: Exception) {
                appDrawable = null
            }
        }

        if (appDrawable != null) {
            holder.ivIcon.setImageDrawable(appDrawable)
            holder.ivIcon.visibility = View.VISIBLE
            holder.tvSymbol.visibility = View.GONE
        } else {
            holder.ivIcon.visibility = View.GONE
            holder.tvSymbol.visibility = View.VISIBLE
            holder.tvSymbol.text = item.iconSymbol
        }

        // Attention State & Badge Check
        val targetPkg = item.targetPackage
        val isAttention = if (!targetPkg.isNullOrEmpty()) DockManager.isTileAttentionRequested(targetPkg) else false
        val badgeText = if (!targetPkg.isNullOrEmpty()) DockManager.getTileBadgeText(targetPkg) else null

        // Tile background tint swap if attention requested
        val tintHex = if (isAttention) DockManager.attentionColorHex else DockManager.accentColorHex
        if (!tintHex.isNullOrEmpty() && (tintHex != "#FFFFFF" || isAttention)) {
            try {
                val colorInt = Color.parseColor(tintHex)
                holder.itemView.background?.mutate()?.setTint(colorInt)
            } catch (e: Exception) {
                holder.itemView.background?.mutate()?.clearColorFilter()
            }
        } else {
            holder.itemView.background?.mutate()?.clearColorFilter()
        }

        // Render Badge
        if (isAttention || !badgeText.isNullOrEmpty()) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = badgeText ?: "🔴"
            try {
                val bgInt = Color.parseColor(DockManager.badgeBgColorHex)
                val textInt = Color.parseColor(DockManager.badgeTextColorHex)
                holder.tvBadge.background?.mutate()?.setTint(bgInt)
                holder.tvBadge.setTextColor(textInt)
            } catch (e: Exception) {}
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        // Bind movement-tolerant touch gestures
        ForgivingTouchHelper.bind(
            view = holder.itemView,
            onClick = { onItemClick(item) },
            onLongClick = { view ->
                onItemLongClick(item, view)
                true
            }
        )
    }

    override fun getItemCount(): Int = items.size
}
