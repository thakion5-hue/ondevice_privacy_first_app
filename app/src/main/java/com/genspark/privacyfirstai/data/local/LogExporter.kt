package com.genspark.privacyfirstai.data.local

import android.content.Context
import com.genspark.privacyfirstai.ai.OnDeviceRuntimeRegistry
import com.genspark.privacyfirstai.ai.gemininano.trace.GeminiNanoInferenceTraceRecorder
import com.genspark.privacyfirstai.ai.gemininano.trace.geminiNanoInferenceTraceSummary
import com.genspark.privacyfirstai.domain.model.DevicePhotoSnapshot
import com.genspark.privacyfirstai.domain.model.ImportedMediaInsight
import com.genspark.privacyfirstai.domain.model.ReceiptMemory
import com.genspark.privacyfirstai.domain.model.ThreatScanRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room 에 쌓인 온디바이스 히스토리를 JSON 스냅샷으로 내보낸다.
 * v6 목표:
 *   - 실제 앱에서 QA / 디버그용 로그를 뽑을 수 있게 한다
 *   - 저장 위치는 앱 내부 저장소 (`filesDir/exports/`) 로 고정,
 *     외부 저장 권한 없이 파일이 만들어진다
 *   - FileProvider 로 다른 앱 공유가 가능하도록 export 형식을 고정한다
 *   - 어떤 네트워크로도 전송하지 않는다 (privacy-first 유지)
 */
