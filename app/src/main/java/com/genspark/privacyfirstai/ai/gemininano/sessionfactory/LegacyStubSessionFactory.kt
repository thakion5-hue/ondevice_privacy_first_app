package com.genspark.privacyfirstai.ai.gemininano.sessionfactory

import com.genspark.privacyfirstai.ai.GeminiNanoInferenceRequest

class LegacyStubSessionFactoryV2 : GeminiNanoSessionFactoryV2 {

    override val factoryContract: GeminiNanoSessionFactoryContractV2 = GeminiNanoSessionFactoryContractV2(
        factoryLabel = "legacy no-op factory (v6.4 split)",
        readinessPhase = "legacy_factory_stub",
        sessionType = "not implemented",
        inferenceEntryPoint = "not implemented",
        outputExpectation = "none",
        fallbackPolicy = "always fallback to bundled TFLite",
        todoChecklist = listOf("v6.1 no-op session factory compatibility")
    )

    override fun diagnostics(): GeminiNanoSessionFactoryDiagnosticV2 = GeminiNanoSessionFactoryDiagnosticV2(
        available = false,
        statusLabel = "No-op factory",
        detail = "v6.4에서도 legacy 경로는 session 을 만들지 않고 trace 도 남기지 않습니다.",
        factorySummary = "${factoryContract.factoryLabel} · ${factoryContract.fallbackPolicy}",
        traceInstrumentation = "disabled"
    )

    override fun openSession(request: GeminiNanoInferenceRequest): GeminiNanoRuntimeSessionV2? = null
}
