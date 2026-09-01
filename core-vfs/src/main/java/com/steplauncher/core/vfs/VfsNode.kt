package com.steplauncher.core.vfs

/**
 * Node model for the Virtual Filesystem tree (Program Organizer).
 * Represents directories or application shortcuts in a hierarchical tree.
 */
data class VfsNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val category: VfsCategory? = null,
    val targetPackage: String? = null,
    val iconSymbol: String = if (isDirectory) "📁" else "📱",
    val children: MutableList<VfsNode> = mutableListOf()
)
