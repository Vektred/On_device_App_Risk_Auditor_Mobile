package com.riskauditor.core.analyzer

import com.riskauditor.core.analyzer.rules.*
import com.riskauditor.core.model.*
import kotlin.math.min

/**
 * Pure Kotlin Explainable Risk Engine.
 * Converts AppFacts into a deterministic RiskResult with points, bands, and human-readable reasons.
 * Zero Android imports for blazing-fast laptop execution and unit testing.
 */
object RiskEngine {

    private val combinationRules = listOf(
        AccessibilityPlusOverlayRule,
        SmsExfiltrationRule,
        DeviceAdminNonMdmRule,
        NotificationExfiltrationRule,
        DropperPatternRule,
        CovertRecordingRule,
        WormSpreadingRule
    )

    private val modifierRules = listOf(
        SideloadedModifierRule,
        OldTargetSdkRule,
        DebuggableBuildRule,
        CleartextTrafficRule
    )

    fun analyze(app: AppFacts): RiskResult {
        val categoryPoints = mutableMapOf<PermissionCategory, Int>()
        val allFindings = mutableListOf<Finding>()
        var categoryPointsSum = 0

        // 1. Calculate points per category with caps enforced
        for (category in PermissionCategory.values()) {
            val catResult = CategoryWeightCalculator.calculateCategory(app, category)
            categoryPoints[category] = catResult.cappedPoints
            categoryPointsSum += catResult.cappedPoints
            allFindings.addAll(catResult.findings)
        }

        // 2. Combination rules
        val comboFindings = mutableListOf<Finding>()
        for (rule in combinationRules) {
            rule.evaluate(app)?.let {
                comboFindings.add(it)
                allFindings.add(it)
            }
        }
        val comboSum = comboFindings.sumOf { it.points }

        // 3. Context mismatch rule
        val contextFindings = mutableListOf<Finding>()
        ContextMismatchRule.evaluate(app)?.let {
            contextFindings.add(it)
            allFindings.add(it)
        }
        val contextSum = contextFindings.sumOf { it.points }

        // 4. Modifiers
        val modifierFindings = mutableListOf<Finding>()
        for (rule in modifierRules) {
            rule.evaluate(app)?.let {
                modifierFindings.add(it)
                allFindings.add(it)
            }
        }
        val modifierSum = modifierFindings.sumOf { it.points }

        // 5. Total score (capped at 100)
        val rawScore = categoryPointsSum + comboSum + contextSum + modifierSum
        val finalScore = min(100, rawScore.coerceAtLeast(0))
        val band = RiskBand.fromScore(finalScore)

        // 6. Reasons selection (prioritizing high-impact combination & context rules, then top category/perm findings)
        val priorityFindings = mutableListOf<Finding>()
        priorityFindings.addAll(comboFindings)
        priorityFindings.addAll(contextFindings)
        priorityFindings.addAll(
            allFindings.filter { it.severity == Severity.CRITICAL && !priorityFindings.contains(it) }
        )
        priorityFindings.addAll(
            allFindings.filter { it.severity == Severity.HIGH && !priorityFindings.contains(it) }
        )
        priorityFindings.addAll(
            allFindings.filter { it !in priorityFindings }.sortedByDescending { it.points }
        )

        // Invariant: High or Critical app needs >= 3 reasons (or as many findings as exist)
        val minReasonsCount = if (band == RiskBand.HIGH || band == RiskBand.CRITICAL) 3 else 1
        val topReasons = if (priorityFindings.size <= minReasonsCount) {
            priorityFindings.toList()
        } else {
            // Take at least minReasonsCount, and include any significant finding
            priorityFindings.take(maxOf(minReasonsCount, priorityFindings.filter { it.points >= 8 }.size.coerceAtMost(6)))
        }

        // 7. Remediation recommendations
        val actions = mutableListOf<RemediationAction>()
        val sa = app.specialAccess

        if (sa.deviceAdminActive) {
            actions.add(
                RemediationAction(
                    title = "Deactivate Device Administrator",
                    description = "Active Device Admin blocks uninstallation. You must turn it OFF in Security settings first.",
                    actionType = ActionType.MANAGE_DEVICE_ADMIN
                )
            )
        }
        if (sa.accessibilityEnabled) {
            actions.add(
                RemediationAction(
                    title = "Disable Accessibility Service",
                    description = "Revoke screen-reading and automated click privileges in Accessibility settings.",
                    actionType = ActionType.REVOKE_ACCESSIBILITY
                )
            )
        }
        if (sa.overlayGranted || sa.overlayDeclared || app.hasGranted("SYSTEM_ALERT_WINDOW")) {
            actions.add(
                RemediationAction(
                    title = "Revoke Overlay (Display Over Apps)",
                    description = "Block the app from drawing floating windows over your screen.",
                    actionType = ActionType.REVOKE_OVERLAY
                )
            )
        }
        if (sa.notificationListenerEnabled) {
            actions.add(
                RemediationAction(
                    title = "Disable Notification Access",
                    description = "Stop the app from reading incoming notifications and 2FA codes.",
                    actionType = ActionType.REVOKE_NOTIFICATION_LISTENER
                )
            )
        }
        if (sa.installPackagesGranted || sa.installPackagesDeclared || app.hasGranted("REQUEST_INSTALL_PACKAGES")) {
            actions.add(
                RemediationAction(
                    title = "Revoke Install Unknown Apps",
                    description = "Prevent the app from installing untrusted APK packages.",
                    actionType = ActionType.REVOKE_INSTALL_PACKAGES
                )
            )
        }
        if (sa.usageStatsGranted) {
            actions.add(
                RemediationAction(
                    title = "Revoke Usage Access",
                    description = "Prevent tracking when you open banking and sensitive apps.",
                    actionType = ActionType.REVOKE_USAGE_STATS
                )
            )
        }
        // General app settings
        actions.add(
            RemediationAction(
                title = "App Permissions & Settings",
                description = "Open system settings to review and revoke runtime permissions.",
                actionType = ActionType.OPEN_APP_SETTINGS
            )
        )
        // Uninstall
        actions.add(
            RemediationAction(
                title = "Uninstall Application",
                description = "Permanently remove this app and its data from your device.",
                actionType = ActionType.UNINSTALL_APP
            )
        )

        return RiskResult(
            packageName = app.packageName,
            appName = app.appName,
            score = finalScore,
            band = band,
            allFindings = allFindings,
            reasons = topReasons,
            categoryPoints = categoryPoints,
            remediationActions = actions
        )
    }
}

