package com.genspark.privacyfirstai.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genspark.privacyfirstai.ai.RuntimeAdapterAvailability
import com.genspark.privacyfirstai.ai.gemininano.AiCorePromptDownloadUiState
import com.genspark.privacyfirstai.ai.gemininano.trace.GeminiNanoInferenceTraceSummary
import com.genspark.privacyfirstai.ai.gemininano.trace.geminiNanoInferenceTraceSummary
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.domain.model.DebugSnapshot
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode
import com.genspark.privacyfirstai.domain.model.SpamFilterMode
import com.genspark.privacyfirstai.domain.model.VendorRuntimeOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    var settings by mutableStateOf(container.preferencesStore.getSettings())
        private set

    var runtimeManifest by mutableStateOf(container.modelManifestStore.load())
        private set

    var runtimeDiagnostics by mutableStateOf<List<RuntimeAdapterAvailability>>(emptyList())
        private set

    var debugSnapshot by mutableStateOf<DebugSnapshot?>(null)
        private set

    var geminiConnectorDiagnostic by mutableStateOf(container.runtimeRegistry.geminiNanoConnectorDiagnostics())
        private set

    var aiCorePromptDownloadState by mutableStateOf(container.aiCorePromptDownloadController.snapshot())
        private set

    var aiCorePromptActionBusy by mutableStateOf(false)
        private set

    var exportHistory by mutableStateOf<List<ExportedLogFile>>(emptyList())
        private set

    var uiMessage by mutableStateOf<UiMessage?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    private var uiMessageCounter = 0L
    private var aiCorePromptRefreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        settings = container.preferencesStore.getSettings()
        runtimeManifest = container.modelManifestStore.load()
        aiCorePromptDownloadState = container.aiCorePromptDownloadController.refreshStatus()
        refreshRuntimeDiagnostics()
        exportHistory = container.logExporter.listExports().toExportedLogFiles()
        syncAiCorePromptRefreshLoop()
        viewModelScope.launch {
            debugSnapshot = container.buildDebugSnapshot()
        }
    }

    fun refreshAiCorePromptState(showFeedback: Boolean = false) {
        aiCorePromptDownloadState = container.aiCorePromptDownloadController.refreshStatus()
        refreshRuntimeDiagnostics()
        syncAiCorePromptRefreshLoop()
        viewModelScope.launch {
            debugSnapshot = container.buildDebugSnapshot()
        }
        if (showFeedback) {
            postStatus(
                aiCorePromptStatusMessage(aiCorePromptDownloadState),
                tone = if (aiCorePromptDownloadState.lastError != null) {
                    UiFeedbackTone.Error
                } else {
                    UiFeedbackTone.Info
                }
            )
        }
    }

    fun startOrRetryAiCorePromptDownload() {
        if (aiCorePromptActionBusy) return
        aiCorePromptActionBusy = true
        viewModelScope.launch {
            val state = container.aiCorePromptDownloadController.requestDownload()
            aiCorePromptDownloadState = state
            refreshRuntimeDiagnostics()
            syncAiCorePromptRefreshLoop()
            debugSnapshot = container.buildDebugSnapshot()
            postStatus(
                when {
                    state.availableForInference -> "Gemini Nano Prompt 모델이 이미 준비되어 있습니다."
                    state.downloadInFlight -> "Gemini Nano 다운로드를 시작했습니다. 진행률은 자동으로 갱신됩니다."
                    state.canRetry -> "Gemini Nano 다운로드를 다시 시도할 수 있습니다. 네트워크 상태를 확인해 주세요."
                    else -> aiCorePromptStatusMessage(state)
                },
                tone = if (state.lastError != null) UiFeedbackTone.Error else UiFeedbackTone.Success
            )
            aiCorePromptActionBusy = false
        }
    }

    fun setSpamFilterMode(mode: SpamFilterMode) {
        container.preferencesStore.setSpamFilterMode(mode)
        settings = container.preferencesStore.getSettings()
        refreshRuntimeDiagnostics()
        postStatus("스팸 판정 모드를 ${mode.label}로 전환했습니다.", UiFeedbackTone.Success)
    }

    fun setPreferredRuntime(runtime: VendorRuntimeOption) {
        container.preferencesStore.setPreferredRuntime(runtime)
        settings = container.preferencesStore.getSettings()
        refreshRuntimeDiagnostics()
        postStatus(
            "선호 런타임을 ${runtime.label}(으)로 바꿨습니다. 미지원 시 자동으로 기본 TFLite로 폴백합니다.",
            UiFeedbackTone.Success
        )
        viewModelScope.launch {
            debugSnapshot = container.buildDebugSnapshot()
        }
    }

    fun setGeminiNanoConnectorMode(mode: GeminiNanoConnectorMode) {
        container.preferencesStore.setGeminiNanoConnectorMode(mode)
        settings = container.preferencesStore.getSettings()
        aiCorePromptDownloadState = container.aiCorePromptDownloadController.refreshStatus()
        refreshRuntimeDiagnostics()
        syncAiCorePromptRefreshLoop()
        postStatus(
            "Gemini Nano connector 를 ${mode.label}(으)로 전환했습니다.",
            UiFeedbackTone.Success
        )
        viewModelScope.launch {
            debugSnapshot = container.buildDebugSnapshot()
        }
    }

    fun setAutoIndexAfterPermissionGrant(enabled: Boolean) {
        container.preferencesStore.setAutoIndexAfterPermissionGrant(enabled)
        settings = container.preferencesStore.getSettings()
    }

    fun currentModeSummary(): String = settings.spamFilterMode.description

    fun currentModeDetail(): String = settings.spamFilterMode.detail

    fun runtimeSummary(): String {
        val enabled = runtimeManifest.runtimes.count { it.status == "enabled" }
        return "프로필 ${runtimeManifest.appProfile} · 활성 런타임 ${enabled}개 · 선호 텍스트 런타임 ${settings.preferredRuntime.label}"
    }

    fun selectedRuntimeDetail(): String = settings.preferredRuntime.detail

    fun selectedRuntimeDiagnostic(): RuntimeAdapterAvailability? =
        runtimeDiagnostics.firstOrNull { it.runtime == settings.preferredRuntime }

    fun geminiConnectorSummary(): String {
        val trace = geminiNanoInferenceTraceSummary()
        val traceHint = if (trace.bufferedTraces > 0) {
            " · trace ${trace.bufferedTraces}/${trace.bufferCapacity}(last=${trace.lastTraceLabel ?: "-"})"
        } else {
            " · trace 대기"
        }
        return "${settings.geminiNanoConnectorMode.description} · provider ${geminiConnectorDiagnostic.providerStatusLabel} · factory ${geminiConnectorDiagnostic.sessionFactoryStatusLabel}$traceHint"
    }

    fun geminiConnectorDetail(): String =
        "${geminiConnectorDiagnostic.detail} provider=${geminiConnectorDiagnostic.providerSummary} / factory=${geminiConnectorDiagnostic.sessionFactorySummary}"

    fun geminiTraceSummary(): GeminiNanoInferenceTraceSummary = geminiNanoInferenceTraceSummary()

    fun clearThreatScans() = runDbAction("스팸 스캔 기록") {
        container.localStore.clearThreatScans()
    }

    fun clearMediaInsights() = runDbAction("이미지 라벨 분석 기록") {
        container.localStore.clearMediaInsights()
    }

    fun clearDevicePhotos() = runDbAction("MediaStore 사진 인덱스") {
        container.localStore.clearDevicePhotos()
    }

    fun clearReceipts() = runDbAction("영수증 메모") {
        container.localStore.clearReceipts()
    }

    fun clearAll() = runDbAction("모든 온디바이스 히스토리") {
        container.localStore.clearAll()
    }

    fun exportLogs() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            runCatching { container.logExporter.exportAll() }
                .onSuccess { result ->
                    postStatus(
                        "로그 저장됨: ${result.fileName} · ${result.sizeBytes / 1024}KB " +
                            "(위협 ${result.threatScanCount} · 미디어 ${result.mediaInsightCount} · " +
                            "기기 사진 ${result.devicePhotoCount} · 영수증 ${result.receiptCount})",
                        UiFeedbackTone.Success
                    )
                    debugSnapshot = container.buildDebugSnapshot()
                    exportHistory = container.logExporter.listExports().toExportedLogFiles()
                }
                .onFailure {
                    postStatus(
                        "로그 내보내기 실패: ${it.message ?: "알 수 없는 오류"}",
                        UiFeedbackTone.Error
                    )
                }
            busy = false
        }
    }

    fun clearExportedLogs() {
        if (busy) return
        busy = true
        val removed = container.logExporter.clearExports()
        exportHistory = container.logExporter.listExports().toExportedLogFiles()
        postStatus("내보낸 로그 파일 ${removed}개를 삭제했습니다.", UiFeedbackTone.Success)
        busy = false
    }

    fun postStatus(
        message: String,
        tone: UiFeedbackTone = UiFeedbackTone.Info,
        showToast: Boolean = false
    ) {
        uiMessage = UiMessage(
            id = ++uiMessageCounter,
            message = message,
            tone = tone,
            showToast = showToast
        )
    }

    fun consumeUiMessage(id: Long) {
        if (uiMessage?.id == id) {
            uiMessage = null
        }
    }

    override fun onCleared() {
        aiCorePromptRefreshJob?.cancel()
        super.onCleared()
    }

    private fun refreshRuntimeDiagnostics() {
        runtimeDiagnostics = container.runtimeRegistry.getAvailability()
        geminiConnectorDiagnostic = container.runtimeRegistry.geminiNanoConnectorDiagnostics()
    }

    private fun syncAiCorePromptRefreshLoop() {
        if (aiCorePromptDownloadState.downloadInFlight || aiCorePromptDownloadState.statusKey == "downloading") {
            if (aiCorePromptRefreshJob?.isActive == true) return
            aiCorePromptRefreshJob = viewModelScope.launch {
                while (true) {
                    delay(AICORE_REFRESH_INTERVAL_MS)
                    aiCorePromptDownloadState = container.aiCorePromptDownloadController.refreshStatus()
                    refreshRuntimeDiagnostics()
                    if (!(aiCorePromptDownloadState.downloadInFlight || aiCorePromptDownloadState.statusKey == "downloading")) {
                        debugSnapshot = container.buildDebugSnapshot()
                        break
                    }
                }
                aiCorePromptRefreshJob = null
            }
        } else {
            aiCorePromptRefreshJob?.cancel()
            aiCorePromptRefreshJob = null
        }
    }

    private fun aiCorePromptStatusMessage(state: AiCorePromptDownloadUiState): String = buildString {
        append(state.statusLabel)
        append(" · ")
        append(state.summary)
        state.progressLabel?.let {
            append(" · ")
            append(it)
        }
    }

    private fun runDbAction(label: String, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess {
                    postStatus("$label 을(를) 비웠습니다.", UiFeedbackTone.Success)
                    debugSnapshot = container.buildDebugSnapshot()
                }
                .onFailure {
                    postStatus(
                        "$label 삭제 실패: ${it.message ?: "알 수 없는 오류"}",
                        UiFeedbackTone.Error
                    )
                }
            busy = false
        }
    }

    private fun List<File>.toExportedLogFiles(): List<ExportedLogFile> = map {
        ExportedLogFile(
            fileName = it.name,
            absolutePath = it.absolutePath,
            sizeBytes = it.length(),
            modifiedAt = it.lastModified(),
            modifiedAtLabel = TIMESTAMP_FORMAT.format(Date(it.lastModified()))
        )
    }

    companion object {
        private const val AICORE_REFRESH_INTERVAL_MS = 1500L
        private val TIMESTAMP_FORMAT =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
    }
}

enum class UiFeedbackTone {
    Info,
    Success,
    Error
}

data class UiMessage(
    val id: Long,
    val message: String,
    val tone: UiFeedbackTone,
    val showToast: Boolean
)

data class ExportedLogFile(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val modifiedAtLabel: String
)
