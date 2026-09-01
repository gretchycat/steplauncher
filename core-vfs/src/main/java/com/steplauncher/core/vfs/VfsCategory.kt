package com.steplauncher.core.vfs

/**
 * Standard app categories in the VFS program organizer.
 * Uses native built-in vector drawable resource names instead of emojis.
 */
enum class VfsCategory(val displayName: String, val iconSymbol: String) {
    PHONE("Phone", "ic_cat_phone"),
    BROWSER("Browser", "ic_cat_browser"),
    CAMERA("Camera", "ic_cat_camera"),
    SOCIAL_MEDIA("Social Media", "ic_cat_social"),
    MULTIMEDIA("Multimedia", "ic_cat_multimedia"),
    GAMES("Games", "ic_cat_games"),
    PRODUCTIVITY("Productivity", "ic_cat_productivity"),
    DEVELOPMENT("Development", "ic_cat_development"),
    SYSTEM("System", "ic_cat_system"),
    UNSORTED("Unsorted", "ic_cat_unsorted")
}
