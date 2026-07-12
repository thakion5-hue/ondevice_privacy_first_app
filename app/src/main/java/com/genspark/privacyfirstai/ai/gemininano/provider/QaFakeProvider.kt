package com.genspark.privacyfirstai.ai.gemininano.provider

import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.QaFakeSessionFactoryV2
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode

/**
 * QA Fake provider (v6.4 split).
 *
 * v6.4에서는 provider 파일 자체를 분리하되, capability probe 는 always-local 이므로
 * 실제 [AiCoreProviderProbe] 는 호출하지 않고 상수 결과만 노출한다.
 */
class QaFakeProviderV2 : GeminiNanoRuntimeProviderV2 {

    override val mode: GeminiNanoConnectorMode = GeminiNanoConnectorMode.QaFake

    override val providerContract: GeminiNanoProviderContractV2 = GeminiNanoProviderContractV2(
        providerLabel = "QA fake provider resolver (v6.4 split)",
        readinessPhase = "provider_ready",
        bindingTarget = "deterministic local provider",
        capabilityProbe = "always local-ready (no OEM dependency)",
        entitlementGate = "none",
        todoChecklist = listOf(
            "fake provider parity with real provider diagnostics",
            "trace metadata regression check",
            "demo build without vendor SDK"
        )
    )

    private val factory: GeminiNanoSessionFactoryV2 by lazy { QaFakeSessionFactoryV2() }

    override fun diagnostics(): GeminiNanoProviderDiagnosticV2 = GeminiNanoProviderDiagnosticV2(
        available = true,
        statusLabel = "QA provider 준비됨",
        detail = "v6.4에서 provider / session factory 계약을 물리 파일로 분리했지만 QA fake 경로는 항상 로컬 준비 상태를 유지합니다.",
        providerSummary = "${providerContract.providerLabel} · ${providerContract.capabilityProbe}",
        probeSummary = "local-ready",
        entitlementSummary = "not required"
    )

    override fun sessionFactory(): GeminiNanoSessionFactoryV2 = factory
}
