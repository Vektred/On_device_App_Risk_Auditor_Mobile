package com.riskauditor.core.model

/**
 * Represents an individual permission declared and/or granted to an application.
 */
data class PermissionFact(
    val name: String,
    val simpleName: String = name.substringAfterLast('.'),
    val granted: Boolean,
    val protectionLevel: ProtectionLevel = ProtectionLevel.DANGEROUS,
    val category: PermissionCategory
)

/**
 * High-privilege special app access types that require separate system settings screens.
 */
data class SpecialAccessFact(
    val accessibilityDeclared: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val accessibilityCanRetrieveWindowContent: Boolean = false,

    val deviceAdminDeclared: Boolean = false,
    val deviceAdminActive: Boolean = false,

    val notificationListenerDeclared: Boolean = false,
    val notificationListenerEnabled: Boolean = false,

    val overlayDeclared: Boolean = false,
    val overlayGranted: Boolean = false,

    val installPackagesDeclared: Boolean = false,
    val installPackagesGranted: Boolean = false,

    val usageStatsDeclared: Boolean = false,
    val usageStatsGranted: Boolean = false,

    val vpnServiceDeclared: Boolean = false,
    val defaultSmsApp: Boolean = false,
    val defaultDialer: Boolean = false,
    val inputMethodEnabled: Boolean = false
)

