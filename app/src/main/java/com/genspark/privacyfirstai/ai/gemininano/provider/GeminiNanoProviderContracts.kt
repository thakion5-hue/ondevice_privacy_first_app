package com.genspark.privacyfirstai.ai.gemininano.provider

import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryV2
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode

/**
 * v6.4에서 provider layer 를 파일/패키지 단위로 물리 분리한다.
 *
 * 이 파일은 provider layer 의 계약 인터페이스와 데이터 클래스만 담는다.
 * 실제 구현은 각 mode 마다 별도 파일로 분리한다:
 *  - [AiCoreTodoProviderV2]
 *  - [QaFakeProviderV2]
 *  - [LegacyStubProviderV2]
 *
 * 그리고 provider 내부는 다시 두 개의 sub-layer 로 명시적으로 나뉜다:
 *  - [AiCoreProviderProbe]         : OEM capability probe 전용
 *  - [AiCoreEntitlementGate]       : entitlement / feature-flag 전용
 */

data class GeminiNanoProviderContractV2(
    val providerLabel: String,
    val readinessPhase: String,
    val bindingTarget: String,
    val capabilityProbe: String,
    val entitlementGate: String,
    val todoChecklist: List<String>
)

data class GeminiNanoProviderDiagnosticV2(
    val available: Boolean,
    val statusLabel: String,
    val detail: String,
    val providerSummary: String,
    val probeSummary: String,
    val entitlementSummary: String
)

interface GeminiNanoRuntimeProviderV2 {
    val mode: GeminiNanoConnectorMode
    val providerContract: GeminiNanoProviderContractV2

    fun diagnostics(): GeminiNanoProviderDiagnosticV2

    fun sessionFactory(): GeminiNanoSessionFactoryV2
}
