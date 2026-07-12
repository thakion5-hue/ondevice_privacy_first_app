package com.genspark.privacyfirstai.ai.gemininano.sessionfactory

import com.genspark.privacyfirstai.ai.GeminiNanoInferenceRequest
import com.genspark.privacyfirstai.ai.GeminiNanoRuntimeInference
import com.genspark.privacyfirstai.ai.gemininano.trace.GeminiNanoInferenceTraceRecorder
import com.genspark.privacyfirstai.ai.gemininano.trace.buildTraceFromResult
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode
import com.genspark.privacyfirstai.domain.model.SpamModelResult

/**
 * v6.4에서 QA fake session factory 를 별도 파일로 분리하고,
 * 매 추론마다 [com.genspark.privacyfirstai.ai.gemininano.trace.GeminiNanoInferenceTrace]
 * 입력/출력 메타데이터를 [GeminiNanoInferenceTraceRecorder] 에 남긴다.
 */
class QaFakeSessionFactoryV2 : GeminiNanoSessionFactoryV2 {

    override val factoryContract: GeminiNanoSessionFactoryContractV2 = GeminiNanoSessionFactoryContractV2(
        factoryLabel = "QA deterministic keyword factory (v6.4 split + trace)",
        readinessPhase = "factory_ready",
        sessionType = "create deterministic keyword session",
        inferenceEntryPoint = "score text locally without SDK dependency + emit trace",
        outputExpectation = "single label: safe | caution | fraud",
        fallbackPolicy = "fake path stays local and can still be bypassed by runtime selection",
        todoChecklist = listOf(
            "trace ring buffer capacity tuning per QA profile",
            "hit signal taxonomy extension",
            "cross-check fake trace vs real AICore trace schema"
        )
    )

    override fun diagnostics(): GeminiNanoSessionFactoryDiagnosticV2 = GeminiNanoSessionFactoryDiagnosticV2(
        available = true,
        statusLabel = "Fake factory 준비됨",
        detail = "session factory 가 즉시 로컬 세션을 열고, 매 추론마다 입력/출력 trace 메타데이터를 기록합니다. 네트워크나 OEM SDK에 의존하지 않습니다.",
        factorySummary = "${factoryContract.factoryLabel} · ${factoryContract.inferenceEntryPoint}",
        traceInstrumentation = "trace_ring_buffer_enabled"
    )

    override fun openSession(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeSessionV2 =
        QaFakeSessionV2()
}

private val QA_FAKE_FRAUD_SIGNALS = listOf(
    "지금 클릭", "무료 지급", "100% 수익", "투자 보장", "원금 보장",
    "지원금", "계정 정지", "본인 확인", "택배 오류", "카드 정지",
    "오픈채팅", "텔레그램", "whatsapp", "dm"
)

private val QA_FAKE_SAFE_SIGNALS = listOf("회의", "안건", "일정", "문서", "고마워", "내일")
private val QA_FAKE_LINK_REGEX = Regex("""https?://|bit\.ly|tinyurl|cutt\.ly|t\.co|open\.kakao""")

internal class QaFakeSessionV2 : GeminiNanoRuntimeSessionV2 {

    override val sessionLabel: String = "qa_fake_keyword_session_v2"

    override fun infer(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference {
        val startedAt = System.currentTimeMillis()
        val raw = request.text
        val normalized = raw.lowercase()
        val fraudHits = QA_FAKE_FRAUD_SIGNALS.filter { normalized.contains(it) }
        val safeHits = QA_FAKE_SAFE_SIGNALS.filter { normalized.contains(it) }
        val hasLinkSignals = QA_FAKE_LINK_REGEX.containsMatchIn(normalized)

        val decisionRule: String
        val result: SpamModelResult
        val hitSignals: List<String>

        when {
            fraudHits.isNotEmpty() || (hasLinkSignals && normalized.contains("즉시")) -> {
                decisionRule = if (fraudHits.isNotEmpty()) "fraud_keyword_match" else "link_with_urgent_cue"
                hitSignals = fraudHits + if (hasLinkSignals) listOf("link_signal") else emptyList()
                result = SpamModelResult(
                    label = "fraud",
                    score = 0.93f,
                    logits = listOf(0.03f, 0.04f, 0.93f)
                )
            }
            hasLinkSignals || normalized.contains("확인") || normalized.contains("인증") -> {
                decisionRule = when {
                    hasLinkSignals -> "link_signal_only"
                    normalized.contains("인증") -> "verification_cue"
                    else -> "confirmation_cue"
                }
                hitSignals = buildList {
                    if (hasLinkSignals) add("link_signal")
                    if (normalized.contains("확인")) add("confirmation_cue")
                    if (normalized.contains("인증")) add("verification_cue")
                }
                result = SpamModelResult(
                    label = "caution",
                    score = 0.76f,
                    logits = listOf(0.08f, 0.76f, 0.16f)
                )
            }
            safeHits.isNotEmpty() -> {
                decisionRule = "safe_keyword_match"
                hitSignals = safeHits
                result = SpamModelResult(
                    label = "safe",
                    score = 0.88f,
                    logits = listOf(0.88f, 0.08f, 0.04f)
                )
            }
            else -> {
                decisionRule = "default_safe_fallback"
                hitSignals = emptyList()
                result = SpamModelResult(
                    label = "safe",
                    score = 0.67f,
                    logits = listOf(0.67f, 0.22f, 0.11f)
                )
            }
        }

        val completedAt = System.currentTimeMillis()

        val trace = buildTraceFromResult(
            requestId = request.requestId,
            sessionLabel = sessionLabel,
            connectorMode = GeminiNanoConnectorMode.QaFake.storageKey,
            providerLabel = "QaFakeProviderV2",
            factoryLabel = "QaFakeSessionFactoryV2",
            rawInput = raw,
            hitSignals = hitSignals,
            linkDetected = hasLinkSignals,
            decisionRule = decisionRule,
            result = result,
            startedAtMillis = startedAt,
            completedAtMillis = completedAt
        )
        GeminiNanoInferenceTraceRecorder.record(trace)

        return GeminiNanoRuntimeInference(
            result = result,
            traceLabel = sessionLabel,
            engineMessage = "QA fake session v2 requestId=${request.requestId} → ${result.label} (rule=${decisionRule}, hits=${hitSignals.size}, latencyMs=${trace.latencyMillis})"
        )
    }
}
