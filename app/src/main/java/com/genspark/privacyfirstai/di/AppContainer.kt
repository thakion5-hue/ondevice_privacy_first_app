package com.genspark.privacyfirstai.di

import android.content.Context
import com.genspark.privacyfirstai.ai.AiCoreTodoGeminiNanoRuntimeConnector
import com.genspark.privacyfirstai.ai.AiCoreTodoGeminiNanoRuntimeProvider
import com.genspark.privacyfirstai.ai.GeminiNanoRuntimeConnector
import com.genspark.privacyfirstai.ai.gemininano.AiCorePromptDownloadController
import com.genspark.privacyfirstai.ai.LocalAssistantOrchestrator
import com.genspark.privacyfirstai.ai.LocalConversationAnalyzer
import com.genspark.privacyfirstai.ai.LocalPhotoIndexer
import com.genspark.privacyfirstai.ai.LocalThreatGuard
import com.genspark.privacyfirstai.ai.MediaStorePhotoScanner
import com.genspark.privacyfirstai.ai.OnDeviceMediaInspector
import com.genspark.privacyfirstai.ai.OnDeviceReceiptOcr
import com.genspark.privacyfirstai.ai.OnDeviceRuntimeRegistry
import com.genspark.privacyfirstai.ai.QaFakeGeminiNanoRuntimeConnector
import com.genspark.privacyfirstai.ai.QaFakeGeminiNanoRuntimeProvider
import com.genspark.privacyfirstai.ai.StubGeminiNanoRuntimeConnector
import com.genspark.privacyfirstai.ai.LegacyStubGeminiNanoRuntimeProvider
import com.genspark.privacyfirstai.ai.TfliteSpamClassifier
import com.genspark.privacyfirstai.data.local.AppDatabase
import com.genspark.privacyfirstai.data.local.AppLocalStore
import com.genspark.privacyfirstai.data.local.AppPreferencesStore
import com.genspark.privacyfirstai.data.local.LogExporter
import com.genspark.privacyfirstai.data.local.ModelManifestStore
import com.genspark.privacyfirstai.data.repository.InMemoryCalendarRepository
import com.genspark.privacyfirstai.data.repository.InMemoryMessageRepository
import com.genspark.privacyfirstai.data.repository.InMemoryPhotoRepository
import com.genspark.privacyfirstai.domain.model.DebugSnapshot
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode

class AppContainer(
    val appContext: Context
) {
    private val database = AppDatabase.create(appContext)
    val localStore = AppLocalStore(database)
    val preferencesStore = AppPreferencesStore(appContext)
    val modelManifestStore = ModelManifestStore(appContext)

    val photoRepository = InMemoryPhotoRepository()
    val messageRepository = InMemoryMessageRepository()
    val calendarRepository = InMemoryCalendarRepository()

    val photoIndexer = LocalPhotoIndexer(photoRepository)
    val conversationAnalyzer = LocalConversationAnalyzer(messageRepository)
    val spamClassifier = TfliteSpamClassifier(appContext)
    val aiCorePromptDownloadController = AiCorePromptDownloadController()

    private val aicoreTodoGeminiNanoProvider by lazy { AiCoreTodoGeminiNanoRuntimeProvider() }
    private val qaFakeGeminiNanoProvider by lazy { QaFakeGeminiNanoRuntimeProvider() }
    private val legacyStubGeminiNanoProvider by lazy { LegacyStubGeminiNanoRuntimeProvider() }

    private val aicoreTodoGeminiNanoConnector by lazy {
        AiCoreTodoGeminiNanoRuntimeConnector(aicoreTodoGeminiNanoProvider)
    }
    private val qaFakeGeminiNanoConnector by lazy {
        QaFakeGeminiNanoRuntimeConnector(qaFakeGeminiNanoProvider)
    }
    private val stubGeminiNanoConnector by lazy {
        StubGeminiNanoRuntimeConnector(legacyStubGeminiNanoProvider)
    }

    val runtimeRegistry = OnDeviceRuntimeRegistry(
        tfliteClassifier = spamClassifier,
        geminiNanoConnectorProvider = ::resolveGeminiNanoConnector
    )
    val threatGuard = LocalThreatGuard(runtimeRegistry)
    val receiptOcr = OnDeviceReceiptOcr()
    val mediaInspector = OnDeviceMediaInspector()
    val mediaStorePhotoScanner = MediaStorePhotoScanner()
    val assistantOrchestrator = LocalAssistantOrchestrator(
        photoRepository = photoRepository,
        messageRepository = messageRepository,
        calendarRepository = calendarRepository,
        photoIndexer = photoIndexer,
        conversationAnalyzer = conversationAnalyzer,
        threatGuard = threatGuard
    )
    val logExporter = LogExporter(
        context = appContext,
        localStore = localStore,
        preferencesStore = preferencesStore,
        modelManifestStore = modelManifestStore,
        runtimeRegistry = runtimeRegistry
    )

    fun resolveGeminiNanoConnector(): GeminiNanoRuntimeConnector =
        when (preferencesStore.getSettings().geminiNanoConnectorMode) {
            GeminiNanoConnectorMode.AiCoreTodo -> aicoreTodoGeminiNanoConnector
            GeminiNanoConnectorMode.QaFake -> qaFakeGeminiNanoConnector
            GeminiNanoConnectorMode.LegacyStub -> stubGeminiNanoConnector
        }

    suspend fun buildDebugSnapshot(): DebugSnapshot {
        val settings = preferencesStore.getSettings()
        val runtimeAvailability = runtimeRegistry.availabilityFor(settings.preferredRuntime)
        val connectorDiagnostic = resolveGeminiNanoConnector().diagnostics()
        return DebugSnapshot(
            threatScanCount = localStore.countThreatScans(),
            mediaInsightCount = localStore.countMediaInsights(),
            devicePhotoCount = localStore.countDevicePhotos(),
            receiptMemoryCount = localStore.countReceipts(),
            lastThreatScanAt = localStore.latestThreatScanAt(),
            lastMediaInsightAt = localStore.latestMediaInsightAt(),
            lastDeviceIndexAt = localStore.latestDeviceIndexAt(),
            tfliteReady = spamClassifier.isReady,
            tfliteInitError = spamClassifier.lastInitError,
            selectedRuntimeKey = settings.preferredRuntime.storageKey,
            selectedRuntimeLabel = settings.preferredRuntime.label,
            selectedRuntimeAvailable = runtimeAvailability.available,
            selectedRuntimeStatus = runtimeAvailability.statusLabel,
            geminiNanoConnectorModeKey = settings.geminiNanoConnectorMode.storageKey,
            geminiNanoConnectorModeLabel = settings.geminiNanoConnectorMode.label,
            geminiNanoConnectorStatus = connectorDiagnostic.statusLabel
        )
    }
}
