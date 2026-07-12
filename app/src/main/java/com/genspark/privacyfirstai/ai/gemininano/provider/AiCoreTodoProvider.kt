package com.genspark.privacyfirstai.ai.gemininano.provider

import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.AiCoreTodoSessionFactoryV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryV2
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode

/**
 * AICore provider가 OS probe + Prompt feature probe + entitlement gate를 조합한다.
 */
class AiCoreTodoProviderV2(
    private val probe: AiCoreProviderProbe = AiCoreProviderProbe(),
    private val featureFlagProbe: AiCoreFeatureFlagProbe = AiCoreFeatureFlagProbe(),
    private val entitlementGate: AiCoreEntitlementGate = AiCoreEntitlementGate()
) : GeminiNanoRuntimeProviderV2 {

    override val mode: GeminiNanoConnectorMode = GeminiNanoConnectorMode.AiCoreTodo

    override val providerContract: GeminiNanoProviderContractV2 = GeminiNanoProviderContractV2(
        providerLabel = "AICore provider resolver (v6.6 prompt bridge)",
        readinessPhase = "provider_probe_plus_prompt_feature_gate",
        bindingTarget = "Android AICore / Gemini Nano via ML Kit Prompt API",
        capabilityProbe = "AiCoreProviderProbe: OS baseline + OEM identity",
        entitlementGate = "AiCoreFeatureFlagProbe + AiCoreEntitlementGate: Prompt availability / download state",
        todoChecklist = listOf(
            "AiCoreProviderProbe 실 SDK capability 호출로 확장",
            "DOWNLOADABLE / DOWNLOADING 상태에서 UX copy 와 설정 진입점 다듬기",
            "probe -> feature status -> entitlement -> session factory handoff 계약 고정"
        )
    )

    private val factory: GeminiNanoSessionFactoryV2 by lazy { AiCoreTodoSessionFactoryV2() }

    override fun diagnostics(): GeminiNanoProviderDiagnosticV2 {
        val probeResult = probe.probe()
        val featureFlag = featureFlagProbe.probe()
        val entitlement = entitlementGate.evaluate(probeResult, featureFlag)
        val available = probeResult.meetsOsBaseline && entitlement.entitled
        val statusLabel = when {
            available -> "AICore Prompt 준비됨"
            featureFlag.statusKey == "downloadable" && featureFlag.lastError != null -> "AICore 모델 재시도 필요"
            featureFlag.statusKey == "downloadable" -> "AICore 모델 다운로드 필요"
            featureFlag.statusKey == "downloading" -> "AICore 모델 다운로드 중"
            featureFlag.statusKey == "status_probe_error" -> "AICore 상태 확인 실패"
            probeResult.meetsOsBaseline -> "AICore entitlement 대기"
            else -> "AICore OS 조건 미달"
        }
        val detail = buildString {
            append("provider probe / Prompt feature gate를 파일 단위로 분리한 v6.6 계약입니다. ")
            append(probeResult.summary)
            append(" / ")
            append(featureFlag.summary)
            featureFlag.progressLabel?.let {
                append(" / 진행률 ")
                append(it)
            }
            featureFlag.lastError?.let {
                append(" / 최근 오류 ")
                append(it)
            }
            append(" / ")
            append(entitlement.summary)
        }
        return GeminiNanoProviderDiagnosticV2(
            available = available,
            statusLabel = statusLabel,
            detail = detail,
            providerSummary = "${providerContract.providerLabel} · ${providerContract.bindingTarget}",
            probeSummary = "${probeResult.summary} / ${featureFlag.summary}",
            entitlementSummary = entitlement.summary
        )
    }

    override fun sessionFactory(): GeminiNanoSessionFactoryV2 = factory
}
