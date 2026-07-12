package com.genspark.privacyfirstai.ai.gemininano

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * ML Kit Prompt API 기반 Gemini Nano 다운로드/재시도 UX를 위한 control-plane.
 *
 * 목표:
 *  - checkStatus / download callback 을 한 곳에 모아 UI/diagnostic 에 동일한 상태를 제공
 *  - DOWNLOADABLE -> download(), DOWNLOADING -> progress polling, AVAILABLE -> inference ready
 *  - 실패 시 재시도 가능한 상태를 남겨 settings / debug 화면에서 바로 노출
 */
data class AiCorePromptDownloadUiState(
    val featureStatus: Int,
    val statusKey: String,
    val availableForInference: Boolean,
    val statusLabel: String,
    val summary: String,
    val bytesToDownload: Long?,
    val downloadedBytes: Long?,
    val progressPercent: Int?,
    val downloadInFlight: Boolean,
    val canDownload: Boolean,
    val canRetry: Boolean,
    val lastError: String?,
    val lastUpdatedAtMillis: Long
) {
    val progressLabel: String?
        get() {
            val downloaded = downloadedBytes ?: return null
            val total = bytesToDownload
            return if (total != null && total > 0L) {
                "${formatByteCount(downloaded)} / ${formatByteCount(total)} (${progressPercent ?: 0}%)"
            } else {
                "${formatByteCount(downloaded)} 다운로드됨"
            }
        }

    val actionLabel: String?
        get() = when {
            canRetry -> "다운로드 재시도"
            canDownload -> "모델 다운로드"
            else -> null
        }
}

class AiCorePromptDownloadController {

    private val client by lazy { GenerativeModelFutures.from(Generation.getClient()) }

    fun snapshot(): AiCorePromptDownloadUiState = AiCorePromptDownloadStateStore.snapshot()

