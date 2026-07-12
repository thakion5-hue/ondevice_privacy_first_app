package com.genspark.privacyfirstai.feature.security

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.genspark.privacyfirstai.ai.RuntimeBadgeTone
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.ui.component.RuntimeBadge

@Composable
fun CleanGuardRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { CleanGuardViewModel(container) }
    CleanGuardScreen(vm = vm, paddingValues = paddingValues)
}

@Composable
fun CleanGuardScreen(vm: CleanGuardViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) vm.scanImage(context, uri)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("실시간 클린 가드", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("엔진 상태", style = MaterialTheme.typography.titleMedium)
                    Text(vm.engineSummary())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuntimeBadge(
                            text = vm.activeSettings.spamFilterMode.label,
                            tone = RuntimeBadgeTone.Positive
                        )
                        RuntimeBadge(
                            text = vm.activeSettings.preferredRuntime.label,
                            tone = RuntimeBadgeTone.Neutral
                        )
                        if (vm.shouldShowGeminiConnector()) {
                            RuntimeBadge(
                                text = vm.geminiConnectorModeLabel(),
                                tone = RuntimeBadgeTone.Warning
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuntimeBadge(
                            text = vm.selectedRuntimeDiagnostic.statusLabel,
                            tone = vm.selectedRuntimeDiagnostic.badgeTone
                        )
                        vm.runtimeFallbackLabel()?.let {
                            RuntimeBadge(
                                text = it,
                                tone = RuntimeBadgeTone.Warning
                            )
                        }
                    }
                    Text(vm.runtimeStatusDetail(), style = MaterialTheme.typography.bodySmall)
                    if (vm.shouldShowGeminiConnector()) {
                        Text(vm.geminiConnectorDetail(), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("설정 탭에서 하이브리드 / 휴리스틱 전용 / TFLite 우선으로 전환할 수 있습니다.")
                    Button(onClick = vm::refreshSettings) {
                        Text("설정 다시 불러오기")
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = vm.input,
                onValueChange = vm::updateInput,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("메시지/광고 문구") }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = vm::scan) { Text("텍스트 스캔") }
                Button(onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text("광고 이미지 OCR 스캔")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("OCR 미리보기", style = MaterialTheme.typography.titleMedium)
                    Text(vm.ocrPreview)
                    if (vm.busy) {
                        Text("실제 온디바이스 OCR 실행 중...")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vm.result == null) {
                        Text("유의미한 위협이 탐지되지 않았습니다.")
                    } else {
                        Text(vm.result!!.label, style = MaterialTheme.typography.titleLarge)
                        Text("위험도: ${vm.result!!.severity}")
                        Text("사유: ${vm.result!!.reason}")
                        Text("조치: ${vm.result!!.recommendedAction}")
                        Text("분석 엔진: ${vm.result!!.modelSource}")
                    }
                }
            }
        }
        if (vm.recentRecords.isNotEmpty()) {
            item {
                Text("최근 Room 저장 스캔", style = MaterialTheme.typography.titleMedium)
            }
            items(vm.recentRecords, key = { it.id }) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${record.label} · ${record.severity}", style = MaterialTheme.typography.titleMedium)
                        Text("입력 경로: ${record.sourceLabel} · 엔진: ${record.modelSource}")
                        Text(record.scannedTextPreview)
                        Text(record.reason)
                    }
                }
            }
        }
    }
}
