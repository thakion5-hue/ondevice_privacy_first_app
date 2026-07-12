package com.genspark.privacyfirstai.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.genspark.privacyfirstai.ai.RuntimeAdapterAvailability
import com.genspark.privacyfirstai.ai.RuntimeBadgeTone
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.domain.model.DebugSnapshot
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode
import com.genspark.privacyfirstai.domain.model.SpamFilterMode
import com.genspark.privacyfirstai.domain.model.VendorRuntimeOption
import com.genspark.privacyfirstai.ui.component.RuntimeBadge
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

@Composable
fun SettingsRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { SettingsViewModel(container) }
    SettingsScreen(vm = vm, paddingValues = paddingValues)
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    var hasMediaPermission by remember { mutableStateOf(hasGalleryPermission(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarTone by remember { mutableStateOf(UiFeedbackTone.Info) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
        vm.refresh()
    }

    LaunchedEffect(vm.uiMessage?.id) {
        vm.uiMessage?.let { message ->
            snackbarTone = message.tone
            if (message.showToast) {
                Toast.makeText(
                    context,
                    message.message,
                    if (message.tone == UiFeedbackTone.Error) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
            snackbarHostState.showSnackbar(message.message)
            vm.consumeUiMessage(message.id)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(vm)
            }
            item {
                DebugCard(snapshot = vm.debugSnapshot, onRefresh = vm::refresh)
            }
            item {
                PermissionCard(
                    hasMediaPermission = hasMediaPermission,
                    autoIndex = vm.settings.autoIndexAfterPermissionGrant,
                    onRequestPermission = {
                        permissionLauncher.launch(readImagesPermission())
                    },
                    onRefreshState = {
                        hasMediaPermission = hasGalleryPermission(context)
                        vm.refresh()
                    },
                    onToggleAutoIndex = vm::setAutoIndexAfterPermissionGrant,
                    onOpenAppSettings = { openAppSettings(context) }
                )
            }
            item {
                RuntimeSwitcherCard(vm)
            }
            item {
                GeminiNanoConnectorCard(vm)
            }
            item {
                ModelSwitcherCard(vm)
            }
            item {
                DataManagementCard(vm)
            }
            item {
                ExportCard(
                    vm = vm,
                    onShareLatest = {
                        val latest = vm.exportHistory.firstOrNull()
                        if (latest == null) {
                            vm.postStatus(
                                "공유할 로그가 없습니다. 먼저 로그 내보내기를 실행하세요.",
                                UiFeedbackTone.Error,
                                showToast = true
                            )
                        } else {
                            shareExportedLog(context, latest) { feedback ->
                                vm.postStatus(
                                    message = feedback.message,
                                    tone = feedback.tone,
                                    showToast = feedback.showToast
                                )
                            }
                        }
                    }
                )
            }
            if (vm.exportHistory.isNotEmpty()) {
                item {
                    Text("내보낸 로그 히스토리", style = MaterialTheme.typography.titleMedium)
                }
                items(vm.exportHistory, key = { it.absolutePath }) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(log.fileName, style = MaterialTheme.typography.titleMedium)
                            Text("저장 시각: ${log.modifiedAtLabel}")
                            Text("크기: ${log.sizeBytes / 1024}KB")
                            Text(
                                "경로: ${log.absolutePath}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !vm.busy,
                                    onClick = {
                                        shareExportedLog(context, log) { feedback ->
                                            vm.postStatus(
                                                message = feedback.message,
                                                tone = feedback.tone,
                                                showToast = feedback.showToast
                                            )
                                        }
                                    }
                                ) {
                                    Text("이 로그 공유")
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("번들 런타임", style = MaterialTheme.typography.titleMedium)
            }
            items(vm.runtimeManifest.runtimes, key = { it.id }) { runtime ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(runtime.id, style = MaterialTheme.typography.titleMedium)
                        Text("${runtime.type} · ${runtime.status} · ${runtime.purpose}")
                        Text(runtime.notes)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            snackbar = { data ->
                val containerColor = when (snackbarTone) {
                    UiFeedbackTone.Info -> MaterialTheme.colorScheme.secondaryContainer
                    UiFeedbackTone.Success -> MaterialTheme.colorScheme.primaryContainer
                    UiFeedbackTone.Error -> MaterialTheme.colorScheme.errorContainer
                }
                val contentColor = when (snackbarTone) {
                    UiFeedbackTone.Info -> MaterialTheme.colorScheme.onSecondaryContainer
                    UiFeedbackTone.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                    UiFeedbackTone.Error -> MaterialTheme.colorScheme.onErrorContainer
                }
                Snackbar(
                    snackbarData = data,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            }
        )
    }
}

@Composable
private fun SummaryCard(vm: SettingsViewModel) {
    val selectedRuntimeDiagnostic = vm.selectedRuntimeDiagnostic()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("설정", style = MaterialTheme.typography.headlineSmall)
            Text(vm.runtimeSummary())
            Text("현재 스팸 판정 엔진: ${vm.settings.spamFilterMode.label}")
            Text(vm.currentModeSummary())
            Text(vm.currentModeDetail(), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("선호 텍스트 런타임: ${vm.settings.preferredRuntime.label}")
            selectedRuntimeDiagnostic?.let { diagnostic ->
                RuntimeStatusBadges(
                    diagnostic = diagnostic,
                    selected = true,
                    showFallback = !diagnostic.available &&
                        vm.settings.preferredRuntime != VendorRuntimeOption.TfliteBuiltin
                )
                Text(diagnostic.detail, style = MaterialTheme.typography.bodySmall)
            }
            Text(vm.selectedRuntimeDetail(), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Gemini Nano connector: ${vm.settings.geminiNanoConnectorMode.label}")
            Text(vm.geminiConnectorSummary(), style = MaterialTheme.typography.bodySmall)
            Text(vm.geminiConnectorDetail(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DebugCard(snapshot: DebugSnapshot?, onRefresh: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("진단 정보", style = MaterialTheme.typography.titleMedium)
            if (snapshot == null) {
                Text("초기화 중...")
            } else {
                Text("TFLite 준비 상태: ${if (snapshot.tfliteReady) "OK" else "실패"}")
                snapshot.tfliteInitError?.let {
                    Text(
                        "초기화 오류: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                Text(
                    "선호 런타임: ${snapshot.selectedRuntimeLabel} · ${snapshot.selectedRuntimeStatus}" +
                        if (snapshot.selectedRuntimeAvailable) "" else " · TFLite 폴백 대기"
                )
                Text(
                    "Gemini Nano connector: ${snapshot.geminiNanoConnectorModeLabel} · ${snapshot.geminiNanoConnectorStatus}"
                )
                Text(
                    "스팸 스캔 기록: ${snapshot.threatScanCount}건 · 마지막 ${snapshot.lastThreatScanAt.toLabel()}"
                )
                Text(
                    "이미지 라벨 분석: ${snapshot.mediaInsightCount}건 · 마지막 ${snapshot.lastMediaInsightAt.toLabel()}"
                )
                Text(
                    "MediaStore 인덱스: ${snapshot.devicePhotoCount}건 · 마지막 ${snapshot.lastDeviceIndexAt.toLabel()}"
                )
                Text("영수증 메모: ${snapshot.receiptMemoryCount}건")
            }
            OutlinedButton(onClick = onRefresh) {
                Text("진단 정보 새로고침")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    hasMediaPermission: Boolean,
    autoIndex: Boolean,
    onRequestPermission: () -> Unit,
    onRefreshState: () -> Unit,
    onToggleAutoIndex: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("권한 UX", style = MaterialTheme.typography.titleMedium)
            Text(
                if (hasMediaPermission) {
                    "갤러리 인덱싱에 필요한 이미지 접근 권한이 허용되어 있습니다."
                } else {
                    "실기기 MediaStore 인덱싱을 쓰려면 이미지 접근 권한이 필요합니다. 허용 전에는 포토 피커 기반 단발성 분석만 동작합니다."
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRequestPermission) {
                        Text(if (hasMediaPermission) "권한 다시 확인" else "이미지 접근 허용")
                    }
                    Button(onClick = onRefreshState) {
                        Text("상태 새로고침")
                    }
                }
                Button(onClick = onOpenAppSettings) {
                    Text("앱 설정 열기")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("권한 허용 후 자동 인덱싱")
                    Text(
                        "켜두면 갤러리 화면에서 권한 승인 직후 최근 사진 인덱싱을 바로 시작합니다.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = autoIndex,
                    onCheckedChange = onToggleAutoIndex
                )
            }
        }
    }
}

@Composable
private fun RuntimeSwitcherCard(vm: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("벤더 런타임 어댑터", style = MaterialTheme.typography.titleMedium)
            Text(
                "0.6.6에서는 Gemini Nano provider probe 위에 ML Kit Prompt feature probe가 추가되어, AICore가 AVAILABLE 상태일 때 실제 on-device session을 엽니다. DOWNLOADABLE/DOWNLOADING 상태에서는 설정 화면에서 모델 다운로드·재시도와 상태 갱신 UX를 제공하고, 그 외에는 기본 TFLite로 자동 폴백합니다.",
                style = MaterialTheme.typography.bodySmall
            )
            VendorRuntimeOption.entries.forEach { runtime ->
                val diagnostic = vm.runtimeDiagnostics.firstOrNull { it.runtime == runtime }
                RuntimeOptionCard(
                    runtime = runtime,
                    diagnostic = diagnostic,
                    selected = vm.settings.preferredRuntime == runtime,
                    onSelect = { vm.setPreferredRuntime(runtime) }
                )
            }
        }
    }
}

@Composable
private fun RuntimeOptionCard(
    runtime: VendorRuntimeOption,
    diagnostic: RuntimeAdapterAvailability?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(runtime.label, style = MaterialTheme.typography.titleSmall)
            RuntimeStatusBadges(
                diagnostic = diagnostic,
                selected = selected,
                showFallback = diagnostic != null &&
                    !diagnostic.available &&
                    runtime != VendorRuntimeOption.TfliteBuiltin
            )
            Text(runtime.description, style = MaterialTheme.typography.bodySmall)
            Text(runtime.detail, style = MaterialTheme.typography.bodySmall)
            diagnostic?.let {
                Text(it.detail, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSelect, enabled = !selected) {
                Text(if (selected) "현재 선택됨" else "이 런타임 선택")
            }
        }
    }
}

@Composable
private fun RuntimeStatusBadges(
    diagnostic: RuntimeAdapterAvailability?,
    selected: Boolean,
    showFallback: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuntimeBadge(
                text = if (selected) "선택됨" else "대기",
                tone = if (selected) RuntimeBadgeTone.Positive else RuntimeBadgeTone.Neutral
            )
            diagnostic?.let {
                RuntimeBadge(text = it.statusLabel, tone = it.badgeTone)
            }
        }
        if (showFallback) {
            RuntimeBadge(
                text = "기본 TFLite 자동 폴백",
                tone = RuntimeBadgeTone.Warning
            )
        }
    }
}

@Composable
private fun GeminiNanoConnectorCard(vm: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Gemini Nano connector 모드", style = MaterialTheme.typography.titleMedium)
            Text(
                "AICore TODO 계약, QA Fake, Legacy Stub 중 하나를 선택해 Gemini Nano 경로를 검증할 수 있습니다. 0.6.6에서는 AICore 경로가 ML Kit Prompt API를 통해 실제 Gemini Nano availability를 확인하고, DOWNLOADABLE/DOWNLOADING 상태에서는 인앱 다운로드·재시도와 자동 상태 갱신을 제공합니다. fake session은 계속 입력/출력 trace를 남깁니다.",
                style = MaterialTheme.typography.bodySmall
            )
            AiCorePromptDownloadCard(vm)
            GeminiNanoConnectorMode.entries.forEach { mode ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RuntimeBadge(
                                text = if (vm.settings.geminiNanoConnectorMode == mode) "선택됨" else "대기",
                                tone = if (vm.settings.geminiNanoConnectorMode == mode) {
                                    RuntimeBadgeTone.Positive
                                } else {
                                    RuntimeBadgeTone.Neutral
                                }
                            )
                            if (mode == GeminiNanoConnectorMode.QaFake) {
                                RuntimeBadge(text = "로컬 QA", tone = RuntimeBadgeTone.Warning)
                            }
                        }
                        Text(mode.description, style = MaterialTheme.typography.bodySmall)
                        Text(mode.detail, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { vm.setGeminiNanoConnectorMode(mode) },
                            enabled = vm.settings.geminiNanoConnectorMode != mode
                        ) {
                            Text(
                                if (vm.settings.geminiNanoConnectorMode == mode) {
                                    "현재 선택됨"
                                } else {
                                    "이 connector 사용"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCorePromptDownloadCard(vm: SettingsViewModel) {
    val state = vm.aiCorePromptDownloadState
    val isAiCoreSelected = vm.settings.geminiNanoConnectorMode == GeminiNanoConnectorMode.AiCoreTodo
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("AICore Prompt 모델 준비", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RuntimeBadge(
                    text = state.statusLabel,
                    tone = when {
                        state.availableForInference -> RuntimeBadgeTone.Positive
                        state.statusKey == "downloading" -> RuntimeBadgeTone.Warning
                        state.canDownload || state.canRetry -> RuntimeBadgeTone.Neutral
                        state.statusKey == "status_probe_error" -> RuntimeBadgeTone.Negative
                        else -> RuntimeBadgeTone.Warning
                    }
                )
                RuntimeBadge(
                    text = if (isAiCoreSelected) "AICore 선택됨" else "AICore 미선택",
                    tone = if (isAiCoreSelected) RuntimeBadgeTone.Positive else RuntimeBadgeTone.Neutral
                )
            }
            Text(
                "Gemini Nano 는 AICore 위에서 동작하며, DOWNLOADABLE 상태에서는 앱 안에서 바로 다운로드/재시도를 시작할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(state.summary, style = MaterialTheme.typography.bodySmall)
            state.progressLabel?.let {
                Text("진행률: $it", style = MaterialTheme.typography.bodySmall)
            }
            state.lastError?.let {
                Text("최근 오류: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "마지막 상태 갱신: ${state.lastUpdatedAtMillis.toLabel()}",
                style = MaterialTheme.typography.bodySmall
            )
            if (!isAiCoreSelected) {
                Text(
                    "지금은 다른 connector 가 선택되어 있습니다. 다운로드를 시작해도 실제 Prompt 세션 사용은 AICore TODO 계약 선택 후에만 열립니다.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !vm.aiCorePromptActionBusy && (state.canDownload || state.canRetry),
                    onClick = vm::startOrRetryAiCorePromptDownload
                ) {
                    Text(state.actionLabel ?: "모델 다운로드")
                }
                OutlinedButton(
                    enabled = !vm.aiCorePromptActionBusy,
                    onClick = { vm.refreshAiCorePromptState(showFeedback = true) }
                ) {
                    Text(if (state.statusKey == "downloading") "상태 즉시 갱신" else "상태 갱신")
                }
            }
        }
    }
}

@Composable
private fun ModelSwitcherCard(vm: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("모델 교체 UI", style = MaterialTheme.typography.titleMedium)
            Text("클린 가드의 텍스트 판정 엔진을 기기 상황에 맞게 전환할 수 있습니다.")
            SpamFilterMode.entries.forEach { mode ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.titleSmall)
                        Text(mode.description, style = MaterialTheme.typography.bodySmall)
                        Text(mode.detail, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { vm.setSpamFilterMode(mode) },
                            enabled = vm.settings.spamFilterMode != mode
                        ) {
                            Text(
                                if (vm.settings.spamFilterMode == mode) {
                                    "현재 선택됨"
                                } else {
                                    "이 모드로 전환"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataManagementCard(vm: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("온디바이스 데이터 관리", style = MaterialTheme.typography.titleMedium)
            Text(
                "각 항목별로 Room DB에 쌓인 히스토리를 개별 삭제할 수 있습니다. 네트워크 전송 없이 로컬에서만 삭제되고 되돌릴 수 없습니다.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !vm.busy,
                    onClick = vm::clearThreatScans
                ) { Text("스팸 스캔 비우기") }
                OutlinedButton(
                    enabled = !vm.busy,
                    onClick = vm::clearMediaInsights
                ) { Text("이미지 분석 비우기") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !vm.busy,
                    onClick = vm::clearDevicePhotos
                ) { Text("MediaStore 인덱스 비우기") }
                OutlinedButton(
                    enabled = !vm.busy,
                    onClick = vm::clearReceipts
                ) { Text("영수증 메모 비우기") }
            }
            Button(
                enabled = !vm.busy,
                onClick = vm::clearAll
            ) {
                Text("모든 히스토리 비우기")
            }
        }
    }
}

@Composable
private fun ExportCard(
    vm: SettingsViewModel,
    onShareLatest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("로그 내보내기", style = MaterialTheme.typography.titleMedium)
            Text(
                "현재 Room DB 히스토리와 설정/런타임 진단 스냅샷을 JSON으로 앱 내부 저장소에 내보냅니다. schema_version=6 형식을 유지하면서 Gemini Nano fake/real session trace(입력 preview / hit signal / decision rule / latency)를 함께 기록합니다.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !vm.busy,
                    onClick = vm::exportLogs
                ) { Text("로그 내보내기 실행") }
                OutlinedButton(
                    enabled = !vm.busy && vm.exportHistory.isNotEmpty(),
                    onClick = vm::clearExportedLogs
                ) { Text("내보낸 로그 삭제") }
            }
            OutlinedButton(
                enabled = !vm.busy && vm.exportHistory.isNotEmpty(),
                onClick = onShareLatest
            ) {
                Text("가장 최근 로그 공유")
            }
        }
    }
}

private fun Long?.toLabel(): String =
    if (this == null || this <= 0L) "기록 없음"
    else TIMESTAMP_FORMAT.format(Date(this))

private fun hasGalleryPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        readImagesPermission()
    ) == PackageManager.PERMISSION_GRANTED

private fun readImagesPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

private data class ShareFeedback(
    val message: String,
    val tone: UiFeedbackTone,
    val showToast: Boolean
)

private fun shareExportedLog(
    context: Context,
    log: ExportedLogFile,
    onFeedback: (ShareFeedback) -> Unit
) {
    val file = File(log.absolutePath)
    if (!file.exists()) {
        onFeedback(
            ShareFeedback(
                message = "공유할 로그 파일을 찾을 수 없습니다. 먼저 다시 내보내 주세요.",
                tone = UiFeedbackTone.Error,
                showToast = true
            )
        )
        return
    }

    val authority = "${context.packageName}.fileprovider"
    val uri = runCatching {
        FileProvider.getUriForFile(context, authority, file)
    }.getOrElse {
        onFeedback(
            ShareFeedback(
                message = "FileProvider URI 생성 실패: ${it.message ?: "알 수 없는 오류"}",
                tone = UiFeedbackTone.Error,
                showToast = true
            )
        )
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, log.fileName)
        putExtra(Intent.EXTRA_TEXT, "Privacy First AI 로그 스냅샷")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(Intent.createChooser(intent, "로그 공유"))
        onFeedback(
            ShareFeedback(
                message = "공유 시트를 열었습니다. 전송할 앱을 선택하세요.",
                tone = UiFeedbackTone.Success,
                showToast = true
            )
        )
    }.onFailure {
        val message = when (it) {
            is ActivityNotFoundException -> "로그를 받을 수 있는 앱을 찾지 못했습니다."
            else -> "로그 공유 실패: ${it.message ?: "알 수 없는 오류"}"
        }
        onFeedback(
            ShareFeedback(
                message = message,
                tone = UiFeedbackTone.Error,
                showToast = true
            )
        )
    }
}
