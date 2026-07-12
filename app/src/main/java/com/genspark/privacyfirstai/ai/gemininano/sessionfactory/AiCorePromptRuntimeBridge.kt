package com.genspark.privacyfirstai.ai.gemininano.sessionfactory

import com.genspark.privacyfirstai.ai.GeminiNanoInferenceRequest
import com.genspark.privacyfirstai.ai.gemininano.AiCorePromptDownloadController
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures
import java.util.concurrent.TimeUnit

/**
 * 실제 AICore / Gemini Nano 바인딩을 담당하는 ML Kit Prompt bridge.
 *
 * 설계 원칙:
 *  - session factory 밖으로 SDK 의존성을 새지 않게 이 파일에 가둔다.
 *  - Prompt API 는 free-form 텍스트를 반환하므로 safe/caution/fraud 3-way 라벨로 정규화한다.
 *  - 네이티브 logits 는 제공되지 않으므로 score/logits 는 정규화된 라벨 confidence 로 합성한다.
 */
data class AiCorePromptFeatureSnapshot(
    val featureStatus: Int,
    val statusKey: String,
    val availableForInference: Boolean,
    val statusLabel: String,
    val summary: String,
    val progressLabel: String?,
    val lastError: String?
)

data class AiCorePromptExecutionResult(
    val label: String,
    val score: Float,
    val logits: List<Float>,
    val rawResponse: String,
    val finishReason: Int?,
    val parseRule: String
)

class AiCorePromptRuntimeBridge(
    private val promptDownloadController: AiCorePromptDownloadController = AiCorePromptDownloadController()
) {

    fun featureSnapshot(): AiCorePromptFeatureSnapshot {
        val state = promptDownloadController.refreshStatus()
        return AiCorePromptFeatureSnapshot(
            featureStatus = state.featureStatus,
            statusKey = state.statusKey,
            availableForInference = state.availableForInference,
            statusLabel = state.statusLabel,
            summary = state.summary,
            progressLabel = state.progressLabel,
            lastError = state.lastError
        )
    }

    fun generateLabel(request: GeminiNanoInferenceRequest): AiCorePromptExecutionResult? {
        val client = GenerativeModelFutures.from(Generation.getClient())
        val status = runCatching {
            client.checkStatus().get(STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.getOrElse { return null }
        if (status != FeatureStatus.AVAILABLE) return null

        val prompt = buildClassificationPrompt(request)
        val contentRequest = GenerateContentRequest.Builder(TextPart(prompt))
            .setTemperature(request.temperature)
            .setTopK(request.topK)
            .setCandidateCount(1)
            .setMaxOutputTokens(request.maxOutputTokens.coerceAtLeast(8))
            .build()

        val response = runCatching {
            client.generateContent(contentRequest).get(INFERENCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.getOrElse { return null }

        val candidate = response.candidates.firstOrNull() ?: return null
        val raw = candidate.text.trim()
        val normalized = normalizeLabel(raw) ?: return null
        return AiCorePromptExecutionResult(
            label = normalized.label,
            score = normalized.score,
            logits = normalized.logits,
            rawResponse = raw,
            finishReason = candidate.finishReason,
            parseRule = normalized.parseRule
        )
    }

    private fun buildClassificationPrompt(request: GeminiNanoInferenceRequest): String = """
        You are an on-device mobile security text classifier.
        Classify the USER_MESSAGE into exactly one lowercase label.
        Allowed labels: ${request.candidateLabels.joinToString(", ")}.
        Label policy:
        - safe: normal benign conversation or routine notification.
        - caution: suspicious verification, unusual link, or unclear risk that needs manual review.
        - fraud: phishing, impersonation, urgent money/credential request, fake investment, or malicious scam.
        Important rules:
        - Return exactly one label from the allowed labels.
        - Do not add punctuation, explanation, JSON, or extra words.
        USER_MESSAGE:
        <<<
        ${request.text}
        >>>
    """.trimIndent()

    private data class NormalizedLabel(
        val label: String,
        val score: Float,
        val logits: List<Float>,
        val parseRule: String
    )

    private fun normalizeLabel(rawResponse: String): NormalizedLabel? {
        val cleaned = rawResponse.trim().lowercase()
        if (cleaned.isEmpty()) return null

        val matchedLabel = when {
            EXACT_LABEL_REGEX.matches(cleaned) -> cleaned
            LABEL_TOKEN_REGEX["fraud"]!!.containsMatchIn(cleaned) -> "fraud"
            LABEL_TOKEN_REGEX["caution"]!!.containsMatchIn(cleaned) -> "caution"
            LABEL_TOKEN_REGEX["safe"]!!.containsMatchIn(cleaned) -> "safe"
            else -> null
        } ?: return null

        val parseRule = when {
            cleaned == matchedLabel -> "mlkit_prompt_label_exact"
            cleaned.startsWith(matchedLabel) -> "mlkit_prompt_label_prefixed"
            else -> "mlkit_prompt_label_embedded"
        }
        val score = when (parseRule) {
            "mlkit_prompt_label_exact" -> 0.84f
            "mlkit_prompt_label_prefixed" -> 0.76f
            else -> 0.68f
        }
        return NormalizedLabel(
            label = matchedLabel,
            score = score,
            logits = syntheticLogitsFor(matchedLabel, score),
            parseRule = parseRule
        )
    }

    private fun syntheticLogitsFor(label: String, score: Float): List<Float> {
        val bounded = score.coerceIn(0.34f, 0.95f)
        val remaining = (1f - bounded).coerceAtLeast(0.02f)
        val other = remaining / 2f
        return when (label) {
            "safe" -> listOf(bounded, other, other)
            "caution" -> listOf(other, bounded, other)
            "fraud" -> listOf(other, other, bounded)
            else -> listOf(0.34f, 0.33f, 0.33f)
        }
    }

    companion object {
        private const val STATUS_TIMEOUT_SECONDS = 3L
        private const val INFERENCE_TIMEOUT_SECONDS = 12L
        private val EXACT_LABEL_REGEX = Regex("^(safe|caution|fraud)$")
        private val LABEL_TOKEN_REGEX = mapOf(
            "safe" to Regex("\\bsafe\\b"),
            "caution" to Regex("\\bcaution\\b"),
            "fraud" to Regex("\\bfraud\\b")
        )
    }
}
