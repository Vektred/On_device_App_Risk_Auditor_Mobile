package com.riskauditor.core.categorizer

import com.riskauditor.core.model.PermissionCategory

/**
 * Pure Kotlin categorizer mapping Android permission strings to the 9 problem statement categories.
 */
object PermissionCategorizer {

    private val categoryMap: Map<String, PermissionCategory> = mapOf(
        // 01 Special access
        "android.permission.SYSTEM_ALERT_WINDOW" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.PACKAGE_USAGE_STATS" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.BIND_DEVICE_ADMIN" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to PermissionCategory.SPECIAL_ACCESS,
        "android.permission.BIND_VPN_SERVICE" to PermissionCategory.SPECIAL_ACCESS,

        // 02 SMS
        "android.permission.READ_SMS" to PermissionCategory.SMS,
        "android.permission.RECEIVE_SMS" to PermissionCategory.SMS,
        "android.permission.SEND_SMS" to PermissionCategory.SMS,
        "android.permission.RECEIVE_MMS" to PermissionCategory.SMS,
        "android.permission.RECEIVE_WAP_PUSH" to PermissionCategory.SMS,
        "android.permission.READ_CELL_BROADCASTS" to PermissionCategory.SMS,

        // 03 Contacts and call logs
        "android.permission.READ_CONTACTS" to PermissionCategory.CONTACTS_AND_CALLS,
        "android.permission.WRITE_CONTACTS" to PermissionCategory.CONTACTS_AND_CALLS,
        "android.permission.GET_ACCOUNTS" to PermissionCategory.CONTACTS_AND_CALLS,
        "android.permission.READ_CALL_LOG" to PermissionCategory.CONTACTS_AND_CALLS,
        "android.permission.WRITE_CALL_LOG" to PermissionCategory.CONTACTS_AND_CALLS,
        "android.permission.PROCESS_OUTGOING_CALLS" to PermissionCategory.CONTACTS_AND_CALLS,

        // 04 Camera and microphone
        "android.permission.CAMERA" to PermissionCategory.CAMERA_AND_MIC,
        "android.permission.RECORD_AUDIO" to PermissionCategory.CAMERA_AND_MIC,
        "android.permission.CAPTURE_AUDIO_OUTPUT" to PermissionCategory.CAMERA_AND_MIC,

        // 05 Location
        "android.permission.ACCESS_FINE_LOCATION" to PermissionCategory.LOCATION,
        "android.permission.ACCESS_COARSE_LOCATION" to PermissionCategory.LOCATION,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionCategory.LOCATION,
        "android.permission.ACCESS_MEDIA_LOCATION" to PermissionCategory.LOCATION,

        // 06 Phone and identity
        "android.permission.READ_PHONE_STATE" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.READ_PHONE_NUMBERS" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.CALL_PHONE" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.ANSWER_PHONE_CALLS" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.READ_PRECISE_PHONE_STATE" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.MODIFY_PHONE_STATE" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.USE_SIP" to PermissionCategory.PHONE_AND_IDENTITY,
        "android.permission.ADD_VOICEMAIL" to PermissionCategory.PHONE_AND_IDENTITY,

        // 07 Storage and media
        "android.permission.READ_EXTERNAL_STORAGE" to PermissionCategory.STORAGE_AND_MEDIA,
        "android.permission.WRITE_EXTERNAL_STORAGE" to PermissionCategory.STORAGE_AND_MEDIA,
        "android.permission.READ_MEDIA_IMAGES" to PermissionCategory.STORAGE_AND_MEDIA,
        "android.permission.READ_MEDIA_VIDEO" to PermissionCategory.STORAGE_AND_MEDIA,
        "android.permission.READ_MEDIA_AUDIO" to PermissionCategory.STORAGE_AND_MEDIA,
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to PermissionCategory.STORAGE_AND_MEDIA,

        // 08 Background and persistence
        "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionCategory.BACKGROUND_AND_PERSISTENCE,
        "android.permission.FOREGROUND_SERVICE" to PermissionCategory.BACKGROUND_AND_PERSISTENCE,
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to PermissionCategory.BACKGROUND_AND_PERSISTENCE,
        "android.permission.SCHEDULE_EXACT_ALARM" to PermissionCategory.BACKGROUND_AND_PERSISTENCE,
        "android.permission.USE_EXACT_ALARM" to PermissionCategory.BACKGROUND_AND_PERSISTENCE,
        "android.permission.WAKE_LOCK" to PermissionCategory.BACKGROUND_AND_PERSISTENCE,

        // 09 Network
        "android.permission.INTERNET" to PermissionCategory.NETWORK,
        "android.permission.ACCESS_NETWORK_STATE" to PermissionCategory.NETWORK,
        "android.permission.ACCESS_WIFI_STATE" to PermissionCategory.NETWORK,
        "android.permission.CHANGE_WIFI_STATE" to PermissionCategory.NETWORK,
        "android.permission.CHANGE_NETWORK_STATE" to PermissionCategory.NETWORK
    )

    fun categorize(permissionName: String): PermissionCategory {
        // Direct match
        categoryMap[permissionName]?.let { return it }

        // Heuristic fallback based on name pattern
        val upper = permissionName.uppercase()
        return when {
            upper.contains("SMS") || upper.contains("MMS") -> PermissionCategory.SMS
            upper.contains("CONTACT") || upper.contains("CALL_LOG") -> PermissionCategory.CONTACTS_AND_CALLS
            upper.contains("CAMERA") || upper.contains("AUDIO") || upper.contains("MICROPHONE") -> PermissionCategory.CAMERA_AND_MIC
            upper.contains("LOCATION") -> PermissionCategory.LOCATION
            upper.contains("PHONE") || upper.contains("CALL") -> PermissionCategory.PHONE_AND_IDENTITY
            upper.contains("STORAGE") || upper.contains("MEDIA") -> PermissionCategory.STORAGE_AND_MEDIA
            upper.contains("BOOT") || upper.contains("FOREGROUND_SERVICE") || upper.contains("WAKE_LOCK") -> PermissionCategory.BACKGROUND_AND_PERSISTENCE
            upper.contains("INTERNET") || upper.contains("NETWORK") || upper.contains("WIFI") -> PermissionCategory.NETWORK
            upper.contains("ACCESSIBILITY") || upper.contains("OVERLAY") || upper.contains("ADMIN") -> PermissionCategory.SPECIAL_ACCESS
            else -> PermissionCategory.OTHER
        }
    }
}