    fun refreshStatus(): AiCorePromptDownloadUiState {
        val status = runCatching {
            client.checkStatus().get(STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.getOrElse { error ->
            return AiCorePromptDownloadStateStore.markProbeError(error.toReadableMessage())
        }
        return AiCorePromptDownloadStateStore.mergeFeatureStatus(status)
    }

    fun requestDownload(): AiCorePromptDownloadUiState {
        val current = refreshStatus()
        return when (current.featureStatus) {
            FeatureStatus.AVAILABLE -> current
            FeatureStatus.DOWNLOADING -> AiCorePromptDownloadStateStore.markDownloadingHint(
                summary = current.progressLabel?.let {
                    "Gemini Nano 다운로드가 이미 진행 중입니다. $it"
                } ?: "Gemini Nano 다운로드가 이미 진행 중입니다."
            )
            FeatureStatus.DOWNLOADABLE -> startDownload()
            else -> current
        }
    }

    private fun startDownload(): AiCorePromptDownloadUiState = synchronized(lock) {
        val existing = AiCorePromptDownloadStateStore.snapshot()
        if (existing.downloadInFlight) {
            existing
        } else {
            AiCorePromptDownloadStateStore.markDownloadQueued()
            runCatching {
                client.download(
                    object : DownloadCallback {
                        override fun onDownloadStarted(bytesToDownload: Long) {
                            AiCorePromptDownloadStateStore.markDownloadStarted(bytesToDownload)
                        }

                        override fun onDownloadProgress(totalBytesDownloaded: Long) {
                            AiCorePromptDownloadStateStore.markDownloadProgress(totalBytesDownloaded)
                        }

                        override fun onDownloadCompleted() {
                            AiCorePromptDownloadStateStore.markDownloadCompleted()
                        }

                        override fun onDownloadFailed(e: GenAiException) {
                            AiCorePromptDownloadStateStore.markDownloadFailed(e.toReadableMessage())
                        }
                    }
                )
            }.onFailure { error ->
                AiCorePromptDownloadStateStore.markDownloadFailed(error.toReadableMessage())
            }
            AiCorePromptDownloadStateStore.snapshot()
        }
    }

    companion object {
        private const val STATUS_TIMEOUT_SECONDS = 3L
        private val lock = Any()
    }
}

private object AiCorePromptDownloadStateStore {
    private val lock = Any()

    private var state: AiCorePromptDownloadUiState = baseStateFor(
        status = FeatureStatus.UNAVAILABLE,
        previous = null,
        overrideStatusKey = "unavailable",
        overrideStatusLabel = "Prompt 세션 미가용",
        overrideSummary = "Gemini Nano 상태를 아직 확인하지 않았습니다."
    )

    fun snapshot(): AiCorePromptDownloadUiState = synchronized(lock) { state }

    fun mergeFeatureStatus(status: Int): AiCorePromptDownloadUiState = synchronized(lock) {
        state = when {
            state.downloadInFlight && status == FeatureStatus.DOWNLOADABLE -> {
                state.copy(lastUpdatedAtMillis = System.currentTimeMillis())
            }
            else -> baseStateFor(status, state)
        }
        state
    }

    fun markProbeError(message: String): AiCorePromptDownloadUiState = synchronized(lock) {
        state = state.copy(
            availableForInference = false,
            statusKey = "status_probe_error",
            statusLabel = "Prompt 상태 확인 실패",
            summary = "ML Kit Prompt 상태 확인 실패: $message",
            downloadInFlight = false,
            canDownload = false,
            canRetry = false,
            lastError = message,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    fun markDownloadQueued(): AiCorePromptDownloadUiState = synchronized(lock) {
        state = state.copy(
            featureStatus = FeatureStatus.DOWNLOADING,
            statusKey = "downloading",
            statusLabel = "Prompt 모델 다운로드 시작 중",
            summary = "Gemini Nano 다운로드를 시작하는 중입니다.",
            downloadInFlight = true,
            canDownload = false,
            canRetry = false,
            lastError = null,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    fun markDownloadingHint(summary: String): AiCorePromptDownloadUiState = synchronized(lock) {
        state = state.copy(
            featureStatus = FeatureStatus.DOWNLOADING,
            statusKey = "downloading",
            statusLabel = "Prompt 모델 다운로드 중",
            summary = summary,
            downloadInFlight = true,
            canDownload = false,
            canRetry = false,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    fun markDownloadStarted(bytesToDownload: Long): AiCorePromptDownloadUiState = synchronized(lock) {
        val total = bytesToDownload.takeIf { it > 0L }
        state = state.copy(
            featureStatus = FeatureStatus.DOWNLOADING,
            statusKey = "downloading",
            statusLabel = "Prompt 모델 다운로드 중",
            summary = total?.let {
                "Gemini Nano 다운로드를 시작했습니다. 총 ${formatByteCount(it)} 준비가 필요합니다."
            } ?: "Gemini Nano 다운로드를 시작했습니다.",
            bytesToDownload = total,
            downloadedBytes = 0L,
            progressPercent = 0,
            downloadInFlight = true,
            canDownload = false,
            canRetry = false,
            lastError = null,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    fun markDownloadProgress(downloaded: Long): AiCorePromptDownloadUiState = synchronized(lock) {
        val total = state.bytesToDownload
        val percent = if (total != null && total > 0L) {
            ((downloaded.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 99)
        } else {
            null
        }
        state = state.copy(
            featureStatus = FeatureStatus.DOWNLOADING,
            statusKey = "downloading",
            statusLabel = "Prompt 모델 다운로드 중",
            summary = if (total != null && total > 0L) {
                "Gemini Nano 다운로드 진행 중입니다. ${formatByteCount(downloaded)} / ${formatByteCount(total)}"
            } else {
                "Gemini Nano 다운로드 진행 중입니다. ${formatByteCount(downloaded)} 완료"
            },
            downloadedBytes = downloaded,
            progressPercent = percent,
            downloadInFlight = true,
            canDownload = false,
            canRetry = false,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    fun markDownloadCompleted(): AiCorePromptDownloadUiState = synchronized(lock) {
        val completedBytes = state.bytesToDownload ?: state.downloadedBytes
        state = state.copy(
            featureStatus = FeatureStatus.AVAILABLE,
            availableForInference = true,
            statusKey = "available",
            statusLabel = "Prompt 모델 준비됨",
            summary = "Gemini Nano 다운로드가 완료되어 Prompt session 을 바로 열 수 있습니다.",
            downloadedBytes = completedBytes,
            progressPercent = 100,
            downloadInFlight = false,
            canDownload = false,
            canRetry = false,
            lastError = null,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    fun markDownloadFailed(message: String): AiCorePromptDownloadUiState = synchronized(lock) {
        val retryable = baseStateFor(
            status = FeatureStatus.DOWNLOADABLE,
            previous = state,
            overrideSummary = "Gemini Nano 다운로드 실패: $message. 네트워크를 확인한 뒤 다시 시도하세요.",
            overrideStatusLabel = "Prompt 모델 재시도 가능"
        )
        state = retryable.copy(
            lastError = message,
            canRetry = true,
            canDownload = true,
            downloadInFlight = false,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        state
    }

    private fun baseStateFor(
        status: Int,
        previous: AiCorePromptDownloadUiState?,
        overrideStatusKey: String? = null,
        overrideStatusLabel: String? = null,
        overrideSummary: String? = null
    ): AiCorePromptDownloadUiState {
        val statusKey = overrideStatusKey ?: when (status) {
            FeatureStatus.AVAILABLE -> "available"
            FeatureStatus.DOWNLOADABLE -> "downloadable"
            FeatureStatus.DOWNLOADING -> "downloading"
            else -> "unavailable"
        }
        val lastError = previous?.lastError
        val statusLabel = overrideStatusLabel ?: when (status) {
            FeatureStatus.AVAILABLE -> "Prompt 모델 준비됨"
            FeatureStatus.DOWNLOADABLE -> if (lastError != null) "Prompt 모델 재시도 가능" else "Prompt 모델 다운로드 가능"
            FeatureStatus.DOWNLOADING -> "Prompt 모델 다운로드 중"
            else -> "Prompt 세션 미가용"
        }
        val summary = overrideSummary ?: when (status) {
            FeatureStatus.AVAILABLE -> "ML Kit Prompt / AICore 상태 AVAILABLE: Gemini Nano 추론 가능"
            FeatureStatus.DOWNLOADABLE -> if (lastError != null) {
                "ML Kit Prompt / AICore 상태 DOWNLOADABLE: 직전 다운로드가 실패해 재시도가 필요합니다."
            } else {
                "ML Kit Prompt / AICore 상태 DOWNLOADABLE: 모델 다운로드 후 추론 가능"
            }
            FeatureStatus.DOWNLOADING -> previous?.progressLabel?.let {
                "ML Kit Prompt / AICore 상태 DOWNLOADING: $it"
            } ?: "ML Kit Prompt / AICore 상태 DOWNLOADING: 모델 준비 중"
            else -> "ML Kit Prompt / AICore 상태 UNAVAILABLE: 이 기기에서는 현재 Gemini Nano를 사용할 수 없습니다."
        }
        return AiCorePromptDownloadUiState(
            featureStatus = status,
            statusKey = statusKey,
            availableForInference = status == FeatureStatus.AVAILABLE,
            statusLabel = statusLabel,
            summary = summary,
            bytesToDownload = previous?.bytesToDownload,
            downloadedBytes = when (status) {
                FeatureStatus.AVAILABLE -> previous?.bytesToDownload ?: previous?.downloadedBytes
                FeatureStatus.DOWNLOADING -> previous?.downloadedBytes
                else -> previous?.downloadedBytes
            },
            progressPercent = when (status) {
                FeatureStatus.AVAILABLE -> 100
                FeatureStatus.DOWNLOADING -> previous?.progressPercent
                else -> null
            },
            downloadInFlight = status == FeatureStatus.DOWNLOADING,
            canDownload = status == FeatureStatus.DOWNLOADABLE,
            canRetry = status == FeatureStatus.DOWNLOADABLE && lastError != null,
            lastError = if (status == FeatureStatus.AVAILABLE) null else lastError,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
    }
}

private fun formatByteCount(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> "%.1fGB".format(bytes / gb)
        bytes >= mb -> "%.1fMB".format(bytes / mb)
        bytes >= kb -> "%.1fKB".format(bytes / kb)
        else -> "${bytes}B"
    }
}

private fun Throwable.toReadableMessage(): String =
    "${javaClass.simpleName}${message?.let { ": $it" } ?: ""}"

private fun GenAiException.toReadableMessage(): String =
    message ?: javaClass.simpleName
