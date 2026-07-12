package com.genspark.privacyfirstai.ai

import com.genspark.privacyfirstai.ai.gemininano.provider.AiCoreTodoProviderV2
import com.genspark.privacyfirstai.ai.gemininano.provider.GeminiNanoProviderContractV2
import com.genspark.privacyfirstai.ai.gemininano.provider.GeminiNanoProviderDiagnosticV2
import com.genspark.privacyfirstai.ai.gemininano.provider.GeminiNanoRuntimeProviderV2
import com.genspark.privacyfirstai.ai.gemininano.provider.LegacyStubProviderV2
import com.genspark.privacyfirstai.ai.gemininano.provider.QaFakeProviderV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoRuntimeSessionV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryContractV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryDiagnosticV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryV2
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode

/**
 * v6.4 어댑터 레이어.
 *
 * v6.3 에서는 이 파일 하나에 provider / session factory 계약 + 구현이 모두 들어 있었지만,
 * v6.4 에서는 실제 계약/구현이 `ai.gemininano.provider.*` 와 `ai.gemininano.sessionfactory.*`
 * 로 파일 단위 분리되었다. 이 파일은 기존 [GeminiNanoRuntimeConnector] 스택이 그대로 동작하도록
 * V2 계약을 v6.3 계약으로 노출해주는 얇은 어댑터만 유지한다.
 */

data class GeminiNanoProviderContract(
    val providerLabel: String,
    val readinessPhase: String,
    val bindingTarget: String,
    val capabilityProbe: String,
    val entitlementGate: String,
    val todoChecklist: List<String>
)

data class GeminiNanoProviderDiagnostic(
    val available: Boolean,
    val statusLabel: String,
    val detail: String,
    val providerSummary: String
)

data class GeminiNanoSessionFactoryContract(
    val factoryLabel: String,
    val readinessPhase: String,
    val sessionType: String,
    val inferenceEntryPoint: String,
    val outputExpectation: String,
    val fallbackPolicy: String,
    val todoChecklist: List<String>
)

data class GeminiNanoSessionFactoryDiagnostic(
    val available: Boolean,
    val statusLabel: String,
    val detail: String,
    val factorySummary: String
)

interface GeminiNanoRuntimeProvider {
    val mode: GeminiNanoConnectorMode
    val providerContract: GeminiNanoProviderContract

    fun diagnostics(): GeminiNanoProviderDiagnostic

    fun sessionFactory(): GeminiNanoSessionFactory
}

interface GeminiNanoSessionFactory {
    val factoryContract: GeminiNanoSessionFactoryContract

    fun diagnostics(): GeminiNanoSessionFactoryDiagnostic

    fun openSession(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeSession?
}

interface GeminiNanoRuntimeSession {
    val sessionLabel: String

    fun infer(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference?
}

// region v2 -> v1 adapters ------------------------------------------------------------------

private fun GeminiNanoProviderContractV2.toV1(): GeminiNanoProviderContract = GeminiNanoProviderContract(
    providerLabel = providerLabel,
    readinessPhase = readinessPhase,
    bindingTarget = bindingTarget,
    capabilityProbe = capabilityProbe,
    entitlementGate = entitlementGate,
    todoChecklist = todoChecklist
)

private fun GeminiNanoProviderDiagnosticV2.toV1(): GeminiNanoProviderDiagnostic = GeminiNanoProviderDiagnostic(
    available = available,
    statusLabel = statusLabel,
    detail = detail,
    providerSummary = providerSummary
)

private fun GeminiNanoSessionFactoryContractV2.toV1(): GeminiNanoSessionFactoryContract = GeminiNanoSessionFactoryContract(
    factoryLabel = factoryLabel,
    readinessPhase = readinessPhase,
    sessionType = sessionType,
    inferenceEntryPoint = inferenceEntryPoint,
    outputExpectation = outputExpectation,
    fallbackPolicy = fallbackPolicy,
    todoChecklist = todoChecklist
)

private fun GeminiNanoSessionFactoryDiagnosticV2.toV1(): GeminiNanoSessionFactoryDiagnostic = GeminiNanoSessionFactoryDiagnostic(
    available = available,
    statusLabel = statusLabel,
    detail = detail,
    factorySummary = factorySummary
)

private class ProviderV2Adapter(private val delegate: GeminiNanoRuntimeProviderV2) : GeminiNanoRuntimeProvider {
    override val mode: GeminiNanoConnectorMode = delegate.mode
    override val providerContract: GeminiNanoProviderContract = delegate.providerContract.toV1()

    private val factoryAdapter: GeminiNanoSessionFactory by lazy {
        SessionFactoryV2Adapter(delegate.sessionFactory())
    }

    override fun diagnostics(): GeminiNanoProviderDiagnostic = delegate.diagnostics().toV1()
    override fun sessionFactory(): GeminiNanoSessionFactory = factoryAdapter
}

private class SessionFactoryV2Adapter(private val delegate: GeminiNanoSessionFactoryV2) : GeminiNanoSessionFactory {
    override val factoryContract: GeminiNanoSessionFactoryContract = delegate.factoryContract.toV1()
    override fun diagnostics(): GeminiNanoSessionFactoryDiagnostic = delegate.diagnostics().toV1()
    override fun openSession(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeSession? {
        val v2 = delegate.openSession(request) ?: return null
        return SessionV2Adapter(v2)
    }
}

private class SessionV2Adapter(private val delegate: GeminiNanoRuntimeSessionV2) : GeminiNanoRuntimeSession {
    override val sessionLabel: String = delegate.sessionLabel
    override fun infer(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference? = delegate.infer(request)
}

// endregion --------------------------------------------------------------------------------

// region Public constructors that keep v6.3 call sites working -----------------------------

class AiCoreTodoGeminiNanoRuntimeProvider : GeminiNanoRuntimeProvider by ProviderV2Adapter(AiCoreTodoProviderV2())
class QaFakeGeminiNanoRuntimeProvider : GeminiNanoRuntimeProvider by ProviderV2Adapter(QaFakeProviderV2())
class LegacyStubGeminiNanoRuntimeProvider : GeminiNanoRuntimeProvider by ProviderV2Adapter(LegacyStubProviderV2())

// endregion --------------------------------------------------------------------------------
