package com.riskauditor.core.analyzer.rules

import com.riskauditor.core.analyzer.ScoringRubric
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.Finding
import com.riskauditor.core.model.PermissionCategory
import com.riskauditor.core.model.Severity
import kotlin.math.roundToInt

/**
 * Computes individual permission findings and enforces category caps according to the scoring rubric.
 */
object CategoryWeightCalculator {

    data class CategoryCalculationResult(
        val cappedPoints: Int,
        val findings: List<Finding>
    )

    fun calculateCategory(
        app: AppFacts,
        category: PermissionCategory
    ): CategoryCalculationResult {
        val findings = mutableListOf<Finding>()
        var rawCategorySum = 0

        // If category is Special Access, evaluate the special access rules
        if (category == PermissionCategory.SPECIAL_ACCESS) {
            val specialRules = listOf(
                AccessibilityRule,
                DeviceAdminRule,
                NotificationListenerRule,
                OverlayRule,
                InstallPackagesRule,
                UsageStatsRule
            )
            for (rule in specialRules) {
                rule.evaluate(app)?.let { finding ->
                    findings.add(finding)
                    rawCategorySum += finding.points
                }
            }
        }

        // Evaluate permissions belonging to this category
        val perms = app.getPermissionsByCategory(category)
        for (perm in perms) {
            val baseWeight = ScoringRubric.PERMISSION_BASE_WEIGHTS[perm.name] ?: when (category) {
                PermissionCategory.SPECIAL_ACCESS -> 8
                PermissionCategory.SMS -> 10
                PermissionCategory.CONTACTS_AND_CALLS -> 6
                PermissionCategory.CAMERA_AND_MIC -> 5
                PermissionCategory.LOCATION -> 5
                PermissionCategory.PHONE_AND_IDENTITY -> 3
                PermissionCategory.STORAGE_AND_MEDIA -> 3
                PermissionCategory.BACKGROUND_AND_PERSISTENCE -> 2
                PermissionCategory.NETWORK -> 1
                PermissionCategory.OTHER -> 1
            }

            val points = if (perm.granted) {
                baseWeight
            } else {
                (baseWeight * ScoringRubric.DECLARED_ONLY_MULTIPLIER).roundToInt().coerceAtLeast(1)
            }

            val statusText = if (perm.granted) "GRANTED (Active)" else "Declared (Inactive)"
            val severity = when {
                points >= 15 -> Severity.CRITICAL
                points >= 8 -> Severity.HIGH
                points >= 4 -> Severity.MEDIUM
                else -> Severity.LOW
            }

            val finding = Finding(
                ruleId = "PERM_${perm.simpleName}",
                points = points,
                severity = severity,
                title = "${perm.simpleName} is $statusText",
                plainExplanation = "App requests access to ${perm.simpleName}. In category ${category.displayName}: ${category.whyItMatters}",
                category = category
            )
            findings.add(finding)
            rawCategorySum += points
        }

        val cap = ScoringRubric.CATEGORY_CAPS[category] ?: category.maxCategoryCap
        val cappedPoints = rawCategorySum.coerceAtMost(cap)

        return CategoryCalculationResult(
            cappedPoints = cappedPoints,
            findings = findings
        )
    }
}

