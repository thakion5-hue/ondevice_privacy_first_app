package com.genspark.privacyfirstai.ai

import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode
import com.genspark.privacyfirstai.domain.model.SpamModelResult
import java.util.UUID

private val DEFAULT_GEMINI_CANDIDATE_LABELS = listOf("safe", "caution", "fraud")

/**
 * Gemini Nano 실제 연결 지점을 어댑터와 분리하기 위한 경계 인터페이스.
 * v6.4에서는 provider / session factory 계약을 파일·패키지 단위로 물리 분리했으며,
 * fake session 은 매 추론마다 입력/출력 trace 메타데이터를 남긴다.
 */
interface GeminiNanoRuntimeConnector {
    val mode: GeminiNanoConnectorMode
    val contract: GeminiNanoConnectorContract

    fun diagnostics(): GeminiNanoRuntimeDiagnostic

    fun classify(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference?

    fun classify(text: String): SpamModelResult? =
        classify(GeminiNanoInferenceRequest.fromText(text))?.result
}

data class GeminiNanoConnectorContract(
    val bindingTarget: String,
    val readinessPhase: String,
    val providerLayer: String,
    val sessionFactory: String,
    val inferenceEntryPoint: String,
    val outputExpectation: String,
    val fallbackPolicy: String,
    val todoChecklist: List<String>
)

data class GeminiNanoRuntimeDiagnostic(
    val available: Boolean,
    val statusLabel: String,
    val detail: String,
    val connectorMode: GeminiNanoConnectorMode,
    val bindingPhase: String,
    val contractSummary: String,
    val providerStatusLabel: String,
    val providerSummary: String,
    val sessionFactoryStatusLabel: String,
    val sessionFactorySummary: String
)

data class GeminiNanoInferenceRequest(
    val requestId: String,
    val text: String,
    val candidateLabels: List<String> = DEFAULT_GEMINI_CANDIDATE_LABELS,
    val maxOutputTokens: Int = 8,
    val temperature: Float = 0f,
    val topK: Int = 1,
    val allowLocalFallback: Boolean = true
) {
    companion object {
        fun fromText(text: String): GeminiNanoInferenceRequest = GeminiNanoInferenceRequest(
            requestId = UUID.randomUUID().toString(),
            text = text
        )
    }
}

data class GeminiNanoRuntimeInference(
    val result: SpamModelResult,
    val traceLabel: String,
    val engineMessage: String
)

private abstract class ProviderBackedGeminiNanoRuntimeConnector(
    private val provider: GeminiNanoRuntimeProvider
) : GeminiNanoRuntimeConnector {
    override val mode: GeminiNanoConnectorMode = provider.mode

    override val contract: GeminiNanoConnectorContract
        get() {
            val providerContract = provider.providerContract
            val factoryContract = provider.sessionFactory().factoryContract
            return GeminiNanoConnectorContract(
                bindingTarget = providerContract.bindingTarget,
                readinessPhase = "${providerContract.readinessPhase}/${factoryContract.readinessPhase}",
                providerLayer = "${providerContract.providerLabel} · ${providerContract.capabilityProbe}",
                sessionFactory = "${factoryContract.factoryLabel} · ${factoryContract.sessionType}",
                inferenceEntryPoint = factoryContract.inferenceEntryPoint,
                outputExpectation = factoryContract.outputExpectation,
                fallbackPolicy = factoryContract.fallbackPolicy,
                todoChecklist = providerContract.todoChecklist + factoryContract.todoChecklist
            )
        }

    override fun diagnostics(): GeminiNanoRuntimeDiagnostic {
        val providerDiagnostic = provider.diagnostics()
        val factoryDiagnostic = provider.sessionFactory().diagnostics()
        val currentContract = contract
        return GeminiNanoRuntimeDiagnostic(
            available = providerDiagnostic.available && factoryDiagnostic.available,
            statusLabel = when {
                providerDiagnostic.available && factoryDiagnostic.available -> "준비됨"
                providerDiagnostic.available -> factoryDiagnostic.statusLabel
                else -> providerDiagnostic.statusLabel
            },
            detail = listOf(
                providerDiagnostic.detail,
                factoryDiagnostic.detail
            ).joinToString(" "),
            connectorMode = mode,
            bindingPhase = currentContract.readinessPhase,
            contractSummary = "${currentContract.providerLayer} · ${currentContract.sessionFactory} · ${currentContract.fallbackPolicy}",
            providerStatusLabel = providerDiagnostic.statusLabel,
            providerSummary = providerDiagnostic.providerSummary,
            sessionFactoryStatusLabel = factoryDiagnostic.statusLabel,
            sessionFactorySummary = factoryDiagnostic.factorySummary
        )
    }

    override fun classify(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference? {
        val session = provider.sessionFactory().openSession(request) ?: return null
        return session.infer(request)
    }
}

class AiCoreTodoGeminiNanoRuntimeConnector(
    provider: GeminiNanoRuntimeProvider = AiCoreTodoGeminiNanoRuntimeProvider()
) : ProviderBackedGeminiNanoRuntimeConnector(provider)

class QaFakeGeminiNanoRuntimeConnector(
    provider: GeminiNanoRuntimeProvider = QaFakeGeminiNanoRuntimeProvider()
) : ProviderBackedGeminiNanoRuntimeConnector(provider)

class StubGeminiNanoRuntimeConnector(
    provider: GeminiNanoRuntimeProvider = LegacyStubGeminiNanoRuntimeProvider()
) : ProviderBackedGeminiNanoRuntimeConnector(provider)
