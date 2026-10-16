package com.riskauditor.core.analyzer.rules

import com.riskauditor.core.analyzer.Rule
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.Finding
import com.riskauditor.core.model.PermissionCategory
import com.riskauditor.core.model.Severity

/**
 * Flags apps whose declared functionality (flashlight, calculator, wallpaper, notes, QR scanner)
 * does not match the sensitive capabilities it holds (e.g. SMS, Call Logs, Contacts, Accessibility).
 */
object ContextMismatchRule : Rule {
    override val ruleId = "CONTEXT_MISMATCH"

    enum class Archetype(
        val keywords: List<String>,
        val allowedCategories: Set<PermissionCategory>
    ) {
        FLASHLIGHT(
            keywords = listOf("torch", "flashlight", "flash light"),
            allowedCategories = setOf(PermissionCategory.CAMERA_AND_MIC, PermissionCategory.OTHER)
        ),
        CALCULATOR(
            keywords = listOf("calculator", "calc", "unit converter"),
            allowedCategories = setOf(PermissionCategory.OTHER)
        ),
        WALLPAPER(
            keywords = listOf("wallpaper", "backgrounds", "lockscreen theme"),
            allowedCategories = setOf(PermissionCategory.STORAGE_AND_MEDIA, PermissionCategory.NETWORK, PermissionCategory.OTHER)
        ),
        QR_SCANNER(
            keywords = listOf("qr", "barcode", "scanner", "qr code"),
            allowedCategories = setOf(PermissionCategory.CAMERA_AND_MIC, PermissionCategory.OTHER)
        ),
        SIMPLE_NOTES(
            keywords = listOf("notepad", "simple notes", "sticky note", "memo pad"),
            allowedCategories = setOf(PermissionCategory.STORAGE_AND_MEDIA, PermissionCategory.OTHER)
        )
    }

    private val sensitiveCategories = setOf(
        PermissionCategory.SMS,
        PermissionCategory.CONTACTS_AND_CALLS,
        PermissionCategory.SPECIAL_ACCESS,
        PermissionCategory.PHONE_AND_IDENTITY
    )

    override fun evaluate(app: AppFacts): Finding? {
        val labelAndPkg = "${app.appName.lowercase()} ${app.packageName.lowercase()}"

        for (archetype in Archetype.values()) {
            val matchesArchetype = archetype.keywords.any { keyword -> labelAndPkg.contains(keyword) }
            if (matchesArchetype) {
                // Find sensitive categories requested or held by this app that are not in its allowed profile
                val heldCategories = mutableSetOf<PermissionCategory>()
                for (perm in app.permissions) {
                    if (perm.category in sensitiveCategories && !archetype.allowedCategories.contains(perm.category)) {
                        heldCategories.add(perm.category)
                    }
                }
                if (app.specialAccess.accessibilityEnabled || app.specialAccess.accessibilityDeclared ||
                    app.specialAccess.deviceAdminActive || app.specialAccess.overlayGranted ||
                    app.specialAccess.notificationListenerEnabled) {
                    if (!archetype.allowedCategories.contains(PermissionCategory.SPECIAL_ACCESS)) {
                        heldCategories.add(PermissionCategory.SPECIAL_ACCESS)
                    }
                }

                if (heldCategories.isNotEmpty()) {
                    val count = heldCategories.size
                    val points = 20 + ((count - 1) * 10)
                    val categoriesList = heldCategories.joinToString(", ") { it.displayName }

                    return Finding(
                        ruleId = ruleId,
                        points = points,
                        severity = Severity.HIGH,
                        title = "Context Mismatch (${archetype.name.replace('_', ' ')})",
                        plainExplanation = "This app appears to be a ${archetype.name.lowercase().replace('_', ' ')}, but requests sensitive access ($categoriesList) that does not fit what the app claims to be."
                    )
                }
            }
        }
        return null
    }
}

