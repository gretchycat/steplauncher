package com.steplauncher.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.steplauncher.core.ipc.DockAppDescriptor
import com.steplauncher.core.renderer.DockManager

/**
 * Receiver listening for external dock app registration signals.
 * Action: com.steplauncher.action.REGISTER_DOCKAPP
 */
class DockAppReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DockAppDescriptor.ACTION_REGISTER_DOCKAPP) {
            val descriptor = DockAppDescriptor.fromIntent(intent)
            if (descriptor != null) {
                Log.d("DockAppReceiver", "Received external dockapp registration: ${descriptor.title} for target ${descriptor.targetDock}")
                DockManager.registerExternalDockApp(descriptor)

                Toast.makeText(
                    context,
                    "Loaded external dock app: ${descriptor.title} on ${descriptor.targetDock} dock",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
