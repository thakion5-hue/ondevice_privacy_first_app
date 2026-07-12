package com.genspark.privacyfirstai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_memories")
data class ReceiptMemoryEntity(
    @PrimaryKey val id: String,
    val sourceLabel: String,
    val merchant: String,
    val amountLabel: String,
    val dateLabel: String,
    val rawText: String,
    val createdAt: Long
)

@Entity(tableName = "threat_scan_records")
data class ThreatScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val sourceLabel: String,
    val scannedTextPreview: String,
    val label: String,
    val severity: String,
    val reason: String,
    val recommendedAction: String,
    val modelSource: String,
    val createdAt: Long
)

@Entity(tableName = "media_insights")
data class MediaInsightEntity(
    @PrimaryKey val id: String,
    val sourceLabel: String,
    val labels: List<String>,
    val summary: String,
    val dimensions: String,
    val receiptLike: Boolean,
    val screenshotLike: Boolean,
    val documentLike: Boolean,
    val createdAt: Long
)

@Entity(tableName = "device_photo_snapshots")
data class DevicePhotoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val album: String,
    val dateLabel: String,
    val contentUri: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val isScreenshot: Boolean,
    val documentLike: Boolean,
    val receiptLike: Boolean,
    val indexedAt: Long
)
