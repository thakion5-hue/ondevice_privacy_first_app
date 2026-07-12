package com.genspark.privacyfirstai.ai

import com.genspark.privacyfirstai.domain.model.SpamFilterMode
import com.genspark.privacyfirstai.domain.model.ThreatScan
import com.genspark.privacyfirstai.domain.model.VendorRuntimeOption

class LocalThreatGuard(
    private val runtimeRegistry: OnDeviceRuntimeRegistry
) {
    fun scan(
        text: String,
        mode: SpamFilterMode = SpamFilterMode.Hybrid,
        preferredRuntime: VendorRuntimeOption = VendorRuntimeOption.TfliteBuiltin
    ): ThreatScan? {
        val normalized = text.lowercase()
        val signals = mutableListOf<String>()
        var riskScore = 0

        val urgentSignals = listOf("지금 클릭", "즉시", "긴급", "마감", "바로 확인")
        val rewardSignals = listOf("무료 지급", "당첨", "100% 수익", "투자 보장", "고수익", "지원금")
        val impersonationSignals = listOf("계정 정지", "본인 확인", "정부 지원", "택배 오류", "카드 정지")
        val deepfakeSignals = listOf("ai 생성", "합성 영상", "딥페이크", "voice clone", "face swap")

        if (urgentSignals.any { normalized.contains(it) }) {
            signals += "긴급 행동 유도 표현"
            riskScore += 2
        }
        if (rewardSignals.any { normalized.contains(it) }) {
            signals += "비정상적 보상·수익 약속"
            riskScore += 4
        }
        if (impersonationSignals.any { normalized.contains(it) }) {
            signals += "기관/서비스 사칭 가능성"
            riskScore += 3
        }
        if (deepfakeSignals.any { normalized.contains(it) }) {
            signals += "합성 미디어 관련 표현"
            riskScore += 2
        }
        if (Regex("""https?://|bit\.ly|tinyurl|cutt\.ly|t\.co|open\.kakao""").containsMatchIn(normalized)) {
            signals += "단축/외부 링크 포함"
            riskScore += 2
        }
        if (Regex("""(오픈채팅|텔레그램|whatsapp|dm)""").containsMatchIn(normalized)) {
            signals += "외부 대화 채널 유도"
            riskScore += 2
        }

        val runtimeResult = if (mode == SpamFilterMode.HeuristicOnly) {
            null
        } else {
            runCatching { runtimeRegistry.classify(text, preferredRuntime) }.getOrNull()
        }
        val modelResult = runtimeResult?.result
        val runtimeLabel = runtimeResult?.runtimeLabel ?: "온디바이스 런타임"

        var modelSource = when (mode) {
            SpamFilterMode.HeuristicOnly -> "heuristic"
            SpamFilterMode.TflitePreferred -> runtimeResult?.modelSource ?: "heuristic_fallback"
            SpamFilterMode.Hybrid -> runtimeResult?.modelSource ?: "heuristic"
        }

        runtimeResult?.fallbackReason?.let {
            signals += "벤더 런타임 폴백: $it"
        }

        when (modelResult?.label) {
            "fraud" -> {
                signals += "${runtimeLabel} 모델이 고위험 패턴을 분류"
                riskScore += when {
                    mode == SpamFilterMode.TflitePreferred -> 5
                    modelResult.score >= 0.75f -> 4
                    else -> 3
                }
                if (mode == SpamFilterMode.TflitePreferred && riskScore < 6) riskScore = 6
            }
            "caution" -> {
                signals += "${runtimeLabel} 모델이 주의 문맥으로 분류"
                riskScore += if (mode == SpamFilterMode.TflitePreferred) 3 else 2
                if (mode == SpamFilterMode.TflitePreferred && riskScore < 3) riskScore = 3
            }
            "safe" -> {
                if (mode != SpamFilterMode.HeuristicOnly) {
                    signals += "${runtimeLabel} 모델이 비교적 안전한 문맥으로 분류"
                }
            }
        }

        if (riskScore >= 6) {
            return ThreatScan(
                label = "스팸/사기 가능성",
                severity = "높음",
                reason = signals.joinToString(" · ").ifBlank { "고위험 텍스트 패턴이 탐지되었습니다." },
                recommendedAction = "링크 클릭·앱 설치·송금 전 중단하고 발신자 신원을 먼저 확인하세요.",
                modelSource = modelSource
            )
        }

        if (riskScore >= 3) {
            return ThreatScan(
                label = "주의 필요",
                severity = "중간",
                reason = signals.joinToString(" · ").ifBlank { "의심 신호가 일부 탐지되었습니다." },
                recommendedAction = "원문 출처와 계정 이력을 확인하고, 개인정보 입력은 보류하세요.",
                modelSource = modelSource
            )
        }

        return if (modelResult?.label == "safe") {
            ThreatScan(
                label = "안전 가능성 높음",
                severity = "낮음",
                reason = "선택한 엔진에서 유의미한 고위험 패턴이 낮게 평가되었습니다.",
                recommendedAction = "그래도 링크·계정 인증 요청은 한 번 더 출처를 확인하세요.",
                modelSource = modelSource
            )
        } else {
            null
        }
    }
}
