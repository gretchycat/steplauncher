package com.steplauncher.core.vfs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * VFS Program Manager: Manages a hierarchical virtual filesystem of directories and application shortcuts.
 * Supports recursive directory nesting, multi-directory icon placements, and auto-synchronization for newly installed packages.
 */
object VfsProgramManager {

    private const val PREFS_NAME = "steplauncher_vfs_prefs"
    private const val KEY_VFS_TREE = "key_vfs_tree"

    var rootNode: VfsNode = createDefaultRoot()
        private set

    private fun createDefaultRoot(): VfsNode {
        val root = VfsNode(
            name = "VFS",
            path = "/VFS",
            isDirectory = true,
            iconSymbol = "💻"
        )
        // Add standard category directories by default
        VfsCategory.values().forEach { category ->
            root.children.add(
                VfsNode(
                    name = category.displayName,
                    path = "/VFS/${category.displayName}",
                    isDirectory = true,
                    category = category,
                    iconSymbol = category.iconSymbol
                )
            )
        }
        return root
    }

    /**
     * Initializes the VFS structure from SharedPreferences, or runs initial auto-synchronization.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_VFS_TREE, null)

        if (!jsonStr.isNullOrEmpty()) {
            try {
                val json = JSONObject(jsonStr)
                rootNode = deserializeNode(json)
            } catch (e: Exception) {
                e.printStackTrace()
                rootNode = createDefaultRoot()
            }
        } else {
            rootNode = createDefaultRoot()
        }

        // Run initial synchronization to ensure all installed apps are assigned
        synchronizeVfs(context)
    }

    /**
     * Synchronization routine: Walks through the VFS tree recursively to collect all package names
     * currently placed in any directory. Any installed application not in any directory is automatically
     * categorized and added to its default category directory.
     */
    fun synchronizeVfs(context: Context): VfsNode {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedActivities = pm.queryIntentActivities(mainIntent, 0)

        // 1. Walk the tree recursively to gather all package names currently placed in ANY directory
        val placedPackages = mutableSetOf<String>()
        collectPlacedPackages(rootNode, placedPackages)

        var hasChanges = false

        // 2. Iterate over installed launcher applications
        for (resolveInfo in installedActivities) {
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()

            // If application is NOT in any directory yet:
            if (!placedPackages.contains(pkg)) {
                val category = DefaultAppResolver.categorizeApp(pkg, label, appInfo)
                val targetCategoryDir = findOrCreateCategoryDirectory(category)

                val appNode = VfsNode(
                    name = label,
                    path = "${targetCategoryDir.path}/$label",
                    isDirectory = false,
                    targetPackage = pkg,
                    category = category,
                    iconSymbol = category.iconSymbol
                )

                targetCategoryDir.children.add(appNode)
                placedPackages.add(pkg)
                hasChanges = true
            }
        }

        if (hasChanges) {
            saveState(context)
        }

        return rootNode
    }

    /**
     * Recursively walks through the VFS tree and collects all target package names.
     */
    private fun collectPlacedPackages(node: VfsNode, result: MutableSet<String>) {
        if (!node.isDirectory && !node.targetPackage.isNullOrEmpty()) {
            result.add(node.targetPackage)
        }
        for (child in node.children) {
            collectPlacedPackages(child, result)
        }
    }

    /**
     * Finds or creates a top-level category directory (e.g. /VFS/Games).
     */
    private fun findOrCreateCategoryDirectory(category: VfsCategory): VfsNode {
        val existing = rootNode.children.firstOrNull { it.isDirectory && (it.category == category || it.name.equals(category.displayName, ignoreCase = true)) }
        if (existing != null) return existing

        val newDir = VfsNode(
            name = category.displayName,
            path = "/VFS/${category.displayName}",
            isDirectory = true,
            category = category,
            iconSymbol = category.iconSymbol
        )
        rootNode.children.add(newDir)
        return newDir
    }

