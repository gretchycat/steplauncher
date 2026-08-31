package com.steplauncher.core.vfs

/**
 * Standard app categories in the VFS program organizer.
 */
enum class VfsCategory(val displayName: String, val iconSymbol: String) {
    PHONE("Phone", "📞"),
    BROWSER("Browser", "🌐"),
    CAMERA("Camera", "📷"),
    SOCIAL_MEDIA("Social Media", "💬"),
    MULTIMEDIA("Multimedia", "🎬"),
    GAMES("Games", "🎮"),
    PRODUCTIVITY("Productivity", "💼"),
    DEVELOPMENT("Development", "⚡"),
    SYSTEM("System", "⚙️"),
    UNSORTED("Unsorted", "📦")
}
