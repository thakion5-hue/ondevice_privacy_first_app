package com.genspark.privacyfirstai.data.local

import com.genspark.privacyfirstai.domain.model.DevicePhotoSnapshot
import com.genspark.privacyfirstai.domain.model.ImportedMediaInsight
import com.genspark.privacyfirstai.domain.model.ReceiptMemory
import com.genspark.privacyfirstai.domain.model.ThreatScan
import com.genspark.privacyfirstai.domain.model.ThreatScanRecord

class AppLocalStore(
    private val db: AppDatabase
) {
    suspend fun getReceipts(): List<ReceiptMemory> =
        db.receiptMemoryDao().getAll().map { it.toDomain() }

    suspend fun saveReceipt(receipt: ReceiptMemory) {
        db.receiptMemoryDao().upsert(receipt.toEntity())
    }

    suspend fun clearReceipts() {
        db.receiptMemoryDao().clearAll()
    }

    suspend fun countReceipts(): Int = db.receiptMemoryDao().count()

    suspend fun getThreatScans(limit: Int = 12): List<ThreatScanRecord> =
        db.threatScanDao().getRecent(limit).map { it.toDomain() }

    suspend fun getAllThreatScans(): List<ThreatScanRecord> =
        db.threatScanDao().getAll().map { it.toDomain() }

    suspend fun saveThreatScan(
        source: String,
        sourceLabel: String,
        scannedText: String,
        result: ThreatScan
    ) {
        db.threatScanDao().insert(
            ThreatScanEntity(
                source = source,
                sourceLabel = sourceLabel,
                scannedTextPreview = scannedText.take(240),
                label = result.label,
                severity = result.severity,
                reason = result.reason,
                recommendedAction = result.recommendedAction,
                modelSource = result.modelSource,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearThreatScans() {
        db.threatScanDao().clearAll()
    }

    suspend fun countThreatScans(): Int = db.threatScanDao().count()

    suspend fun latestThreatScanAt(): Long? = db.threatScanDao().latestTimestamp()

    suspend fun getMediaInsights(limit: Int = 20): List<ImportedMediaInsight> =
        db.mediaInsightDao().getRecent(limit).map { it.toDomain() }

    suspend fun getAllMediaInsights(): List<ImportedMediaInsight> =
        db.mediaInsightDao().getAll().map { it.toDomain() }

    suspend fun saveMediaInsights(insights: List<ImportedMediaInsight>) {
        val now = System.currentTimeMillis()
        db.mediaInsightDao().upsertAll(insights.map { it.toEntity(now) })
    }

    suspend fun clearMediaInsights() {
        db.mediaInsightDao().clearAll()
    }

    suspend fun countMediaInsights(): Int = db.mediaInsightDao().count()

    suspend fun latestMediaInsightAt(): Long? = db.mediaInsightDao().latestTimestamp()

    suspend fun getDevicePhotos(limit: Int = 80): List<DevicePhotoSnapshot> =
        db.devicePhotoDao().getRecent(limit).map { it.toDomain() }

    suspend fun getAllDevicePhotos(): List<DevicePhotoSnapshot> =
        db.devicePhotoDao().getAll().map { it.toDomain() }

    suspend fun replaceDevicePhotos(photos: List<DevicePhotoSnapshot>) {
        db.devicePhotoDao().clearAll()
        db.devicePhotoDao().upsertAll(photos.map { it.toEntity(System.currentTimeMillis()) })
    }

    suspend fun clearDevicePhotos() {
        db.devicePhotoDao().clearAll()
    }

    suspend fun countDevicePhotos(): Int = db.devicePhotoDao().count()

    suspend fun latestDeviceIndexAt(): Long? = db.devicePhotoDao().latestIndexedAt()

    suspend fun clearAll() {
        db.receiptMemoryDao().clearAll()
        db.threatScanDao().clearAll()
        db.mediaInsightDao().clearAll()
        db.devicePhotoDao().clearAll()
    }
}

private fun ReceiptMemoryEntity.toDomain() = ReceiptMemory(
    id = id,
    sourceLabel = sourceLabel,
    merchant = merchant,
    amountLabel = amountLabel,
    dateLabel = dateLabel,
    rawText = rawText
)

private fun ReceiptMemory.toEntity() = ReceiptMemoryEntity(
    id = id,
    sourceLabel = sourceLabel,
    merchant = merchant,
    amountLabel = amountLabel,
    dateLabel = dateLabel,
    rawText = rawText,
    createdAt = System.currentTimeMillis()
)

private fun ThreatScanEntity.toDomain() = ThreatScanRecord(
    id = id,
    source = source,
    sourceLabel = sourceLabel,
    scannedTextPreview = scannedTextPreview,
    label = label,
    severity = severity,
    reason = reason,
    recommendedAction = recommendedAction,
    modelSource = modelSource,
    createdAt = createdAt
)

private fun ImportedMediaInsight.toEntity(createdAt: Long) = MediaInsightEntity(
    id = id,
    sourceLabel = sourceLabel,
    labels = labels,
    summary = summary,
    dimensions = dimensions,
    receiptLike = receiptLike,
    screenshotLike = screenshotLike,
    documentLike = documentLike,
    createdAt = createdAt
)

private fun MediaInsightEntity.toDomain() = ImportedMediaInsight(
    id = id,
    sourceLabel = sourceLabel,
    labels = labels,
    summary = summary,
    dimensions = dimensions,
    receiptLike = receiptLike,
    screenshotLike = screenshotLike,
    documentLike = documentLike
)

private fun DevicePhotoSnapshot.toEntity(indexedAt: Long) = DevicePhotoEntity(
    id = id,
    title = title,
    album = album,
    dateLabel = dateLabel,
    contentUri = contentUri,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    isScreenshot = isScreenshot,
    documentLike = documentLike,
    receiptLike = receiptLike,
    indexedAt = indexedAt
)

private fun DevicePhotoEntity.toDomain() = DevicePhotoSnapshot(
    id = id,
    title = title,
    album = album,
    dateLabel = dateLabel,
    contentUri = contentUri,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    isScreenshot = isScreenshot,
    documentLike = documentLike,
    receiptLike = receiptLike
)
