package com.steplauncher.core.ipc

import android.content.Intent
import android.os.Parcelable

/**
 * Descriptor for registering an external or internal dock application.
 */
data class DockAppDescriptor(
    val id: String,
    val title: String,
    val packageName: String,
    val targetDock: String = TARGET_DOCK_TOP_LEFT, // "TOP_LEFT" is default target as requested
    val iconResName: String? = null,
    val type: String = TYPE_EXTERNAL,
    val extraData: String? = null
) {
    companion object {
        const val TARGET_DOCK_TOP_LEFT = "TOP_LEFT"
        const val TARGET_DOCK_TOP_RIGHT = "TOP_RIGHT"
        const val TARGET_DOCK_BOTTOM = "BOTTOM"

        const val TYPE_TELEMETRY = "TELEMETRY"
        const val TYPE_LAUNCHER = "LAUNCHER"
        const val TYPE_WIDGET = "WIDGET"
        const val TYPE_EXTERNAL = "EXTERNAL"

        const val ACTION_REGISTER_DOCKAPP = "com.steplauncher.action.REGISTER_DOCKAPP"
        const val EXTRA_ID = "extra_dock_id"
        const val EXTRA_TITLE = "extra_dock_title"
        const val EXTRA_PACKAGE = "extra_dock_package"
        const val EXTRA_TARGET_DOCK = "extra_dock_target"
        const val EXTRA_ICON = "extra_dock_icon"
        const val EXTRA_TYPE = "extra_dock_type"
        const val EXTRA_DATA = "extra_dock_data"

        fun fromIntent(intent: Intent): DockAppDescriptor? {
            val id = intent.getStringExtra(EXTRA_ID) ?: return null
            val title = intent.getStringExtra(EXTRA_TITLE) ?: id
            val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: intent.`package` ?: "unknown.package"
            val target = intent.getStringExtra(EXTRA_TARGET_DOCK) ?: TARGET_DOCK_TOP_LEFT
            val icon = intent.getStringExtra(EXTRA_ICON)
            val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_EXTERNAL
            val data = intent.getStringExtra(EXTRA_DATA)

            return DockAppDescriptor(
                id = id,
                title = title,
                packageName = pkg,
                targetDock = target,
                iconResName = icon,
                type = type,
                extraData = data
            )
        }
    }

    fun toRegistrationIntent(): Intent {
        return Intent(ACTION_REGISTER_DOCKAPP).apply {
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_TARGET_DOCK, targetDock)
            putExtra(EXTRA_ICON, iconResName)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_DATA, extraData)
        }
    }
}
