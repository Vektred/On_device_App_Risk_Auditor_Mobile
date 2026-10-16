package com.riskauditor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppSnapshotDao {
    @Query("SELECT * FROM app_snapshots WHERE packageName = :packageName")
    suspend fun getSnapshot(packageName: String): AppSnapshotEntity?

    @Query("SELECT * FROM app_snapshots")
    suspend fun getAllSnapshots(): List<AppSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(snapshot: AppSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<AppSnapshotEntity>)

    @Query("DELETE FROM app_snapshots WHERE packageName = :packageName")
    suspend fun deleteSnapshot(packageName: String)
}

