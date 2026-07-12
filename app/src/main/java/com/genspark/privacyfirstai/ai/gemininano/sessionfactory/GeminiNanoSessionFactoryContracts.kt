package com.genspark.privacyfirstai.ai.gemininano.sessionfactory

import com.genspark.privacyfirstai.ai.GeminiNanoInferenceRequest
import com.genspark.privacyfirstai.ai.GeminiNanoRuntimeInference

/**
 * v6.4에서 session factory layer 를 파일/패키지 단위로 물리 분리한다.
 *
 * 이 파일에는 factory / session 계약만 담고, 각 mode 별 구현은:
 *  - [AiCoreTodoSessionFactoryV2]
 *  - [QaFakeSessionFactoryV2]  (trace 메타데이터 부착 지점)
 *  - [LegacyStubSessionFactoryV2]
 * 로 파일을 분리했다.
 *
 * v6.3 대비 변화:
 *  - session 이 만든 결과에 GeminiNanoInferenceTrace 를 부착할 수 있도록
 *    GeminiNanoRuntimeInference 를 그대로 재사용 (연결 지점은 [GeminiNanoRuntimeInference.engineMessage]
 *    가 아니라 별도 recorder 로 흐른다).
 */

data class GeminiNanoSessionFactoryContractV2(
    val factoryLabel: String,
    val readinessPhase: String,
    val sessionType: String,
    val inferenceEntryPoint: String,
    val outputExpectation: String,
    val fallbackPolicy: String,
    val todoChecklist: List<String>
)

data class GeminiNanoSessionFactoryDiagnosticV2(
    val available: Boolean,
    val statusLabel: String,
    val detail: String,
    val factorySummary: String,
    val traceInstrumentation: String
)

interface GeminiNanoSessionFactoryV2 {
    val factoryContract: GeminiNanoSessionFactoryContractV2

    fun diagnostics(): GeminiNanoSessionFactoryDiagnosticV2

    fun openSession(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeSessionV2?
}

interface GeminiNanoRuntimeSessionV2 {
    val sessionLabel: String

    fun infer(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeInference?
}