    /**
     * Recursively creates a new directory under a specified parent directory path.
     */
    fun createDirectory(parentPath: String, dirName: String, context: Context): VfsNode? {
        val parent = findNodeByPath(rootNode, parentPath) ?: rootNode
        if (!parent.isDirectory) return null

        val newPath = "${parent.path}/$dirName"
        val newDir = VfsNode(
            name = dirName,
            path = newPath,
            isDirectory = true,
            iconSymbol = "📁"
        )
        parent.children.add(newDir)
        saveState(context)
        return newDir
    }

    /**
     * Adds an application shortcut to a specific directory path.
     */
    fun addAppToDirectory(targetDirPath: String, packageName: String, label: String, iconSymbol: String = "📱", context: Context): Boolean {
        val targetDir = findNodeByPath(rootNode, targetDirPath) ?: return false
        if (!targetDir.isDirectory) return false

        val appNode = VfsNode(
            name = label,
            path = "${targetDir.path}/$label",
            isDirectory = false,
            targetPackage = packageName,
            iconSymbol = iconSymbol
        )
        targetDir.children.add(appNode)
        saveState(context)
        return true
    }

    /**
     * Recursively deletes a node by path.
     */
    fun deleteNode(targetPath: String, context: Context): Boolean {
        val deleted = deleteNodeRecursive(rootNode, targetPath)
        if (deleted) {
            saveState(context)
        }
        return deleted
    }

    private fun deleteNodeRecursive(current: VfsNode, targetPath: String): Boolean {
        val iterator = current.children.iterator()
        while (iterator.hasNext()) {
            val child = iterator.next()
            if (child.path == targetPath) {
                iterator.remove()
                return true
            }
            if (child.isDirectory && deleteNodeRecursive(child, targetPath)) {
                return true
            }
        }
        return false
    }

    /**
     * Finds a node recursively by path.
     */
    fun findNodeByPath(current: VfsNode, targetPath: String): VfsNode? {
        if (current.path == targetPath) return current
        for (child in current.children) {
            if (child.path == targetPath) return child
            if (child.isDirectory && targetPath.startsWith(child.path)) {
                val found = findNodeByPath(child, targetPath)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * Saves VFS tree to SharedPreferences.
     */
    fun saveState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = serializeNode(rootNode)
            prefs.edit().putString(KEY_VFS_TREE, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeNode(node: VfsNode): JSONObject {
        val json = JSONObject()
        json.put("name", node.name)
        json.put("path", node.path)
        json.put("isDirectory", node.isDirectory)
        json.put("iconSymbol", node.iconSymbol)
        if (node.category != null) {
            json.put("category", node.category.name)
        }
        if (!node.targetPackage.isNullOrEmpty()) {
            json.put("targetPackage", node.targetPackage)
        }
        if (node.isDirectory && node.children.isNotEmpty()) {
            val arr = JSONArray()
            node.children.forEach { arr.put(serializeNode(it)) }
            json.put("children", arr)
        }
        return json
    }

    private fun deserializeNode(json: JSONObject): VfsNode {
        val name = json.optString("name", "Folder")
        val path = json.optString("path", "/VFS/$name")
        val isDirectory = json.optBoolean("isDirectory", true)
        val iconSymbol = json.optString("iconSymbol", if (isDirectory) "📁" else "📱")
        val targetPackage = if (json.has("targetPackage")) json.getString("targetPackage") else null
        val categoryStr = if (json.has("category")) json.getString("category") else null
        val category = if (!categoryStr.isNullOrEmpty()) {
            try { VfsCategory.valueOf(categoryStr) } catch (e: Exception) { null }
        } else null

        val node = VfsNode(
            name = name,
            path = path,
            isDirectory = isDirectory,
            category = category,
            targetPackage = targetPackage,
            iconSymbol = iconSymbol
        )

        if (json.has("children")) {
            val arr = json.getJSONArray("children")
            for (i in 0 until arr.length()) {
                node.children.add(deserializeNode(arr.getJSONObject(i)))
            }
        }
        return node
    }
}
