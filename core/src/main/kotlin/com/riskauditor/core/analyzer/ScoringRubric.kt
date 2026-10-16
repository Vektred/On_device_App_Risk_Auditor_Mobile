package com.riskauditor.core.analyzer

import com.riskauditor.core.model.PermissionCategory

/**
 * Single source of truth for all weights, caps, and combination rules in the Risk Engine.
 * Used by RiskEngine to score apps and to export the scoring rubric deliverable.
 */
object ScoringRubric {

    // Declared permission modifier (multiplier applied when permission is only declared, not granted)
    const val DECLARED_ONLY_MULTIPLIER = 0.3

    // Category Caps (Section 4.2 starter weights)
    val CATEGORY_CAPS = mapOf(
        PermissionCategory.SPECIAL_ACCESS to 60,
        PermissionCategory.SMS to 25,
        PermissionCategory.CONTACTS_AND_CALLS to 15,
        PermissionCategory.CAMERA_AND_MIC to 14,
        PermissionCategory.LOCATION to 14,
        PermissionCategory.PHONE_AND_IDENTITY to 10,
        PermissionCategory.STORAGE_AND_MEDIA to 12,
        PermissionCategory.BACKGROUND_AND_PERSISTENCE to 8,
        PermissionCategory.NETWORK to 2,
        PermissionCategory.OTHER to 5
    )

    // Base Special Access Weights
    const val WEIGHT_ACCESSIBILITY_ENABLED = 35
    const val WEIGHT_ACCESSIBILITY_DECLARED = 8

    const val WEIGHT_DEVICE_ADMIN_ACTIVE = 30
    const val WEIGHT_DEVICE_ADMIN_DECLARED = 6

    const val WEIGHT_NOTIFICATION_LISTENER_ENABLED = 25
    const val WEIGHT_NOTIFICATION_LISTENER_DECLARED = 6

    const val WEIGHT_OVERLAY_GRANTED = 20
    const val WEIGHT_OVERLAY_DECLARED = 8

    const val WEIGHT_INSTALL_PACKAGES_GRANTED = 15
    const val WEIGHT_INSTALL_PACKAGES_DECLARED = 8

    const val WEIGHT_USAGE_STATS_GRANTED = 10
    const val WEIGHT_USAGE_STATS_DECLARED = 4

    // Specific Permission Base Weights (when Granted; multiplied by 0.3 if only Declared)
    val PERMISSION_BASE_WEIGHTS = mapOf(
        // SMS
        "android.permission.READ_SMS" to 15,
        "android.permission.RECEIVE_SMS" to 10,
        "android.permission.SEND_SMS" to 12,
        "android.permission.RECEIVE_MMS" to 8,

        // Contacts & Calls
        "android.permission.READ_CONTACTS" to 8,
        "android.permission.WRITE_CONTACTS" to 5,
        "android.permission.READ_CALL_LOG" to 10,
        "android.permission.WRITE_CALL_LOG" to 6,

        // Camera & Mic
        "android.permission.CAMERA" to 7,
        "android.permission.RECORD_AUDIO" to 7,

        // Location
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 8,
        "android.permission.ACCESS_FINE_LOCATION" to 6,
        "android.permission.ACCESS_COARSE_LOCATION" to 3,

        // Phone & Identity
        "android.permission.CALL_PHONE" to 5,
        "android.permission.READ_PHONE_NUMBERS" to 4,
        "android.permission.READ_PHONE_STATE" to 3,

        // Storage & Media
        "android.permission.MANAGE_EXTERNAL_STORAGE" to 12,
        "android.permission.READ_EXTERNAL_STORAGE" to 4,
        "android.permission.WRITE_EXTERNAL_STORAGE" to 4,
        "android.permission.READ_MEDIA_IMAGES" to 4,
        "android.permission.READ_MEDIA_VIDEO" to 4,
        "android.permission.READ_MEDIA_AUDIO" to 4,

        // Background & Persistence
        "android.permission.RECEIVE_BOOT_COMPLETED" to 3,
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to 3,
        "android.permission.FOREGROUND_SERVICE" to 2,

        // Network
        "android.permission.INTERNET" to 1,
        "android.permission.ACCESS_NETWORK_STATE" to 1
    )

    // Combination Rule Bonuses
    const val COMBO_ACCESSIBILITY_OVERLAY = 25
    const val COMBO_SMS_INTERNET_BOOT_SIDELOAD = 25
    const val COMBO_DEVICE_ADMIN_NON_MDM = 20
    const val COMBO_NOTIFICATION_INTERNET = 20
    const val COMBO_INSTALL_PACKAGES_SIDELOAD_INTERNET = 20
    const val COMBO_CAMERA_MIC_BOOT_INTERNET = 15
    const val COMBO_CONTACTS_SMS_INTERNET = 10

    // Modifiers
    const val MODIFIER_SIDELOADED_OR_UNKNOWN = 8
    const val MODIFIER_TARGET_SDK_OLD = 5 // targetSdk < 28

    // Known legitimate MDM / Security packages that shouldn't be penalized for device admin
    val KNOWN_SECURITY_PACKAGES = setOf(
        "com.google.android.apps.work.clouddpc",
        "com.microsoft.windowsintune.companyportal",
        "com.airwatch.androidagent",
        "com.mobileiron",
        "com.sophos.smsec",
        "com.lookout",
        "com.bitdefender.security",
        "com.kaspersky.mobile.antivirus",
        "com.eset.ems2.gp",
        "com.symantec.mobilesecurity"
    )
}

