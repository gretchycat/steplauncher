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
    var iconSymbol: String = if (isDirectory) "ic_cat_folder" else "ic_cat_app",
    val children: MutableList<VfsNode> = mutableListOf()
)
