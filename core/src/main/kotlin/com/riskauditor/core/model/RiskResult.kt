package com.riskauditor.core.model

enum class Severity(val label: String) {
    INFO("Info"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

data class Finding(
    val ruleId: String,
    val points: Int,
    val severity: Severity,
    val title: String,
    val plainExplanation: String,
    val category: PermissionCategory? = null
)

enum class RiskBand(
    val title: String,
    val minScore: Int,
    val maxScore: Int,
    val description: String
) {
    LOW(
        title = "Low",
        minScore = 0,
        maxScore = 24,
        description = "Standard or minimal permissions expected for basic functionality."
    ),
    MEDIUM(
        title = "Medium",
        minScore = 25,
        maxScore = 49,
        description = "Elevated permissions that could impact user privacy if misused."
    ),
    HIGH(
        title = "High",
        minScore = 50,
        maxScore = 74,
        description = "Significant capabilities or dangerous combinations requiring caution."
    ),
    CRITICAL(
        title = "Critical",
        minScore = 75,
        maxScore = 100,
        description = "Extreme danger; contains capabilities often abused by banking trojans and spyware."
    );

    companion object {
        fun fromScore(score: Int): RiskBand = when {
            score >= 75 -> CRITICAL
            score >= 50 -> HIGH
            score >= 25 -> MEDIUM
            else -> LOW
        }
    }
}

data class RiskResult(
    val packageName: String,
    val appName: String,
    val score: Int,
    val band: RiskBand,
    val allFindings: List<Finding>,
    val reasons: List<Finding>,
    val categoryPoints: Map<PermissionCategory, Int>,
    val remediationActions: List<RemediationAction> = emptyList()
)

data class RemediationAction(
    val title: String,
    val description: String,
    val actionType: ActionType,
    val targetSetting: String? = null
)

enum class ActionType {
    OPEN_APP_SETTINGS,
    REVOKE_OVERLAY,
    REVOKE_INSTALL_PACKAGES,
    REVOKE_ACCESSIBILITY,
    REVOKE_NOTIFICATION_LISTENER,
    REVOKE_USAGE_STATS,
    MANAGE_DEVICE_ADMIN,
    UNINSTALL_APP
}

