package com.riskauditor.scanner

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.riskauditor.core.model.SpecialAccessFact

/**
 * Detects high-privilege special app accesses across the Android system.
 * Handles both declared capabilities and active/enabled system grants.
 */
class SpecialAccessDetector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val accessibilityManager: AccessibilityManager? =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    private val devicePolicyManager: DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    private val appOpsManager: AppOpsManager? =
        context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    // Pre-fetched system-wide active sets for efficient batch scanning
    private val enabledAccessibilityPackages by lazy {
        try {
            accessibilityManager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.map { it.resolveInfo.serviceInfo.packageName }
                ?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private val activeAdminPackages by lazy {
        try {
            devicePolicyManager?.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private val enabledNotificationListeners by lazy {
        try {
            NotificationManagerCompat.getEnabledListenerPackages(context)
        } catch (e: Exception) {
            emptySet()
        }
    }

    private val defaultSmsPackage by lazy {
        try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: Exception) {
            null
        }
    }

    private val defaultDialerPackage by lazy {
        try {
            telecomManager?.defaultDialerPackage
        } catch (e: Exception) {
            null
        }
    }

    private val enabledInputMethodsString by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun detectSpecialAccess(packageInfo: PackageInfo): SpecialAccessFact {
        val pkg = packageInfo.packageName
        val appInfo = packageInfo.applicationInfo
        val uid = appInfo?.uid ?: Process.myUid()

        // 1. Accessibility
        val accessibilityDeclared = packageInfo.services?.any {
            it.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE"
        } ?: false
        val accessibilityEnabled = enabledAccessibilityPackages.contains(pkg)

        // 2. Device Admin
        val deviceAdminDeclared = packageInfo.receivers?.any {
            it.permission == "android.permission.BIND_DEVICE_ADMIN"
        } ?: false
        val deviceAdminActive = activeAdminPackages.contains(pkg)

        // 3. Notification Listener
        val notificationListenerDeclared = packageInfo.services?.any {
            it.permission == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        } ?: false
        val notificationListenerEnabled = enabledNotificationListeners.contains(pkg)

        // 4. Overlay (SYSTEM_ALERT_WINDOW)
        val overlayDeclared = packageInfo.requestedPermissions?.contains("android.permission.SYSTEM_ALERT_WINDOW") ?: false
        val overlayGranted = checkAppOp(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, pkg, overlayDeclared)

        // 5. Install Unknown Apps (REQUEST_INSTALL_PACKAGES)
        val installPackagesDeclared = packageInfo.requestedPermissions?.contains("android.permission.REQUEST_INSTALL_PACKAGES") ?: false
        val installPackagesGranted = checkAppOp(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES else "android:request_install_packages",
            uid,
            pkg,
            installPackagesDeclared
        )

        // 6. Usage Stats
        val usageStatsDeclared = packageInfo.requestedPermissions?.contains("android.permission.PACKAGE_USAGE_STATS") ?: false
        val usageStatsGranted = checkAppOp(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, pkg, usageStatsDeclared)

        // Extras
        val vpnDeclared = packageInfo.services?.any {
            it.permission == "android.permission.BIND_VPN_SERVICE"
        } ?: false

        val isDefaultSms = defaultSmsPackage == pkg
        val isDefaultDialer = defaultDialerPackage == pkg
        val isInputMethod = enabledInputMethodsString.contains(pkg)

        return SpecialAccessFact(
            accessibilityDeclared = accessibilityDeclared,
            accessibilityEnabled = accessibilityEnabled,
            accessibilityCanRetrieveWindowContent = accessibilityEnabled,
            deviceAdminDeclared = deviceAdminDeclared,
            deviceAdminActive = deviceAdminActive,
            notificationListenerDeclared = notificationListenerDeclared,
            notificationListenerEnabled = notificationListenerEnabled,
            overlayDeclared = overlayDeclared,
            overlayGranted = overlayGranted,
            installPackagesDeclared = installPackagesDeclared,
            installPackagesGranted = installPackagesGranted,
            usageStatsDeclared = usageStatsDeclared,
            usageStatsGranted = usageStatsGranted,
            vpnServiceDeclared = vpnDeclared,
            defaultSmsApp = isDefaultSms,
            defaultDialer = isDefaultDialer,
            inputMethodEnabled = isInputMethod
        )
    }

    private fun checkAppOp(op: String, uid: Int, pkg: String, defaultPermGranted: Boolean): Boolean {
        if (appOpsManager == null) return defaultPermGranted
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(op, uid, pkg)
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(op, uid, pkg)
            }
            when (mode) {
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> false
                else -> defaultPermGranted // MODE_DEFAULT fallback
            }
        } catch (e: Exception) {
            defaultPermGranted
        }
    }
}

