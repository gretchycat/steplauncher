package com.steplauncher.core.vfs

/**
 * Node model for the Virtual Filesystem tree (Program Organizer).
 * Serves as placeholder or real file structure representation.
 */
data class VfsNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val category: VfsCategory? = null,
    val children: List<VfsNode> = emptyList(),
    val targetPackage: String? = null
)
