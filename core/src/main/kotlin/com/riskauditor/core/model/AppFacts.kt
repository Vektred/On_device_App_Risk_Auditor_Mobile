package com.riskauditor.core.model

/**
 * Pure Kotlin snapshot of all facts extracted from an Android application.
 * Free of any Android framework dependencies for high-speed unit testing.
 */
data class AppFacts(
    val packageName: String,
    val appName: String,
    val versionName: String? = null,
    val versionCode: Long = 1L,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val targetSdkVersion: Int = 34,
    val installerPackageName: String? = null,
    val isSystemApp: Boolean = false,
    val isUpdatedSystemApp: Boolean = false,
    val appCategory: Int = -1, // e.g., ApplicationInfo.CATEGORY_UNDEFINED (-1)
    val permissions: List<PermissionFact> = emptyList(),
    val specialAccess: SpecialAccessFact = SpecialAccessFact(),
    val isDebuggable: Boolean = false,
    val usesCleartextTraffic: Boolean = false,
    val exportedComponentsCount: Int = 0
) {
    /**
     * Determines whether the app was sideloaded (via adb, file manager, browser, or package installer)
     * rather than an official app store like Google Play or Galaxy Store.
     * Note: Sideloaded / adb install returns null or packageinstaller.
     */
    val isSideloaded: Boolean
        get() = when (installerPackageName) {
            "com.android.vending",
            "com.sec.android.app.samsungapps",
            "com.amazon.venezia" -> false
            else -> true // null (adb install), packageinstaller, or third-party file
        }

    fun hasGranted(permissionSuffixOrName: String): Boolean {
        return permissions.any {
            (it.name == permissionSuffixOrName || it.name.endsWith(".$permissionSuffixOrName")) && it.granted
        }
    }

    fun hasDeclared(permissionSuffixOrName: String): Boolean {
        return permissions.any {
            it.name == permissionSuffixOrName || it.name.endsWith(".$permissionSuffixOrName")
        }
    }

    fun getPermissionsByCategory(category: PermissionCategory): List<PermissionFact> {
        return permissions.filter { it.category == category }
    }
}

