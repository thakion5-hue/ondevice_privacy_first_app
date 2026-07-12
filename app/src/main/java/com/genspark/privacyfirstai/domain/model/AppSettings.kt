package com.genspark.privacyfirstai.domain.model

enum class SpamFilterMode(
    val storageKey: String,
    val label: String,
    val description: String,
    val detail: String
) {
    Hybrid(
        storageKey = "hybrid",
        label = "하이브리드",
        description = "휴리스틱과 TFLite를 함께 반영해 가장 균형 있게 판정합니다.",
        detail = "실사용 권장 모드. TFLite가 이상 시에도 규칙 기반으로 폴백되어 안정성이 높습니다."
    ),
    HeuristicOnly(
        storageKey = "heuristic_only",
        label = "휴리스틱 전용",
        description = "모델 추론 없이 규칙 기반만 사용합니다. 저사양 기기 디버깅에 유용합니다.",
        detail = "TFLite 초기화가 실패하는 기기, 배터리를 최대한 아끼는 상황, 규칙 기반만으로 재현이 필요한 QA 시나리오에 적합합니다."
    ),
    TflitePreferred(
        storageKey = "tflite_preferred",
        label = "TFLite 우선",
        description = "선택한 온디바이스 런타임 결과를 우선 사용하고, 실패 시 휴리스틱으로 보강합니다.",
        detail = "TFLite 또는 향후 벤더 런타임 결과를 먼저 반영하고, 초기화 실패/미지원 시 안전 폴백합니다."
    );

    companion object {
        fun fromKey(value: String?): SpamFilterMode =
            entries.firstOrNull { it.storageKey == value } ?: Hybrid
    }
}

enum class VendorRuntimeOption(
    val storageKey: String,
    val label: String,
    val description: String,
    val detail: String
) {
    TfliteBuiltin(
        storageKey = "tflite_builtin",
        label = "기본 TFLite",
        description = "현재 앱에 실제 연결된 기본 온디바이스 텍스트 분류 런타임입니다.",
        detail = "지금 바로 동작 가능한 기본 경로입니다. 다른 벤더 런타임이 아직 연결되지 않았거나 기기 적합성을 모를 때 기본값으로 권장됩니다."
    ),
    GeminiNano(
        storageKey = "gemini_nano",
        label = "Gemini Nano 어댑터",
        description = "AICore / Gemini Nano 브리지용 스캐폴딩입니다.",
        detail = "0.6.6에서는 AICore provider probe 위에 ML Kit Prompt API feature status probe를 추가하고, AVAILABLE 상태일 때 AiCoreTodoSessionFactoryV2가 실제 Gemini Nano 세션을 엽니다. DOWNLOADABLE/DOWNLOADING 상태에서는 설정 화면에서 모델 다운로드·재시도와 상태 갱신 UX를 제공하고, 그 외에는 계속 기본 TFLite로 안전 폴백합니다."
    ),
    QualcommAiEngine(
        storageKey = "qualcomm_ai_engine",
        label = "Qualcomm AI Engine 어댑터",
        description = "Snapdragon 계열 벤더 런타임 연결용 스캐폴딩입니다.",
        detail = "런타임 선택/진단/폴백 체인이 준비되어 있습니다. 실제 QNN 연동 시 classify 구현부만 교체하면 됩니다."
    ),
    SamsungGauss(
        storageKey = "samsung_gauss",
        label = "Samsung Gauss 어댑터",
        description = "삼성 온디바이스 AI 런타임 연결용 스캐폴딩입니다.",
        detail = "선택 UI와 상태 진단, TFLite 폴백 경로를 먼저 열어둡니다. 실제 Gauss SDK 사용 시 어댑터 내부만 채우면 됩니다."
    );

    companion object {
        fun fromKey(value: String?): VendorRuntimeOption =
            entries.firstOrNull { it.storageKey == value } ?: TfliteBuiltin
    }
}

enum class GeminiNanoConnectorMode(
    val storageKey: String,
    val label: String,
    val description: String,
    val detail: String
) {
    AiCoreTodo(
        storageKey = "aicore_todo",
        label = "AICore TODO 계약",
        description = "실제 Gemini Nano / AICore 바인딩에 맞춘 계약만 구체화한 기본 모드입니다.",
        detail = "0.6.6에서는 provider layer가 AiCoreProviderProbe / AiCoreFeatureFlagProbe / AiCoreEntitlementGate / AiCoreTodoProviderV2로 이어지고, session factory는 ML Kit Prompt API를 통해 AVAILABLE 상태에서 실제 Gemini Nano 세션을 엽니다. DOWNLOADABLE/DOWNLOADING 상태는 설정 화면에서 다운로드·재시도와 자동 상태 갱신으로 노출되며, 출력은 safe/caution/fraud로 정규화되고 trace 스키마는 fake / 실 SDK 경로에서 동일하게 사용합니다."
    ),
    QaFake(
        storageKey = "qa_fake",
        label = "QA Fake Connector",
        description = "실제 SDK 없이 Gemini Nano 경로를 재현하기 위한 결정적 QA 모드입니다.",
        detail = "v6.4 부터 fake session 은 매 추론마다 GeminiNanoInferenceTrace(입력 preview, hit signal, decision rule, latency, confidence band)를 ring buffer 에 남기고, 로그 export 에 함께 담깁니다."
    ),
    LegacyStub(
        storageKey = "legacy_stub",
        label = "Legacy Stub",
        description = "v6.1 분리 경계만 유지하는 최소 스텁 모드입니다.",
        detail = "connector 경계만 확인하고 실제 추론은 수행하지 않습니다. 과거 v6.1 동작과 비교 회귀 테스트가 필요할 때 사용할 수 있습니다."
    );

    companion object {
        fun fromKey(value: String?): GeminiNanoConnectorMode =
            entries.firstOrNull { it.storageKey == value } ?: AiCoreTodo
    }
}

data class AppSettings(
    val spamFilterMode: SpamFilterMode = SpamFilterMode.Hybrid,
    val autoIndexAfterPermissionGrant: Boolean = true,
    val preferredRuntime: VendorRuntimeOption = VendorRuntimeOption.TfliteBuiltin,
    val geminiNanoConnectorMode: GeminiNanoConnectorMode = GeminiNanoConnectorMode.AiCoreTodo
)

data class RuntimeManifestEntry(
    val id: String,
    val type: String,
    val status: String,
    val purpose: String,
    val notes: String
)

data class RuntimeManifest(
    val appProfile: String,
    val runtimes: List<RuntimeManifestEntry>
)

/**
 * 설정 화면 / 진단용 실시간 앱 상태 스냅샷.
 */
data class DebugSnapshot(
    val threatScanCount: Int,
    val mediaInsightCount: Int,
    val devicePhotoCount: Int,
    val receiptMemoryCount: Int,
    val lastThreatScanAt: Long?,
    val lastMediaInsightAt: Long?,
    val lastDeviceIndexAt: Long?,
    val tfliteReady: Boolean,
    val tfliteInitError: String?,
    val selectedRuntimeKey: String,
    val selectedRuntimeLabel: String,
    val selectedRuntimeAvailable: Boolean,
    val selectedRuntimeStatus: String,
    val geminiNanoConnectorModeKey: String,
    val geminiNanoConnectorModeLabel: String,
    val geminiNanoConnectorStatus: String
)
