package com.riskauditor.core.model

/**
 * The nine mandatory permission and capability categories defined in Problem Statement PS-01.
 */
enum class PermissionCategory(
    val id: String,
    val displayName: String,
    val description: String,
    val whyItMatters: String,
    val maxCategoryCap: Int
) {
    SPECIAL_ACCESS(
        id = "special_access",
        displayName = "Special access",
        description = "Highest weight system privileges granted through separate settings screens.",
        whyItMatters = "Screen reading, fake login overlays, uninstall blocking, OTP capture from notifications, installing further apps.",
        maxCategoryCap = 60
    ),
    SMS(
        id = "sms",
        displayName = "SMS",
        description = "Permissions to read, receive, and send SMS messages.",
        whyItMatters = "OTP theft, premium SMS fraud, message exfiltration.",
        maxCategoryCap = 25
    ),
    CONTACTS_AND_CALLS(
        id = "contacts_calls",
        displayName = "Contacts and call logs",
        description = "Permissions to access personal address books and phone call history.",
        whyItMatters = "Harvesting the social graph for scams, extortion, and impersonation.",
        maxCategoryCap = 15
    ),
    CAMERA_AND_MIC(
        id = "camera_mic",
        displayName = "Camera and microphone",
        description = "Hardware sensors capable of visual and acoustic recording.",
        whyItMatters = "Covert eavesdropping, surveillance, and blackmail recording.",
        maxCategoryCap = 14
    ),
    LOCATION(
        id = "location",
        displayName = "Location",
        description = "Fine, coarse, and background geographic positioning.",
        whyItMatters = "Physical tracking, routine stalking, and habit profiling.",
        maxCategoryCap = 14
    ),
    PHONE_AND_IDENTITY(
        id = "phone_identity",
        displayName = "Phone and identity",
        description = "Phone state, phone numbers, carrier details, and call initiation.",
        whyItMatters = "Device fingerprinting, SIM identity fraud, rogue calling.",
        maxCategoryCap = 10
    ),
    STORAGE_AND_MEDIA(
        id = "storage_media",
        displayName = "Storage and media",
        description = "Full filesystem access, photos, videos, and audio storage.",
        whyItMatters = "Photo and document theft, ransomware file encryption.",
        maxCategoryCap = 12
    ),
    BACKGROUND_AND_PERSISTENCE(
        id = "background_persistence",
        displayName = "Background and persistence",
        description = "Boot persistence, foreground services, battery optimization exemptions.",
        whyItMatters = "Survives reboots, avoids termination, and keeps running unseen.",
        maxCategoryCap = 8
    ),
    NETWORK(
        id = "network",
        displayName = "Network",
        description = "Internet access and network state monitoring.",
        whyItMatters = "The exfiltration channel; low risk alone, high in combination with sensitive data.",
        maxCategoryCap = 2
    ),
    OTHER(
        id = "other",
        displayName = "Other permissions",
        description = "Standard or miscellaneous platform permissions.",
        whyItMatters = "General platform capabilities with minimal direct risk.",
        maxCategoryCap = 5
    )
}

