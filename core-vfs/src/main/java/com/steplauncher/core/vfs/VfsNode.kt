package com.steplauncher.core.vfs

/**
 * Node model for the Virtual Filesystem tree (Program Organizer).
 * Represents directories or application shortcuts in a hierarchical tree.
 */
data class VfsNode(
    var name: String,
    var path: String,
    val isDirectory: Boolean,
    val category: VfsCategory? = null,
    val targetPackage: String? = null,
    var iconSymbol: String = if (isDirectory) "📁" else "📱",
    val children: MutableList<VfsNode> = mutableListOf()
)
