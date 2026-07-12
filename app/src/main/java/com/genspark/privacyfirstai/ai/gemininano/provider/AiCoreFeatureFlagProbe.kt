package com.genspark.privacyfirstai.ai.gemininano.provider

import com.genspark.privacyfirstai.ai.gemininano.AiCorePromptDownloadController

/**
 * AICore feature-flag / model-availability probe.
 *
 * Prompt API download/retry UX와 동일한 상태 소스를 재사용한다.
 */
data class AiCoreFeatureFlagProbeResult(
    val featureStatus: Int,
    val statusKey: String,
    val availableForInference: Boolean,
    val statusLabel: String,
    val summary: String,
    val progressLabel: String?,
    val lastError: String?,
    val checklist: List<String>
)

class AiCoreFeatureFlagProbe(
    private val promptDownloadController: AiCorePromptDownloadController = AiCorePromptDownloadController()
) {

    fun probe(): AiCoreFeatureFlagProbeResult {
        val state = promptDownloadController.refreshStatus()
        return AiCoreFeatureFlagProbeResult(
            featureStatus = state.featureStatus,
            statusKey = state.statusKey,
            availableForInference = state.availableForInference,
            statusLabel = state.statusLabel,
            summary = state.summary,
            progressLabel = state.progressLabel,
            lastError = state.lastError,
            checklist = FEATURE_FLAG_CHECKLIST
        )
    }

    companion object {
        private val FEATURE_FLAG_CHECKLIST = listOf(
            "check Prompt API FeatureStatus through ML Kit",
            "surface AVAILABLE/DOWNLOADABLE/DOWNLOADING to provider diagnostics",
            "offer in-app download/retry action and status refresh for DOWNLOADABLE",
            "let session factory open a real session only when AVAILABLE"
        )
    }
}
