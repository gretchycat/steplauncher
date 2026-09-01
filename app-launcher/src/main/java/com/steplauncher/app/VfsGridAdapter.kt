package com.steplauncher.app

import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
 * Application shortcuts always use the application's native built-in icon unless a custom image URI is set.
 */
class VfsGridAdapter(
    private val items: List<VfsNode>,
    var isMultiSelectMode: Boolean = false,
    var selectedPackages: Set<String> = emptySet(),
    private val onItemClick: (VfsNode, Int) -> Unit,
    private val onItemLongClick: (VfsNode, View) -> Unit
) : RecyclerView.Adapter<VfsGridAdapter.VfsViewHolder>() {

    class VfsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_vfs_icon)
        val tvSymbol: TextView = itemView.findViewById(R.id.tv_vfs_symbol)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_vfs_title)
        val tvBadge: TextView = itemView.findViewById(R.id.tv_vfs_badge)
        val tvCheck: TextView = itemView.findViewById(R.id.tv_vfs_check)
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

        val pkg = item.targetPackage
        var isIconRendered = false

        // 1. Application Shortcuts: ALWAYS use built-in application icon (or custom image URI)
        if (!item.isDirectory && !pkg.isNullOrEmpty()) {
            val customIconUri = item.iconSymbol
            if (customIconUri.startsWith("content://") || customIconUri.startsWith("file://") || customIconUri.startsWith("/")) {
                try {
                    val uri = Uri.parse(customIconUri)
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    holder.ivIcon.setImageBitmap(bitmap)
                    holder.ivIcon.visibility = View.VISIBLE
                    holder.tvSymbol.visibility = View.GONE
                    isIconRendered = true
                } catch (e: Exception) {
                    isIconRendered = false
                }
            }

            if (!isIconRendered) {
                try {
                    val appDrawable = pm.getApplicationIcon(pkg)
                    holder.ivIcon.setImageDrawable(appDrawable)
                    holder.ivIcon.visibility = View.VISIBLE
                    holder.tvSymbol.visibility = View.GONE
                    isIconRendered = true
                } catch (e: Exception) {
                    isIconRendered = false
                }
            }
        }

        // 2. Directories or Category Links: Use assigned category vector drawable or custom image URI
        if (!isIconRendered) {
            val customIconUri = item.iconSymbol
            if (customIconUri.startsWith("content://") || customIconUri.startsWith("file://") || customIconUri.startsWith("/")) {
                try {
                    val uri = Uri.parse(customIconUri)
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    holder.ivIcon.setImageBitmap(bitmap)
                    holder.ivIcon.visibility = View.VISIBLE
                    holder.tvSymbol.visibility = View.GONE
                } catch (e: Exception) {
                    val fallbackRes = if (item.isDirectory) R.drawable.ic_cat_folder else R.drawable.ic_cat_app
                    holder.ivIcon.setImageResource(fallbackRes)
                    holder.ivIcon.visibility = View.VISIBLE
                    holder.tvSymbol.visibility = View.GONE
                }
            } else {
                val resId = context.resources.getIdentifier(customIconUri, "drawable", context.packageName)
                val iconRes = if (resId != 0) resId else (if (item.isDirectory) R.drawable.ic_cat_folder else R.drawable.ic_cat_app)
                holder.ivIcon.setImageResource(iconRes)
                holder.ivIcon.visibility = View.VISIBLE
                holder.tvSymbol.visibility = View.GONE
            }
        }

        // Multi-Select Checkbox State
        if (isMultiSelectMode && !item.isDirectory && !pkg.isNullOrEmpty()) {
            holder.tvCheck.visibility = View.VISIBLE
            holder.tvCheck.text = if (selectedPackages.contains(pkg)) "☑️" else "☐"
        } else {
            holder.tvCheck.visibility = View.GONE
        }

        // Attention State & Badge Check
        val targetPkg = item.targetPackage
        val isAttention = if (!targetPkg.isNullOrEmpty()) DockManager.isTileAttentionRequested(targetPkg) else false
        val badgeText = if (!targetPkg.isNullOrEmpty()) DockManager.getTileBadgeText(targetPkg) else null

        // Tile background tint swap if attention requested or selected
        val isSelected = !pkg.isNullOrEmpty() && selectedPackages.contains(pkg)
        val tintHex = when {
            isSelected -> "#00E5FF" // Highlight cyan when selected
            isAttention -> DockManager.attentionColorHex
            else -> DockManager.accentColorHex
        }

        if (!tintHex.isNullOrEmpty() && (tintHex != "#FFFFFF" || isAttention || isSelected)) {
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
            onClick = { onItemClick(item, holder.adapterPosition) },
            onLongClick = { view ->
                onItemLongClick(item, view)
                true
            }
        )
    }

    override fun getItemCount(): Int = items.size
}
