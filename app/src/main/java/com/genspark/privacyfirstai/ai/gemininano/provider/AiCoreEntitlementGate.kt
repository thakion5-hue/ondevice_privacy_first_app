package com.genspark.privacyfirstai.ai.gemininano.provider

/**
 * entitlement 판정에 실제 Prompt API feature availability를 반영한다.
 *
 * 책임:
 *  - probe 결과 + feature-flag probe 결과를 함께 받아 entitlement 판정
 *  - AVAILABLE 이면 session factory 가 실 세션을 열 수 있게 허용
 *  - DOWNLOADABLE / DOWNLOADING / UNAVAILABLE 은 명시적으로 요약해 UI/로그로 노출
 */
data class AiCoreEntitlementDecision(
    val entitled: Boolean,
    val reason: String,
    val summary: String,
    val checklist: List<String>
)

class AiCoreEntitlementGate {

    fun evaluate(
        probe: AiCoreProviderProbeResult,
        featureFlag: AiCoreFeatureFlagProbeResult
    ): AiCoreEntitlementDecision {
        if (!probe.meetsOsBaseline) {
            return AiCoreEntitlementDecision(
                entitled = false,
                reason = "os_baseline_not_met",
                summary = "OS 조건 불충족으로 entitlement 자체를 평가하지 않습니다.",
                checklist = ENTITLEMENT_TODO
            )
        }

        return when (featureFlag.statusKey) {
            "available" -> AiCoreEntitlementDecision(
                entitled = true,
                reason = "mlkit_prompt_available",
                summary = "OS 조건과 AICore Prompt AVAILABLE 상태가 모두 충족되어 entitlement 를 허용합니다.",
                checklist = ENTITLEMENT_TODO
            )

            "downloadable" -> AiCoreEntitlementDecision(
                entitled = false,
                reason = "aicore_model_download_required",
                summary = "OS 조건은 충족되지만 Gemini Nano 모델이 아직 다운로드되지 않아 entitlement 를 보류합니다.",
                checklist = ENTITLEMENT_TODO
            )

            "downloading" -> AiCoreEntitlementDecision(
                entitled = false,
                reason = "aicore_model_downloading",
                summary = "OS 조건은 충족되지만 Gemini Nano 모델이 아직 다운로드 중이라 entitlement 를 보류합니다.",
                checklist = ENTITLEMENT_TODO
            )

            "status_probe_error" -> AiCoreEntitlementDecision(
                entitled = false,
                reason = "feature_status_probe_error",
                summary = "Prompt API 상태 조회가 실패해 entitlement 를 보수적으로 막습니다.",
                checklist = ENTITLEMENT_TODO
            )

            else -> AiCoreEntitlementDecision(
                entitled = false,
                reason = "feature_unavailable",
                summary = "OS 조건은 충족되지만 이 기기에서는 현재 Prompt API/AICore 기능을 사용할 수 없습니다.",
                checklist = ENTITLEMENT_TODO
            )
        }
    }

    companion object {
        private val ENTITLEMENT_TODO = listOf(
            "resolve OEM entitlement allowlist only if Prompt API path is insufficient",
            "offer in-app download/retry UX for DOWNLOADABLE and DOWNLOADING",
            "map entitlement result to session factory readiness"
        )
    }
}
