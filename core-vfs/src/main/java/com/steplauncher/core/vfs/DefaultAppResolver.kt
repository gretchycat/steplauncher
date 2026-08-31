package com.steplauncher.core.vfs

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

data class ResolvedAppInfo(
    val label: String,
    val packageName: String,
    val className: String?,
    val launchIntent: Intent,
    val iconSymbol: String = "📱"
)

object DefaultAppResolver {

    /**
     * Resolves the default Phone/Dialer application on the device.
     */
    fun resolvePhoneApp(context: Context): ResolvedAppInfo {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:")
        }
        val pm = context.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: pm.resolveActivity(Intent(Intent.ACTION_DIAL), 0)

        if (resolveInfo != null) {
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: intent
            return ResolvedAppInfo(
                label = label.ifBlank { "Phone" },
                packageName = pkg,
                className = resolveInfo.activityInfo.name,
                launchIntent = launchIntent,
                iconSymbol = "📞"
            )
        }

        // Fallback standard dial intent
        return ResolvedAppInfo(
            label = "Phone",
            packageName = "com.android.dialer",
            className = null,
            launchIntent = Intent(Intent.ACTION_DIAL),
            iconSymbol = "📞"
        )
    }

    /**
     * Resolves the default Web Browser application on the device.
     */
    fun resolveBrowserApp(context: Context): ResolvedAppInfo {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        val pm = context.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        if (resolveInfo != null) {
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: intent
            return ResolvedAppInfo(
                label = label.ifBlank { "Browser" },
                packageName = pkg,
                className = resolveInfo.activityInfo.name,
                launchIntent = launchIntent,
                iconSymbol = "🌐"
            )
        }

        return ResolvedAppInfo(
            label = "Browser",
            packageName = "com.android.chrome",
            className = null,
            launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://")),
            iconSymbol = "🌐"
        )
    }

    /**
     * Resolves the default Camera application on the device.
     */
    fun resolveCameraApp(context: Context): ResolvedAppInfo {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        val pm = context.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: pm.resolveActivity(intent, 0)

        if (resolveInfo != null) {
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: intent
            return ResolvedAppInfo(
                label = label.ifBlank { "Camera" },
                packageName = pkg,
                className = resolveInfo.activityInfo.name,
                launchIntent = launchIntent,
                iconSymbol = "📷"
            )
        }

        return ResolvedAppInfo(
            label = "Camera",
            packageName = "com.android.camera",
            className = null,
            launchIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            iconSymbol = "📷"
        )
    }

    /**
     * Scans installed applications and resolves apps belonging to specific categories (e.g. Social, Multimedia).
     */
    fun resolveCategoryApps(context: Context, category: VfsCategory): List<ResolvedAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(mainIntent, 0)
        val result = mutableListOf<ResolvedAppInfo>()

        for (resolveInfo in activities) {
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: continue

            val isMatch = when (category) {
                VfsCategory.SOCIAL_MEDIA -> isSocialPackage(pkg, label, appInfo)
                VfsCategory.MULTIMEDIA -> isMultimediaPackage(pkg, label, appInfo)
                else -> false
            }

            if (isMatch) {
                result.add(
                    ResolvedAppInfo(
                        label = label,
                        packageName = pkg,
                        className = resolveInfo.activityInfo.name,
                        launchIntent = launchIntent,
                        iconSymbol = category.iconSymbol
                    )
                )
            }
        }
        return result
    }

    private fun isSocialPackage(pkg: String, label: String, appInfo: ApplicationInfo): Boolean {
        val lower = (pkg + " " + label).lowercase()
        if (lower.contains("social") || lower.contains("messenger") || lower.contains("chat") ||
            lower.contains("twitter") || lower.contains("x") || lower.contains("whatsapp") ||
            lower.contains("telegram") || lower.contains("discord") || lower.contains("instagram") ||
            lower.contains("facebook") || lower.contains("reddit") || lower.contains("signal")
        ) {
            return true
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (appInfo.category == ApplicationInfo.CATEGORY_SOCIAL) return true
        }
        return false
    }

    private fun isMultimediaPackage(pkg: String, label: String, appInfo: ApplicationInfo): Boolean {
        val lower = (pkg + " " + label).lowercase()
        if (lower.contains("media") || lower.contains("video") || lower.contains("music") ||
            lower.contains("audio") || lower.contains("player") || lower.contains("youtube") ||
            lower.contains("vlc") || lower.contains("spotify") || lower.contains("netflix") ||
            lower.contains("gallery") || lower.contains("photos")
        ) {
            return true
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (appInfo.category == ApplicationInfo.CATEGORY_AUDIO ||
                appInfo.category == ApplicationInfo.CATEGORY_VIDEO ||
                appInfo.category == ApplicationInfo.CATEGORY_IMAGE
            ) return true
        }
        return false
    }
}
