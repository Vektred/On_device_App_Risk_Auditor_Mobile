package com.riskauditor.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import com.riskauditor.core.categorizer.PermissionCategorizer
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.PermissionFact
import com.riskauditor.core.model.ProtectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android system inventory scanner.
 * Extracts installed applications, permissions, installer origin, and special access capabilities.
 */
class AppScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val specialAccessDetector = SpecialAccessDetector(context)

    suspend fun scanAllApps(includeSystemApps: Boolean = false): List<AppFacts> = withContext(Dispatchers.Default) {
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS

        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        packages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            // Exclude pure system apps unless explicitly requested by the user toggle
            if (isSystem && !isUpdatedSystem && !includeSystemApps) {
                return@mapNotNull null
            }

            extractAppFacts(packageInfo, appInfo, isSystem, isUpdatedSystem)
        }
    }

    suspend fun scanSinglePackage(packageName: String): AppFacts? = withContext(Dispatchers.Default) {
        try {
            val flags = PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
            val appInfo = packageInfo.applicationInfo ?: return@withContext null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            extractAppFacts(packageInfo, appInfo, isSystem, isUpdatedSystem)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractAppFacts(
        packageInfo: PackageInfo,
        appInfo: ApplicationInfo,
        isSystem: Boolean,
        isUpdatedSystem: Boolean
    ): AppFacts {
        val appName = try {
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            packageInfo.packageName
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val installerPackageName = getInstallerPackage(packageInfo.packageName)

        // Declared vs granted permissions (handling null for permission-less apps!)
        val requested = packageInfo.requestedPermissions ?: emptyArray()
        val flags = packageInfo.requestedPermissionsFlags ?: IntArray(requested.size)

        val permissions = requested.indices.map { i ->
            val permName = requested[i]
            val isGranted = (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            val protection = resolveProtectionLevel(permName)
            val category = PermissionCategorizer.categorize(permName)

            PermissionFact(
                name = permName,
                granted = isGranted,
                protectionLevel = protection,
                category = category
            )
        }

        // Special access detection
        val specialAccess = specialAccessDetector.detectSpecialAccess(packageInfo)

        // Static manifest indicators
        val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val usesCleartext = (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        val appCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appInfo.category
        } else {
            -1
        }

        val exportedCount = (packageInfo.activities?.count { it.exported } ?: 0) +
                (packageInfo.services?.count { it.exported } ?: 0) +
                (packageInfo.receivers?.count { it.exported } ?: 0) +
                (packageInfo.providers?.count { it.exported } ?: 0)

        return AppFacts(
            packageName = packageInfo.packageName,
            appName = appName,
            versionName = packageInfo.versionName,
            versionCode = versionCode,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            targetSdkVersion = appInfo.targetSdkVersion,
            installerPackageName = installerPackageName,
            isSystemApp = isSystem,
            isUpdatedSystemApp = isUpdatedSystem,
            appCategory = appCategory,
            permissions = permissions,
            specialAccess = specialAccess,
            isDebuggable = isDebuggable,
            usesCleartextTraffic = usesCleartext,
            exportedComponentsCount = exportedCount
        )
    }

    private fun getInstallerPackage(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveProtectionLevel(permissionName: String): ProtectionLevel {
        return try {
            val permInfo = packageManager.getPermissionInfo(permissionName, 0)
            val base = permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
            val isAppOp = (permInfo.protectionLevel and PermissionInfo.PROTECTION_FLAG_APPOP) != 0

            when {
                isAppOp || base == PermissionInfo.PROTECTION_SIGNATURE && isAppOp -> ProtectionLevel.SPECIAL
                base == PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.DANGEROUS
                base == PermissionInfo.PROTECTION_SIGNATURE -> ProtectionLevel.SIGNATURE
                base == PermissionInfo.PROTECTION_NORMAL -> ProtectionLevel.NORMAL
                else -> ProtectionLevel.CUSTOM
            }
        } catch (e: Exception) {
            ProtectionLevel.CUSTOM
        }
    }
}

