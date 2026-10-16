package com.riskauditor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_snapshots")
data class AppSnapshotEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String?,
    val riskScore: Int,
    val grantedPermissionsCsv: String,
    val declaredPermissionsCsv: String,
    val specialAccessCsv: String,
    val lastScannedAt: Long
)

