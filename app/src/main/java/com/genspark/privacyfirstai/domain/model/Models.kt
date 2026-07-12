package com.genspark.privacyfirstai.domain.model

data class PhotoAsset(
    val id: String,
    val title: String,
    val city: String,
    val dateLabel: String,
    val tags: List<String>,
    val isReceipt: Boolean,
    val isScreenshot: Boolean,
    val duplicateGroup: String? = null,
    val qualityScore: Int = 90
)

data class ConversationSnippet(
    val id: String,
    val contactName: String,
    val content: String,
    val timeLabel: String,
    val mood: String
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val dateLabel: String,
    val location: String,
    val notes: String
)

data class AssistantAnswer(
    val title: String,
    val body: String,
    val references: List<String>
)

data class JournalDraft(
    val headline: String,
    val body: String,
    val hashtags: List<String>
)

data class ThreatScan(
    val label: String,
    val severity: String,
    val reason: String,
    val recommendedAction: String,
    val modelSource: String = "heuristic"
)

data class ThreatScanRecord(
    val id: Long = 0,
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

data class ReceiptMemory(
    val id: String,
    val sourceLabel: String,
    val merchant: String,
    val amountLabel: String,
    val dateLabel: String,
    val rawText: String
)

data class ImportedMediaInsight(
    val id: String,
    val sourceLabel: String,
    val labels: List<String>,
    val summary: String,
    val dimensions: String,
    val receiptLike: Boolean,
    val screenshotLike: Boolean,
    val documentLike: Boolean
)

data class DevicePhotoSnapshot(
    val id: String,
    val title: String,
    val album: String,
    val dateLabel: String,
    val contentUri: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val isScreenshot: Boolean,
    val documentLike: Boolean,
    val receiptLike: Boolean
)

data class SpamModelResult(
    val label: String,
    val score: Float,
    val logits: List<Float>
)
