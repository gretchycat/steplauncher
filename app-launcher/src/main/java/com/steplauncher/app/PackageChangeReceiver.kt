package com.steplauncher.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.steplauncher.core.vfs.VfsProgramManager

/**
 * Listens for package installation, uninstallation, and update events on Android.
 * Automatically runs VFS auto-synchronization to place any unassigned new application into its category directory.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart

        Log.d("PackageChangeReceiver", "Package event received: action=$action, pkg=$packageName")

        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_CHANGED -> {
                // Trigger VFS Program Manager auto-synchronization
                VfsProgramManager.synchronizeVfs(context)

                // If LauncherActivity is running, refresh docks dynamically
                if (context is LauncherActivity) {
                    context.runOnUiThread {
                        context.refreshLauncherUi()
                    }
                }
            }
        }
    }
}
