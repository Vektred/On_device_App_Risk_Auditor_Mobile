package com.riskauditor.data.diff

import com.riskauditor.core.categorizer.PermissionCategorizer
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.PermissionCategory
import com.riskauditor.core.model.RiskResult
import com.riskauditor.data.db.AppSnapshotDao
import com.riskauditor.data.db.AppSnapshotEntity

class PermissionDiffEngine(private val snapshotDao: AppSnapshotDao) {

    suspend fun computeDiffAndSave(app: AppFacts, currentResult: RiskResult): DiffResult? {
        val previous = snapshotDao.getSnapshot(app.packageName)

        // Capture current state strings
        val currentGranted = app.permissions.filter { it.granted }.map { it.name }.toSet()
        val currentDeclared = app.permissions.map { it.name }.toSet()
        val currentSpecialList = mutableListOf<String>()
        val sa = app.specialAccess
        if (sa.accessibilityEnabled) currentSpecialList.add("Accessibility Service")
        if (sa.deviceAdminActive) currentSpecialList.add("Device Administrator")
        if (sa.notificationListenerEnabled) currentSpecialList.add("Notification Listener")
        if (sa.overlayGranted) currentSpecialList.add("Display Over Other Apps")
        if (sa.installPackagesGranted) currentSpecialList.add("Install Unknown Apps")
        if (sa.usageStatsGranted) currentSpecialList.add("App Usage Access")

        // Save new snapshot to DB
        val newEntity = AppSnapshotEntity(
            packageName = app.packageName,
            appName = app.appName,
            versionCode = app.versionCode,
            versionName = app.versionName,
            riskScore = currentResult.score,
            grantedPermissionsCsv = currentGranted.joinToString(","),
            declaredPermissionsCsv = currentDeclared.joinToString(","),
            specialAccessCsv = currentSpecialList.joinToString(","),
            lastScannedAt = System.currentTimeMillis()
        )
        snapshotDao.insertOrUpdate(newEntity)

        // If this was the first ever scan, this is just the baseline: return null
        if (previous == null) {
            return null
        }

        // Compare against previous snapshot
        val oldDeclared = if (previous.declaredPermissionsCsv.isBlank()) emptySet() else previous.declaredPermissionsCsv.split(",").toSet()
        val oldGranted = if (previous.grantedPermissionsCsv.isBlank()) emptySet() else previous.grantedPermissionsCsv.split(",").toSet()
        val oldSpecial = if (previous.specialAccessCsv.isBlank()) emptySet() else previous.specialAccessCsv.split(",").toSet()

        val newlyAdded = (currentDeclared - oldDeclared).toList()
        val newlyGranted = (currentGranted - oldGranted).toList()
        val newlySpecial = (currentSpecialList.toSet() - oldSpecial).toList()

        // Check if sensitive categories are affected
        val affectedCategories = mutableSetOf<PermissionCategory>()
        for (perm in newlyAdded + newlyGranted) {
            val cat = PermissionCategorizer.categorize(perm)
            if (cat != PermissionCategory.OTHER && cat != PermissionCategory.NETWORK) {
                affectedCategories.add(cat)
            }
        }
        if (newlySpecial.isNotEmpty()) {
            affectedCategories.add(PermissionCategory.SPECIAL_ACCESS)
        }

        val isDangerous = affectedCategories.isNotEmpty() ||
                (currentResult.score - previous.riskScore) >= 15 ||
                currentResult.score >= 50

        // Only return diff if there were actual changes
        if (newlyAdded.isEmpty() && newlyGranted.isEmpty() && newlySpecial.isEmpty() && currentResult.score == previous.riskScore) {
            return null
        }

        return DiffResult(
            packageName = app.packageName,
            appName = app.appName,
            oldVersionCode = previous.versionCode,
            newVersionCode = app.versionCode,
            oldScore = previous.riskScore,
            newScore = currentResult.score,
            newlyAddedPermissions = newlyAdded,
            newlyGrantedPermissions = newlyGranted,
            newlyEnabledSpecialAccess = newlySpecial,
            sensitiveCategoriesAffected = affectedCategories,
            isDangerousDiff = isDangerous
        )
    }
}

