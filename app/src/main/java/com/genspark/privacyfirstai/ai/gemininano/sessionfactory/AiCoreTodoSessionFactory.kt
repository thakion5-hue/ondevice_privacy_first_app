package com.genspark.privacyfirstai.ai.gemininano.sessionfactory

import com.genspark.privacyfirstai.ai.GeminiNanoInferenceRequest
import com.genspark.privacyfirstai.ai.GeminiNanoRuntimeInference
import com.genspark.privacyfirstai.ai.gemininano.trace.GeminiNanoInferenceTraceRecorder
import com.genspark.privacyfirstai.ai.gemininano.trace.buildInputPreview
import com.genspark.privacyfirstai.ai.gemininano.trace.buildTraceFromResult
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode
import com.genspark.privacyfirstai.domain.model.SpamModelResult

class AiCoreTodoSessionFactoryV2(
    private val promptBridge: AiCorePromptRuntimeBridge = AiCorePromptRuntimeBridge()
) : GeminiNanoSessionFactoryV2 {

    override val factoryContract: GeminiNanoSessionFactoryContractV2 = GeminiNanoSessionFactoryContractV2(
        factoryLabel = "AICore ML Kit Prompt session factory (v6.6 real bridge)",
        readinessPhase = "mlkit_prompt_bridge_started",
        sessionType = "Generation.getClient() + GenerativeModelFutures",
        inferenceEntryPoint = "generateContent(text classification prompt) -> normalize safe|caution|fraud",
        outputExpectation = "single label: safe | caution | fraud",
        fallbackPolicy = "Prompt API unavailable / model not downloaded / inference failure -> bundled TFLite fallback",
        todoChecklist = listOf(
            "surface manual download / retry UX while keeping safe TFLite fallback",
            "upgrade synthetic score/logits to richer confidence when SDK exposes them",
            "expand prompt regression coverage against QA fake trace corpus",
            "reuse GeminiNanoInferenceTrace schema for real AICore path"
        )
    )

    override fun diagnostics(): GeminiNanoSessionFactoryDiagnosticV2 {
        val snapshot = promptBridge.featureSnapshot()
        val available = snapshot.availableForInference
        val statusLabel = when {
            snapshot.statusKey == "available" -> "Prompt session 준비됨"
            snapshot.statusKey == "downloadable" && snapshot.lastError != null -> "Prompt 모델 재시도 가능"
            snapshot.statusKey == "downloadable" -> "Prompt 모델 다운로드 가능"
            snapshot.statusKey == "downloading" -> "Prompt 모델 다운로드 중"
            snapshot.statusKey == "status_probe_error" -> "Prompt 상태 확인 실패"
            else -> "Prompt 세션 미가용"
        }
        val detail = buildString {
            append("v6.6에서는 AiCoreTodoSessionFactoryV2가 실제 ML Kit Prompt API 바인딩을 사용합니다. ")
            append(snapshot.summary)
            snapshot.progressLabel?.let {
                append(" / 진행률 ")
                append(it)
            }
            snapshot.lastError?.let {
                append(" / 최근 오류 ")
                append(it)
            }
            append(". AVAILABLE 상태일 때만 실 세션을 열고, 그 외에는 기존처럼 기본 TFLite로 폴백합니다.")
        }
        return GeminiNanoSessionFactoryDiagnosticV2(
            available = available,
            statusLabel = statusLabel,
            detail = detail,
            factorySummary = "${factoryContract.factoryLabel} · ${factoryContract.inferenceEntryPoint}",
            traceInstrumentation = if (available) {
                "trace_schema_reused_for_real_prompt_path"
            } else {
                "trace_schema_ready_waiting_for_prompt_availability"
            }
        )
    }

    override fun openSession(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeSessionV2? {
        val snapshot = promptBridge.featureSnapshot()
        if (!snapshot.availableForInference) return null
        return AiCorePromptSessionV2(promptBridge)
    }
}

internal class AiCorePromptSessionV2(
    private val promptBridge: AiCorePromptRuntimeBridge
) : GeminiNanoRuntimeSessionV2 {

    override val sessionLabel: String = "aicore_mlkit_prompt_session_v1"

    override fun infer(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference? {
        val startedAt = System.currentTimeMillis()
        val execution = promptBridge.generateLabel(request) ?: return null
        val completedAt = System.currentTimeMillis()

        val result = SpamModelResult(
            label = execution.label,
            score = execution.score,
            logits = execution.logits
        )

        val trace = buildTraceFromResult(
            requestId = request.requestId,
            sessionLabel = sessionLabel,
            connectorMode = GeminiNanoConnectorMode.AiCoreTodo.storageKey,
            providerLabel = "AiCoreTodoProviderV2",
            factoryLabel = "AiCoreTodoSessionFactoryV2",
            rawInput = request.text,
            hitSignals = listOf(
                "mlkit_prompt_api",
                "response_preview=${buildInputPreview(execution.rawResponse, 24)}",
                "finish_reason=${execution.finishReason ?: -1}"
            ),
            linkDetected = false,
            decisionRule = execution.parseRule,
            result = result,
            startedAtMillis = startedAt,
            completedAtMillis = completedAt
        )
        GeminiNanoInferenceTraceRecorder.record(trace)

        return GeminiNanoRuntimeInference(
            result = result,
            traceLabel = sessionLabel,
            engineMessage = "AICore ML Kit Prompt requestId=${request.requestId} → ${result.label} (rule=${execution.parseRule}, finishReason=${execution.finishReason ?: -1}, latencyMs=${trace.latencyMillis})"
        )
    }
}
