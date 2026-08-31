package com.steplauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject

/**
 * Orchestrator for traditional Android AppWidget hosting across StepLauncher Workspaces.
 * Controls AppWidgetHost lifecycle, widget allocation, per-workspace canvas positioning, and state persistence.
 */
object WorkspaceWidgetHostManager {

    private const val APPWIDGET_HOST_ID = 2048
    private const val PREFS_NAME = "steplauncher_workspace_widgets_prefs"
    private const val KEY_WIDGETS_JSON = "key_workspace_widgets_json"

    private var appWidgetHost: AppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null
    private val widgetList = mutableListOf<WorkspaceWidgetInfo>()

    fun init(context: Context) {
        if (appWidgetHost == null) {
            appWidgetHost = AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID)
            appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            restoreWidgets(context)
        }
    }

    fun startListening() {
        appWidgetHost?.startListening()
    }

    fun stopListening() {
        appWidgetHost?.stopListening()
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost?.allocateAppWidgetId() ?: -1
    }

    fun deleteAppWidgetId(appWidgetId: Int, context: Context) {
        appWidgetHost?.deleteAppWidgetId(appWidgetId)
        widgetList.removeAll { it.appWidgetId == appWidgetId }
        saveWidgets(context)
    }

    fun addWidget(info: WorkspaceWidgetInfo, context: Context) {
        widgetList.removeAll { it.appWidgetId == info.appWidgetId }
        widgetList.add(info)
        saveWidgets(context)
    }

    fun getWidgetsForWorkspace(workspaceIndex: Int): List<WorkspaceWidgetInfo> {
        return widgetList.filter { it.workspaceIndex == workspaceIndex }
    }

    fun getAppWidgetProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return appWidgetManager?.getAppWidgetInfo(appWidgetId)
    }

    fun createHostView(context: Context, info: WorkspaceWidgetInfo): AppWidgetHostView? {
        val host = appWidgetHost ?: return null
        val manager = appWidgetManager ?: return null

        val providerInfo = manager.getAppWidgetInfo(info.appWidgetId) ?: return null
        val hostView = host.createView(context, info.appWidgetId, providerInfo)
        hostView.setAppWidget(info.appWidgetId, providerInfo)
        
        // Provide padding options bundle
        val opts = Bundle()
        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, info.widthDp)
        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, info.heightDp)
        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, info.widthDp + 50)
        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, info.heightDp + 50)
        hostView.updateAppWidgetOptions(opts)

        return hostView
    }

    private fun saveWidgets(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val array = JSONArray()
            widgetList.forEach { array.put(it.toJson()) }
            prefs.edit().putString(KEY_WIDGETS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreWidgets(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_WIDGETS_JSON, null) ?: return
            widgetList.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                widgetList.add(WorkspaceWidgetInfo.fromJson(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
