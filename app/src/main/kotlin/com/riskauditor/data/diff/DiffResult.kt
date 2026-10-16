package com.riskauditor.data.diff

import com.riskauditor.core.model.PermissionCategory

data class DiffResult(
    val packageName: String,
    val appName: String,
    val oldVersionCode: Long,
    val newVersionCode: Long,
    val oldScore: Int,
    val newScore: Int,
    val newlyAddedPermissions: List<String>,
    val newlyGrantedPermissions: List<String>,
    val newlyEnabledSpecialAccess: List<String>,
    val sensitiveCategoriesAffected: Set<PermissionCategory>,
    val isDangerousDiff: Boolean
)

