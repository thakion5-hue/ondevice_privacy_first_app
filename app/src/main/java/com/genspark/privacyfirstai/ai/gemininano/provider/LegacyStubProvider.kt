package com.genspark.privacyfirstai.ai.gemininano.provider

import android.os.Build
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.GeminiNanoSessionFactoryV2
import com.genspark.privacyfirstai.ai.gemininano.sessionfactory.LegacyStubSessionFactoryV2
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode

class LegacyStubProviderV2 : GeminiNanoRuntimeProviderV2 {

    override val mode: GeminiNanoConnectorMode = GeminiNanoConnectorMode.LegacyStub

    override val providerContract: GeminiNanoProviderContractV2 = GeminiNanoProviderContractV2(
        providerLabel = "legacy provider boundary (v6.4 split)",
        readinessPhase = "legacy_provider_stub",
        bindingTarget = "legacy connector boundary",
        capabilityProbe = "not implemented",
        entitlementGate = "not implemented",
        todoChecklist = listOf("v6.1 provider boundary compatibility")
    )

    private val factory: GeminiNanoSessionFactoryV2 by lazy { LegacyStubSessionFactoryV2() }

    override fun diagnostics(): GeminiNanoProviderDiagnosticV2 {
        val likelyCapable = Build.VERSION.SDK_INT >= 34
        return GeminiNanoProviderDiagnosticV2(
            available = false,
            statusLabel = if (likelyCapable) "Legacy provider" else "Provider 조건 미달",
            detail = if (likelyCapable) {
                "v6.4에서도 legacy 경로는 최소 경계만 유지하고 실제 provider probe / entitlement 를 수행하지 않습니다."
            } else {
                "OS 조건 미달로 legacy provider 도 활성화하지 않습니다. TFLite 폴백만 유지됩니다."
            },
            providerSummary = "${providerContract.providerLabel} · ${providerContract.bindingTarget}",
            probeSummary = "not implemented",
            entitlementSummary = "not implemented"
        )
    }

    override fun sessionFactory(): GeminiNanoSessionFactoryV2 = factory
}
