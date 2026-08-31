package com.steplauncher.widget

import org.json.JSONObject

/**
 * Metadata model representing an Android AppWidget pinned to a specific workspace background canvas.
 */
data class WorkspaceWidgetInfo(
    val id: String,
    val appWidgetId: Int,
    val workspaceIndex: Int,
    val providerPackageName: String,
    val providerClassName: String,
    var xDp: Int = 16,
    var yDp: Int = 16,
    var widthDp: Int = 220,
    var heightDp: Int = 160
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("appWidgetId", appWidgetId)
        json.put("workspaceIndex", workspaceIndex)
        json.put("providerPackageName", providerPackageName)
        json.put("providerClassName", providerClassName)
        json.put("xDp", xDp)
        json.put("yDp", yDp)
        json.put("widthDp", widthDp)
        json.put("heightDp", heightDp)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): WorkspaceWidgetInfo {
            return WorkspaceWidgetInfo(
                id = json.optString("id"),
                appWidgetId = json.optInt("appWidgetId"),
                workspaceIndex = json.optInt("workspaceIndex"),
                providerPackageName = json.optString("providerPackageName"),
                providerClassName = json.optString("providerClassName"),
                xDp = json.optInt("xDp", 16),
                yDp = json.optInt("yDp", 16),
                widthDp = json.optInt("widthDp", 220),
                heightDp = json.optInt("heightDp", 160)
            )
        }
    }
}