class LogExporter(
    private val context: Context,
    private val localStore: AppLocalStore,
    private val preferencesStore: AppPreferencesStore,
    private val modelManifestStore: ModelManifestStore,
    private val runtimeRegistry: OnDeviceRuntimeRegistry
) {

    data class ExportResult(
        val filePath: String,
        val fileName: String,
        val sizeBytes: Long,
        val threatScanCount: Int,
        val mediaInsightCount: Int,
        val devicePhotoCount: Int,
        val receiptCount: Int
    )

    suspend fun exportAll(): ExportResult {
        val exportDir = File(context.filesDir, EXPORT_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

        val threatScans = localStore.getAllThreatScans()
        val mediaInsights = localStore.getAllMediaInsights()
        val devicePhotos = localStore.getAllDevicePhotos()
        val receipts = localStore.getReceipts()
        val settings = preferencesStore.getSettings()
        val manifest = modelManifestStore.load()
        val runtimeAvailability = runtimeRegistry.getAvailability()
        val geminiConnectorDiagnostic = runtimeRegistry.geminiNanoConnectorDiagnostics()
        val geminiConnectorContract = runtimeRegistry.geminiNanoConnectorContract()
        val traceSnapshot = GeminiNanoInferenceTraceRecorder.snapshot()
        val traceSummary = geminiNanoInferenceTraceSummary()

        val root = JSONObject().apply {
            put("schema_version", SCHEMA_VERSION)
            put("exported_at", System.currentTimeMillis())
            put("exported_at_iso", ISO_FORMAT.format(Date()))
            put("app_profile", manifest.appProfile)
            put("settings", JSONObject().apply {
                put("spam_filter_mode", settings.spamFilterMode.storageKey)
                put("preferred_runtime", settings.preferredRuntime.storageKey)
                put("gemini_nano_connector_mode", settings.geminiNanoConnectorMode.storageKey)
                put(
                    "auto_index_after_permission_grant",
                    settings.autoIndexAfterPermissionGrant
                )
            })
            put("gemini_nano_connector", JSONObject().apply {
                put("mode", geminiConnectorDiagnostic.connectorMode.storageKey)
                put("mode_label", geminiConnectorDiagnostic.connectorMode.label)
                put("available", geminiConnectorDiagnostic.available)
                put("status", geminiConnectorDiagnostic.statusLabel)
                put("binding_phase", geminiConnectorDiagnostic.bindingPhase)
                put("contract_summary", geminiConnectorDiagnostic.contractSummary)
                put("detail", geminiConnectorDiagnostic.detail)
                put("provider", JSONObject().apply {
                    put("status", geminiConnectorDiagnostic.providerStatusLabel)
                    put("summary", geminiConnectorDiagnostic.providerSummary)
                    put("layer", geminiConnectorContract.providerLayer)
                })
                put("session_factory", JSONObject().apply {
                    put("status", geminiConnectorDiagnostic.sessionFactoryStatusLabel)
                    put("summary", geminiConnectorDiagnostic.sessionFactorySummary)
                    put("layer", geminiConnectorContract.sessionFactory)
                    put("inference_entry_point", geminiConnectorContract.inferenceEntryPoint)
                    put("output_expectation", geminiConnectorContract.outputExpectation)
                    put("fallback_policy", geminiConnectorContract.fallbackPolicy)
                })
                put("todo_checklist", JSONArray(geminiConnectorContract.todoChecklist))
                put("inference_trace", JSONObject().apply {
                    put("schema", "gemini_nano_inference_trace_v1")
                    put("buffered", traceSummary.bufferedTraces)
                    put("capacity", traceSummary.bufferCapacity)
                    put("last_label", traceSummary.lastTraceLabel)
                    put("last_latency_ms", traceSummary.lastTraceLatencyMillis)
                    put("records", JSONArray().apply {
                        traceSnapshot.forEach { t ->
                            put(JSONObject().apply {
                                put("trace_id", t.traceId)
                                put("request_id", t.requestId)
                                put("session_label", t.sessionLabel)
                                put("connector_mode", t.connectorMode)
                                put("provider_label", t.providerLabel)
                                put("factory_label", t.factoryLabel)
                                put("input_preview", t.inputPreview)
                                put("input_char_length", t.inputCharLength)
                                put("input_token_estimate", t.inputTokenEstimate)
                                put("hit_signals", JSONArray(t.hitSignals))
                                put("link_detected", t.linkDetected)
                                put("decision_rule", t.decisionRule)
                                put("output_label", t.outputLabel)
                                put("output_score", t.outputScore.toDouble())
                                put("output_logits", JSONArray(t.outputLogits.map { it.toDouble() }))
                                put("output_confidence_band", t.outputConfidenceBand)
                                put("started_at", t.startedAtMillis)
                                put("completed_at", t.completedAtMillis)
                                put("latency_ms", t.latencyMillis)
                                if (t.fallbackReason != null) put("fallback_reason", t.fallbackReason)
                            })
                        }
                    })
                })
            })
            put("runtime_diagnostics", JSONArray().apply {
                runtimeAvailability.forEach {
                    put(JSONObject().apply {
                        put("id", it.runtime.storageKey)
                        put("label", it.runtime.label)
                        put("available", it.available)
                        put("status", it.statusLabel)
                        put("detail", it.detail)
                        put("badge_tone", it.badgeTone.name.lowercase())
                    })
                }
            })
            put("runtimes", JSONArray().apply {
                manifest.runtimes.forEach {
                    put(JSONObject().apply {
                        put("id", it.id)
                        put("type", it.type)
                        put("status", it.status)
                        put("purpose", it.purpose)
                        put("notes", it.notes)
                    })
                }
            })
            put("threat_scans", threatScans.toJsonArray())
            put("media_insights", mediaInsights.toMediaJsonArray())
            put("device_photos", devicePhotos.toDeviceJsonArray())
            put("receipt_memories", receipts.toReceiptJsonArray())
        }

        val timestamp = FILE_TIMESTAMP_FORMAT.format(Date())
        val fileName = "privacy_first_ai_log_${timestamp}.json"
        val outFile = File(exportDir, fileName)
        outFile.writeText(root.toString(2))

        return ExportResult(
            filePath = outFile.absolutePath,
            fileName = outFile.name,
            sizeBytes = outFile.length(),
            threatScanCount = threatScans.size,
            mediaInsightCount = mediaInsights.size,
            devicePhotoCount = devicePhotos.size,
            receiptCount = receipts.size
        )
    }

    private fun List<ThreatScanRecord>.toJsonArray(): JSONArray = JSONArray().also { arr ->
        forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("source", it.source)
                put("source_label", it.sourceLabel)
                put("scanned_text_preview", it.scannedTextPreview)
                put("label", it.label)
                put("severity", it.severity)
                put("reason", it.reason)
                put("recommended_action", it.recommendedAction)
                put("model_source", it.modelSource)
                put("created_at", it.createdAt)
                put("created_at_iso", ISO_FORMAT.format(Date(it.createdAt)))
            })
        }
    }

    private fun List<ImportedMediaInsight>.toMediaJsonArray(): JSONArray = JSONArray().also { arr ->
        forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("source_label", it.sourceLabel)
                put("labels", JSONArray(it.labels))
                put("summary", it.summary)
                put("dimensions", it.dimensions)
                put("receipt_like", it.receiptLike)
                put("screenshot_like", it.screenshotLike)
                put("document_like", it.documentLike)
            })
        }
    }

    private fun List<DevicePhotoSnapshot>.toDeviceJsonArray(): JSONArray = JSONArray().also { arr ->
        forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("album", it.album)
                put("date_label", it.dateLabel)
                put("content_uri", it.contentUri)
                put("width", it.width)
                put("height", it.height)
                put("size_bytes", it.sizeBytes)
                put("is_screenshot", it.isScreenshot)
                put("document_like", it.documentLike)
                put("receipt_like", it.receiptLike)
            })
        }
    }

    private fun List<ReceiptMemory>.toReceiptJsonArray(): JSONArray = JSONArray().also { arr ->
        forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("source_label", it.sourceLabel)
                put("merchant", it.merchant)
                put("amount_label", it.amountLabel)
                put("date_label", it.dateLabel)
                put("raw_text", it.rawText)
            })
        }
    }

    fun listExports(): List<File> {
        val dir = File(context.filesDir, EXPORT_DIR_NAME)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun clearExports(): Int {
        val dir = File(context.filesDir, EXPORT_DIR_NAME)
        if (!dir.exists()) return 0
        var count = 0
        dir.listFiles()?.forEach {
            if (it.isFile && it.delete()) count++
        }
        return count
    }

    companion object {
        const val SCHEMA_VERSION = 6
        private const val EXPORT_DIR_NAME = "exports"
        private val ISO_FORMAT =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        private val FILE_TIMESTAMP_FORMAT =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}
