package com.riskauditor.core

import com.riskauditor.core.categorizer.PermissionCategorizer
import com.riskauditor.core.model.PermissionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionCategorizerTest {

    @Test
    fun testAllNineCategoriesMapping() {
        // 01 Special access
        assertEquals(
            PermissionCategory.SPECIAL_ACCESS,
            PermissionCategorizer.categorize("android.permission.SYSTEM_ALERT_WINDOW")
        )
        assertEquals(
            PermissionCategory.SPECIAL_ACCESS,
            PermissionCategorizer.categorize("android.permission.REQUEST_INSTALL_PACKAGES")
        )
        assertEquals(
            PermissionCategory.SPECIAL_ACCESS,
            PermissionCategorizer.categorize("android.permission.BIND_ACCESSIBILITY_SERVICE")
        )

        // 02 SMS
        assertEquals(
            PermissionCategory.SMS,
            PermissionCategorizer.categorize("android.permission.READ_SMS")
        )
        assertEquals(
            PermissionCategory.SMS,
            PermissionCategorizer.categorize("android.permission.RECEIVE_SMS")
        )
        assertEquals(
            PermissionCategory.SMS,
            PermissionCategorizer.categorize("android.permission.SEND_SMS")
        )

        // 03 Contacts and call logs
        assertEquals(
            PermissionCategory.CONTACTS_AND_CALLS,
            PermissionCategorizer.categorize("android.permission.READ_CONTACTS")
        )
        assertEquals(
            PermissionCategory.CONTACTS_AND_CALLS,
            PermissionCategorizer.categorize("android.permission.READ_CALL_LOG")
        )

        // 04 Camera and mic
        assertEquals(
            PermissionCategory.CAMERA_AND_MIC,
            PermissionCategorizer.categorize("android.permission.CAMERA")
        )
        assertEquals(
            PermissionCategory.CAMERA_AND_MIC,
            PermissionCategorizer.categorize("android.permission.RECORD_AUDIO")
        )

        // 05 Location
        assertEquals(
            PermissionCategory.LOCATION,
            PermissionCategorizer.categorize("android.permission.ACCESS_FINE_LOCATION")
        )
        assertEquals(
            PermissionCategory.LOCATION,
            PermissionCategorizer.categorize("android.permission.ACCESS_BACKGROUND_LOCATION")
        )

        // 06 Phone and identity
        assertEquals(
            PermissionCategory.PHONE_AND_IDENTITY,
            PermissionCategorizer.categorize("android.permission.READ_PHONE_STATE")
        )
        assertEquals(
            PermissionCategory.PHONE_AND_IDENTITY,
            PermissionCategorizer.categorize("android.permission.CALL_PHONE")
        )

        // 07 Storage and media
        assertEquals(
            PermissionCategory.STORAGE_AND_MEDIA,
            PermissionCategorizer.categorize("android.permission.READ_MEDIA_IMAGES")
        )
        assertEquals(
            PermissionCategory.STORAGE_AND_MEDIA,
            PermissionCategorizer.categorize("android.permission.MANAGE_EXTERNAL_STORAGE")
        )

        // 08 Background and persistence
        assertEquals(
            PermissionCategory.BACKGROUND_AND_PERSISTENCE,
            PermissionCategorizer.categorize("android.permission.RECEIVE_BOOT_COMPLETED")
        )
        assertEquals(
            PermissionCategory.BACKGROUND_AND_PERSISTENCE,
            PermissionCategorizer.categorize("android.permission.FOREGROUND_SERVICE")
        )

        // 09 Network
        assertEquals(
            PermissionCategory.NETWORK,
            PermissionCategorizer.categorize("android.permission.INTERNET")
        )
        assertEquals(
            PermissionCategory.NETWORK,
            PermissionCategorizer.categorize("android.permission.ACCESS_NETWORK_STATE")
        )
    }

    @Test
    fun testCustomAndUnknownPermissionsFallback() {
        assertEquals(
            PermissionCategory.SMS,
            PermissionCategorizer.categorize("com.example.custom.READ_SMS_SECRET")
        )
        assertEquals(
            PermissionCategory.OTHER,
            PermissionCategorizer.categorize("com.vendor.custom.VIBRATOR_SPECIAL")
        )
    }
}

