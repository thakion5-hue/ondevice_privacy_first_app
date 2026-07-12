package com.genspark.privacyfirstai.feature.security

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genspark.privacyfirstai.ai.RuntimeAdapterAvailability
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.domain.model.ThreatScan
import com.genspark.privacyfirstai.domain.model.ThreatScanRecord
import com.genspark.privacyfirstai.domain.model.VendorRuntimeOption
import kotlinx.coroutines.launch

class CleanGuardViewModel(
    private val container: AppContainer
) : ViewModel() {
    var input by mutableStateOf("지금 클릭하면 무료 지급! AI 생성 투자 영상으로 하루 100% 수익")
    var result by mutableStateOf<ThreatScan?>(null)
        private set
    var ocrPreview by mutableStateOf("광고/메시지 이미지를 선택하면 한국어 OCR 결과를 여기서 미리 볼 수 있습니다.")
        private set
    var recentRecords by mutableStateOf<List<ThreatScanRecord>>(emptyList())
        private set
    var activeSettings by mutableStateOf(container.preferencesStore.getSettings())
        private set
    var selectedRuntimeDiagnostic by mutableStateOf(
        container.runtimeRegistry.availabilityFor(container.preferencesStore.getSettings().preferredRuntime)
    )
        private set
    var geminiConnectorDiagnostic by mutableStateOf(container.runtimeRegistry.geminiNanoConnectorDiagnostics())
        private set
    var busy by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            recentRecords = container.localStore.getThreatScans()
            refreshSettings()
            result = scanInternal(input)
        }
    }

    fun updateInput(value: String) {
        input = value
    }

    fun refreshSettings() {
        activeSettings = container.preferencesStore.getSettings()
        selectedRuntimeDiagnostic = container.runtimeRegistry.availabilityFor(
            activeSettings.preferredRuntime
        )
        geminiConnectorDiagnostic = container.runtimeRegistry.geminiNanoConnectorDiagnostics()
    }

    fun scan() {
        viewModelScope.launch {
            refreshSettings()
            result = scanInternal(input)
            result?.let {
                container.localStore.saveThreatScan(
                    source = "manual_text",
                    sourceLabel = "직접 입력",
                    scannedText = input,
                    result = it
                )
                recentRecords = container.localStore.getThreatScans()
            }
        }
    }

    fun scanImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            busy = true
            refreshSettings()
            ocrPreview = "이미지에서 텍스트를 추출하는 중..."
            runCatching {
                container.receiptOcr.extractText(context, uri)
            }.onSuccess { text ->
                val normalized = text.ifBlank { "텍스트를 인식하지 못했습니다." }
                input = normalized
                ocrPreview = normalized.take(500)
                result = scanInternal(normalized)
                result?.let {
                    container.localStore.saveThreatScan(
                        source = "ocr_image",
                        sourceLabel = uri.lastPathSegment ?: "선택 이미지",
                        scannedText = normalized,
                        result = it
                    )
                    recentRecords = container.localStore.getThreatScans()
                }
            }.onFailure { error ->
                ocrPreview = "OCR 실패: ${error.message ?: "알 수 없는 오류"}"
            }
            busy = false
        }
    }

    fun engineSummary(): String =
        "현재 엔진: ${activeSettings.spamFilterMode.label} · 선호 런타임 ${activeSettings.preferredRuntime.label}"

    fun runtimeStatusDetail(): String = selectedRuntimeDiagnostic.detail

    fun runtimeFallbackLabel(): String? =
        if (!selectedRuntimeDiagnostic.available &&
            activeSettings.preferredRuntime != VendorRuntimeOption.TfliteBuiltin
        ) {
            "기본 TFLite 자동 폴백"
        } else {
            null
        }

    fun shouldShowGeminiConnector(): Boolean =
        activeSettings.preferredRuntime == VendorRuntimeOption.GeminiNano

    fun geminiConnectorModeLabel(): String = activeSettings.geminiNanoConnectorMode.label

    fun geminiConnectorDetail(): String {
        val trace = com.genspark.privacyfirstai.ai.gemininano.trace.geminiNanoInferenceTraceSummary()
        val traceHint = if (trace.bufferedTraces > 0) {
            " trace=${trace.bufferedTraces}/${trace.bufferCapacity} last=${trace.lastTraceLabel} latency=${trace.lastTraceLatencyMillis ?: 0}ms"
        } else {
            " trace=대기"
        }
        return "${geminiConnectorDiagnostic.detail} provider=${geminiConnectorDiagnostic.providerStatusLabel} / factory=${geminiConnectorDiagnostic.sessionFactoryStatusLabel}$traceHint"
    }

    private fun scanInternal(text: String): ThreatScan? =
        container.threatGuard.scan(
            text = text,
            mode = activeSettings.spamFilterMode,
            preferredRuntime = activeSettings.preferredRuntime
        )
}
