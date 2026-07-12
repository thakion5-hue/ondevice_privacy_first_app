package com.genspark.privacyfirstai.ai

import android.os.Build
import com.genspark.privacyfirstai.domain.model.SpamModelResult
import com.genspark.privacyfirstai.domain.model.VendorRuntimeOption

interface SpamRuntimeAdapter {
    val runtime: VendorRuntimeOption

    fun availability(): RuntimeAdapterAvailability

    fun classify(text: String): SpamModelResult?

    fun modelSource(): String = runtime.storageKey

    fun runtimeLabel(): String = runtime.label
}

enum class RuntimeBadgeTone {
    Positive,
    Neutral,
    Warning,
    Negative
}

data class RuntimeAdapterAvailability(
    val runtime: VendorRuntimeOption,
    val available: Boolean,
    val statusLabel: String,
    val detail: String,
    val badgeTone: RuntimeBadgeTone
)

data class RuntimeClassificationResult(
    val result: SpamModelResult,
    val modelSource: String,
    val runtimeLabel: String,
    val fallbackReason: String? = null
)

class OnDeviceRuntimeRegistry(
    private val tfliteClassifier: TfliteSpamClassifier,
    private val geminiNanoConnectorProvider: () -> GeminiNanoRuntimeConnector = { StubGeminiNanoRuntimeConnector() }
) {
    private fun adapters(): List<SpamRuntimeAdapter> = listOf(
        TfliteSpamRuntimeAdapter(tfliteClassifier),
        GeminiNanoSpamRuntimeAdapter(geminiNanoConnectorProvider()),
        QualcommAiEngineSpamRuntimeAdapter(),
        SamsungGaussSpamRuntimeAdapter()
    )

    fun getAvailability(): List<RuntimeAdapterAvailability> = adapters().map { it.availability() }

    fun availabilityFor(runtime: VendorRuntimeOption): RuntimeAdapterAvailability =
        adapters().firstOrNull { it.runtime == runtime }?.availability()
            ?: RuntimeAdapterAvailability(
                runtime = runtime,
                available = false,
                statusLabel = "미확인",
                detail = "등록되지 않은 런타임입니다.",
                badgeTone = RuntimeBadgeTone.Negative
            )

    fun geminiNanoConnectorDiagnostics(): GeminiNanoRuntimeDiagnostic =
        geminiNanoConnectorProvider().diagnostics()

    fun geminiNanoConnectorContract(): GeminiNanoConnectorContract =
        geminiNanoConnectorProvider().contract

    fun classify(
        text: String,
        preferredRuntime: VendorRuntimeOption
    ): RuntimeClassificationResult? {
        val activeAdapters = adapters()
        val preferredAdapter = activeAdapters.firstOrNull { it.runtime == preferredRuntime }
            ?: activeAdapters.first()
        val directResult = preferredAdapter.classify(text)
        if (directResult != null) {
            return RuntimeClassificationResult(
                result = directResult,
                modelSource = preferredAdapter.modelSource(),
                runtimeLabel = preferredAdapter.runtimeLabel()
            )
        }

        if (preferredRuntime == VendorRuntimeOption.TfliteBuiltin) {
            return null
        }

        val fallback = activeAdapters.first { it.runtime == VendorRuntimeOption.TfliteBuiltin }
        val fallbackResult = fallback.classify(text) ?: return null
        val availability = preferredAdapter.availability()
        return RuntimeClassificationResult(
            result = fallbackResult,
            modelSource = "${preferredAdapter.modelSource()}_fallback_tflite",
            runtimeLabel = "${preferredAdapter.runtimeLabel()} → 기본 TFLite 폴백",
            fallbackReason = availability.detail
        )
    }
}

private class TfliteSpamRuntimeAdapter(
    private val classifier: TfliteSpamClassifier
) : SpamRuntimeAdapter {
    override val runtime: VendorRuntimeOption = VendorRuntimeOption.TfliteBuiltin

    override fun availability(): RuntimeAdapterAvailability = RuntimeAdapterAvailability(
        runtime = runtime,
        available = classifier.isReady,
        statusLabel = if (classifier.isReady) "준비됨" else "초기화 실패",
        detail = classifier.lastInitError
            ?: "앱 번들 TFLite 분류기를 직접 사용합니다.",
        badgeTone = if (classifier.isReady) RuntimeBadgeTone.Positive else RuntimeBadgeTone.Negative
    )

    override fun classify(text: String): SpamModelResult? = classifier.classify(text)
}

private class GeminiNanoSpamRuntimeAdapter(
    private val connector: GeminiNanoRuntimeConnector
) : SpamRuntimeAdapter {
    override val runtime: VendorRuntimeOption = VendorRuntimeOption.GeminiNano

    override fun availability(): RuntimeAdapterAvailability {
        val diagnostic = connector.diagnostics()
        return RuntimeAdapterAvailability(
            runtime = runtime,
            available = diagnostic.available,
            statusLabel = diagnostic.statusLabel,
            detail = diagnostic.detail,
            badgeTone = when {
                diagnostic.available -> RuntimeBadgeTone.Positive
                diagnostic.connectorMode.storageKey == "aicore_todo" && Build.VERSION.SDK_INT >= 34 -> RuntimeBadgeTone.Neutral
                diagnostic.connectorMode.storageKey == "legacy_stub" && Build.VERSION.SDK_INT >= 34 -> RuntimeBadgeTone.Neutral
                else -> RuntimeBadgeTone.Negative
            }
        )
    }

    override fun classify(text: String): SpamModelResult? = connector.classify(text)

    override fun modelSource(): String = "${runtime.storageKey}_${connector.mode.storageKey}"

    override fun runtimeLabel(): String = "${runtime.label}(${connector.mode.label})"
}

private class QualcommAiEngineSpamRuntimeAdapter : SpamRuntimeAdapter {
    override val runtime: VendorRuntimeOption = VendorRuntimeOption.QualcommAiEngine

    override fun availability(): RuntimeAdapterAvailability {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val likelySnapdragon = manufacturer.contains("xiaomi") ||
            manufacturer.contains("oneplus") ||
            manufacturer.contains("nothing") ||
            manufacturer.contains("sony")
        return RuntimeAdapterAvailability(
            runtime = runtime,
            available = false,
            statusLabel = if (likelySnapdragon) "스캐폴딩" else "미연결",
            detail = "Qualcomm AI Engine / QNN 연결용 인터페이스만 준비되어 있습니다. 실제 라이브러리 연결 전까지는 기본 TFLite로 폴백합니다.",
            badgeTone = if (likelySnapdragon) RuntimeBadgeTone.Neutral else RuntimeBadgeTone.Warning
        )
    }

    override fun classify(text: String): SpamModelResult? = null
}

private class SamsungGaussSpamRuntimeAdapter : SpamRuntimeAdapter {
    override val runtime: VendorRuntimeOption = VendorRuntimeOption.SamsungGauss

    override fun availability(): RuntimeAdapterAvailability {
        val isSamsung = Build.MANUFACTURER.orEmpty().contains("samsung", ignoreCase = true)
        return RuntimeAdapterAvailability(
            runtime = runtime,
            available = false,
            statusLabel = if (isSamsung) "스캐폴딩(삼성 기기)" else "미연결",
            detail = "Samsung Gauss 온디바이스 런타임을 위한 어댑터 틀만 제공됩니다. 실제 SDK 연결 전에는 TFLite로 자동 폴백합니다.",
            badgeTone = if (isSamsung) RuntimeBadgeTone.Neutral else RuntimeBadgeTone.Warning
        )
    }

    override fun classify(text: String): SpamModelResult? = null
}
