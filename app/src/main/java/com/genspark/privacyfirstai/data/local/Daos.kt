package com.genspark.privacyfirstai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReceiptMemoryDao {
    @Query("SELECT * FROM receipt_memories ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReceiptMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReceiptMemoryEntity)

    @Query("DELETE FROM receipt_memories")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM receipt_memories")
    suspend fun count(): Int
}

@Dao
interface ThreatScanDao {
    @Query("SELECT * FROM threat_scan_records ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ThreatScanEntity>

    @Query("SELECT * FROM threat_scan_records ORDER BY createdAt DESC")
    suspend fun getAll(): List<ThreatScanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThreatScanEntity)

    @Query("DELETE FROM threat_scan_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM threat_scan_records")
    suspend fun count(): Int

    @Query("SELECT MAX(createdAt) FROM threat_scan_records")
    suspend fun latestTimestamp(): Long?
}

@Dao
interface MediaInsightDao {
    @Query("SELECT * FROM media_insights ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MediaInsightEntity>

    @Query("SELECT * FROM media_insights ORDER BY createdAt DESC")
    suspend fun getAll(): List<MediaInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MediaInsightEntity>)

    @Query("DELETE FROM media_insights")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM media_insights")
    suspend fun count(): Int

    @Query("SELECT MAX(createdAt) FROM media_insights")
    suspend fun latestTimestamp(): Long?
}

@Dao
interface DevicePhotoDao {
    @Query("SELECT * FROM device_photo_snapshots ORDER BY indexedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DevicePhotoEntity>

    @Query("SELECT * FROM device_photo_snapshots ORDER BY indexedAt DESC")
    suspend fun getAll(): List<DevicePhotoEntity>

    @Query("DELETE FROM device_photo_snapshots")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DevicePhotoEntity>)

    @Query("SELECT COUNT(*) FROM device_photo_snapshots")
    suspend fun count(): Int

    @Query("SELECT MAX(indexedAt) FROM device_photo_snapshots")
    suspend fun latestIndexedAt(): Long?
}
