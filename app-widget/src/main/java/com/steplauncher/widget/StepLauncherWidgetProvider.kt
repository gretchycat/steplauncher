package com.steplauncher.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.steplauncher.core.renderer.DockManager
import com.steplauncher.core.renderer.DockTile

/**
 * AppWidgetProvider allowing the StepLauncher Dock system to function as a home screen widget
 * in third-party launchers.
 */
class StepLauncherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        DockManager.initializeDefaults(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            DockManager.initializeDefaults(context)

            val views = RemoteViews(context.packageName, R.layout.widget_step_launcher)

            // Top Right Main Dock Content
            val topRightSb = StringBuilder()
            DockManager.topRightDockTiles.forEach { tile ->
                topRightSb.append("${tile.iconSymbol} ${tile.title}\n")
            }
            views.setTextViewText(R.id.widget_top_right_content, topRightSb.toString().trimEnd())

            // Bottom Left Running Tasks Stack Content
            val bottomLeftSb = StringBuilder()
            DockManager.bottomLeftDockTiles.forEach { tile ->
                bottomLeftSb.append("${tile.iconSymbol} ${tile.title}\n")
            }
            views.setTextViewText(R.id.widget_top_left_content, bottomLeftSb.toString().trimEnd())

            // Bottom Dock Content
            val bottomSb = StringBuilder()
            DockManager.bottomDockTiles.forEachIndexed { idx, tile ->
                if (idx > 0) bottomSb.append(" | ")
                bottomSb.append("${tile.iconSymbol} ${tile.title}")
            }
            views.setTextViewText(R.id.widget_bottom_dock_content, bottomSb.toString())

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
