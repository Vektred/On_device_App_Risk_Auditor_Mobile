package com.riskauditor.core.analyzer.rules

import com.riskauditor.core.analyzer.Rule
import com.riskauditor.core.analyzer.ScoringRubric
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.Finding
import com.riskauditor.core.model.PermissionCategory
import com.riskauditor.core.model.Severity

object AccessibilityRule : Rule {
    override val ruleId = "SA_ACCESSIBILITY"

    override fun evaluate(app: AppFacts): Finding? {
        val sa = app.specialAccess
        return when {
            sa.accessibilityEnabled -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_ACCESSIBILITY_ENABLED,
                severity = Severity.CRITICAL,
                title = "Accessibility Service is ACTIVE",
                plainExplanation = "It can read your entire screen, inspect UI elements, and tap buttons on your behalf. Banking trojans abuse this to capture credentials and perform unauthorized transactions.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            sa.accessibilityDeclared -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_ACCESSIBILITY_DECLARED,
                severity = Severity.HIGH,
                title = "Declares Accessibility Service",
                plainExplanation = "The app bundles an Accessibility Service. If enabled by the user, it can read keystrokes and screen content.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            else -> null
        }
    }
}

object DeviceAdminRule : Rule {
    override val ruleId = "SA_DEVICE_ADMIN"

    override fun evaluate(app: AppFacts): Finding? {
        val sa = app.specialAccess
        return when {
            sa.deviceAdminActive -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_DEVICE_ADMIN_ACTIVE,
                severity = Severity.CRITICAL,
                title = "Device Administrator is ACTIVE",
                plainExplanation = "Holds enterprise-level control over the device. Active device admins block uninstallation until deactivated in Settings.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            sa.deviceAdminDeclared -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_DEVICE_ADMIN_DECLARED,
                severity = Severity.HIGH,
                title = "Declares Device Admin Receiver",
                plainExplanation = "Contains code to register as a Device Administrator, which can prevent uninstallation and wipe device data if activated.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            else -> null
        }
    }
}

object NotificationListenerRule : Rule {
    override val ruleId = "SA_NOTIFICATION_LISTENER"

    override fun evaluate(app: AppFacts): Finding? {
        val sa = app.specialAccess
        return when {
            sa.notificationListenerEnabled -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_NOTIFICATION_LISTENER_ENABLED,
                severity = Severity.CRITICAL,
                title = "Notification Access is ENABLED",
                plainExplanation = "Can intercept all incoming notifications, including two-factor verification SMS/codes, private WhatsApp/Signal chats, and banking alerts.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            sa.notificationListenerDeclared -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_NOTIFICATION_LISTENER_DECLARED,
                severity = Severity.MEDIUM,
                title = "Declares Notification Listener Service",
                plainExplanation = "Bundles a service that can read all notification contents once granted user permission.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            else -> null
        }
    }
}

object OverlayRule : Rule {
    override val ruleId = "SA_OVERLAY"

    override fun evaluate(app: AppFacts): Finding? {
        val sa = app.specialAccess
        val declared = sa.overlayDeclared || app.hasDeclared("SYSTEM_ALERT_WINDOW")
        val granted = sa.overlayGranted || app.hasGranted("SYSTEM_ALERT_WINDOW")

        return when {
            granted -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_OVERLAY_GRANTED,
                severity = Severity.HIGH,
                title = "Display Over Other Apps GRANTED",
                plainExplanation = "Can draw floating windows over any running application. Malware uses overlays to spoof legitimate login screens and steal banking credentials.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            declared -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_OVERLAY_DECLARED,
                severity = Severity.MEDIUM,
                title = "Requests Overlay Permission",
                plainExplanation = "Requests permission to draw floating overlays over other applications.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            else -> null
        }
    }
}

object InstallPackagesRule : Rule {
    override val ruleId = "SA_INSTALL_PACKAGES"

    override fun evaluate(app: AppFacts): Finding? {
        val sa = app.specialAccess
        val declared = sa.installPackagesDeclared || app.hasDeclared("REQUEST_INSTALL_PACKAGES")
        val granted = sa.installPackagesGranted || app.hasGranted("REQUEST_INSTALL_PACKAGES")

        return when {
            granted -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_INSTALL_PACKAGES_GRANTED,
                severity = Severity.HIGH,
                title = "Install Unknown Apps GRANTED",
                plainExplanation = "Can prompt the user to install external APK files from outside the official app store, acting as a gateway for second-stage malware.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            declared -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_INSTALL_PACKAGES_DECLARED,
                severity = Severity.MEDIUM,
                title = "Requests Install Unknown Apps",
                plainExplanation = "Requests capability to install additional application packages onto the device.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            else -> null
        }
    }
}

object UsageStatsRule : Rule {
    override val ruleId = "SA_USAGE_STATS"

    override fun evaluate(app: AppFacts): Finding? {
        val sa = app.specialAccess
        val declared = sa.usageStatsDeclared || app.hasDeclared("PACKAGE_USAGE_STATS")
        val granted = sa.usageStatsGranted || app.hasGranted("PACKAGE_USAGE_STATS")

        return when {
            granted -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_USAGE_STATS_GRANTED,
                severity = Severity.MEDIUM,
                title = "App Usage Access GRANTED",
                plainExplanation = "Can monitor which apps you open and how long you spend in them, allowing it to detect when banking or crypto apps are launched.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            declared -> Finding(
                ruleId = ruleId,
                points = ScoringRubric.WEIGHT_USAGE_STATS_DECLARED,
                severity = Severity.LOW,
                title = "Requests App Usage Access",
                plainExplanation = "Requests access to observe application usage history and foreground app transitions.",
                category = PermissionCategory.SPECIAL_ACCESS
            )
            else -> null
        }
    }
}

