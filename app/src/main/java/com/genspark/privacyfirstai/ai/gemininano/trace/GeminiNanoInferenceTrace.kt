package com.genspark.privacyfirstai.ai.gemininano.trace

import com.genspark.privacyfirstai.domain.model.SpamModelResult
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * v6.4에서 fake / connector 세션이 매 추론마다 남기는 입력/출력 trace 메타데이터.
 *
 * 목적:
 *  - QA 재현성 확보: 어떤 입력이 어떤 규칙에 걸려서 어떤 label 로 갔는지 추적
 *  - LogExporter 가 최근 세션 trace 를 함께 export 해서 오프라인 디버깅 가능
 *  - 실제 AICore 바인딩 이후에도 동일한 trace 스키마를 재사용해 지표 회귀 비교
 *
 * 여기서는 personally identifiable 원문을 통째로 저장하지 않고 preview / 통계 위주로만 남긴다.
 */
data class GeminiNanoInferenceTrace(
    val traceId: Long,
    val requestId: String,
    val sessionLabel: String,
    val connectorMode: String,
    val providerLabel: String,
    val factoryLabel: String,

    val inputPreview: String,
    val inputCharLength: Int,
    val inputTokenEstimate: Int,
    val hitSignals: List<String>,
    val linkDetected: Boolean,
    val decisionRule: String,

    val outputLabel: String,
    val outputScore: Float,
    val outputLogits: List<Float>,
    val outputConfidenceBand: String,

    val startedAtMillis: Long,
    val completedAtMillis: Long,
    val latencyMillis: Long,
    val fallbackReason: String? = null
)

private const val PREVIEW_MAX_CHARS = 60

fun buildInputPreview(text: String, maxChars: Int = PREVIEW_MAX_CHARS): String {
    val trimmed = text.trim()
    if (trimmed.length <= maxChars) return trimmed
    return trimmed.substring(0, maxChars) + "…"
}

fun estimateInputTokens(text: String): Int {
    // 로컬에서 실제 tokenizer 없이 대략의 token 수를 추정한다.
    // 공백 기준 단어 수 + CJK 문자당 1 tokens 정도의 heuristic.
    if (text.isEmpty()) return 0
    val whitespaceTokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val cjkTokens = text.count { it.code in 0x3400..0x9FFF }
    return maxOf(1, whitespaceTokens + (cjkTokens / 2))
}

fun classifyConfidenceBand(score: Float): String = when {
    score >= 0.85f -> "high"
    score >= 0.65f -> "medium"
    else -> "low"
}

/**
 * 최근 fake / 실 세션 trace 를 메모리 상에 최대 [capacity] 건 유지하는 ring buffer.
 * export 시점에 스냅샷을 뜬다. process 단위 in-memory 이므로 privacy-first 정책과 호환된다.
 */
object GeminiNanoInferenceTraceRecorder {
    private const val DEFAULT_CAPACITY = 20

    private val counter = AtomicLong(0)
    private val buffer = ArrayDeque<GeminiNanoInferenceTrace>(DEFAULT_CAPACITY)
    private var capacity: Int = DEFAULT_CAPACITY

    @Synchronized
    fun nextTraceId(): Long = counter.incrementAndGet()

    @Synchronized
    fun record(trace: GeminiNanoInferenceTrace) {
        if (buffer.size >= capacity) {
            buffer.pollFirst()
        }
        buffer.offerLast(trace)
    }

    @Synchronized
    fun snapshot(): List<GeminiNanoInferenceTrace> = buffer.toList()

    @Synchronized
    fun clear() {
        buffer.clear()
    }

    @Synchronized
    fun setCapacity(newCapacity: Int) {
        require(newCapacity > 0) { "capacity must be > 0" }
        capacity = newCapacity
        while (buffer.size > capacity) {
            buffer.pollFirst()
        }
    }
}

data class GeminiNanoInferenceTraceSummary(
    val totalTracesRecorded: Long,
    val bufferedTraces: Int,
    val bufferCapacity: Int,
    val lastTraceLatencyMillis: Long?,
    val lastTraceLabel: String?
)

fun geminiNanoInferenceTraceSummary(): GeminiNanoInferenceTraceSummary {
    val snap = GeminiNanoInferenceTraceRecorder.snapshot()
    val last = snap.lastOrNull()
    return GeminiNanoInferenceTraceSummary(
        totalTracesRecorded = snap.lastOrNull()?.traceId ?: 0L,
        bufferedTraces = snap.size,
        bufferCapacity = 20,
        lastTraceLatencyMillis = last?.latencyMillis,
        lastTraceLabel = last?.outputLabel
    )
}

/** Convenience factory to attach a trace to a [SpamModelResult]. */
fun buildTraceFromResult(
    requestId: String,
    sessionLabel: String,
    connectorMode: String,
    providerLabel: String,
    factoryLabel: String,
    rawInput: String,
    hitSignals: List<String>,
    linkDetected: Boolean,
    decisionRule: String,
    result: SpamModelResult,
    startedAtMillis: Long,
    completedAtMillis: Long,
    fallbackReason: String? = null
): GeminiNanoInferenceTrace = GeminiNanoInferenceTrace(
    traceId = GeminiNanoInferenceTraceRecorder.nextTraceId(),
    requestId = requestId,
    sessionLabel = sessionLabel,
    connectorMode = connectorMode,
    providerLabel = providerLabel,
    factoryLabel = factoryLabel,
    inputPreview = buildInputPreview(rawInput),
    inputCharLength = rawInput.length,
    inputTokenEstimate = estimateInputTokens(rawInput),
    hitSignals = hitSignals,
    linkDetected = linkDetected,
    decisionRule = decisionRule,
    outputLabel = result.label,
    outputScore = result.score,
    outputLogits = result.logits,
    outputConfidenceBand = classifyConfidenceBand(result.score),
    startedAtMillis = startedAtMillis,
    completedAtMillis = completedAtMillis,
    latencyMillis = completedAtMillis - startedAtMillis,
    fallbackReason = fallbackReason
)
