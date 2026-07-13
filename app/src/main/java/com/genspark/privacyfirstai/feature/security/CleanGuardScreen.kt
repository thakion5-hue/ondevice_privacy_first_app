package com.genspark.privacyfirstai.feature.security

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.genspark.privacyfirstai.ui.component.AppHeroCard
import com.genspark.privacyfirstai.ui.component.AppMetricCard
import com.genspark.privacyfirstai.ui.component.AppPill
import com.genspark.privacyfirstai.ui.component.AppScreen
import com.genspark.privacyfirstai.ui.component.AppSectionTitle
import com.genspark.privacyfirstai.ui.component.RuntimeBadge
import com.genspark.privacyfirstai.ui.theme.Danger
import com.genspark.privacyfirstai.ui.theme.Gold
import com.genspark.privacyfirstai.ui.theme.Mint

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

    AppScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppHeroCard(
                    eyebrow = "REAL-TIME PROTECTION",
                    title = "실시간 클린 가드",
                    subtitle = "스팸·피싱 의심 문구와 광고 이미지를 기기 안에서 바로 스캔하고 위험도를 분류합니다."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuntimeBadge(
                            text = vm.activeSettings.spamFilterMode.label,
                            tone = RuntimeBadgeTone.Positive
                        )
                        RuntimeBadge(
                            text = vm.activeSettings.preferredRuntime.label,
                            tone = RuntimeBadgeTone.Neutral
                        )
                        vm.runtimeFallbackLabel()?.let {
                            RuntimeBadge(text = it, tone = RuntimeBadgeTone.Warning)
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "스캔 엔진",
                        value = vm.activeSettings.spamFilterMode.label,
                        detail = vm.engineSummary(),
                        accentColor = Mint
                    )
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "처리 상태",
                        value = if (vm.busy) "OCR 진행 중" else "즉시 감지 가능",
                        detail = vm.runtimeStatusDetail(),
                        accentColor = Gold
                    )
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppSectionTitle(
                            title = "메시지 또는 광고 문구 검사",
                            subtitle = "문자 입력과 이미지 OCR 검사 모두 같은 시각 톤으로 통합했습니다."
                        )
                        OutlinedTextField(
                            value = vm.input,
                            onValueChange = vm::updateInput,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("메시지/광고 문구") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = vm::scan, modifier = Modifier.weight(1f)) {
                                Text("텍스트 스캔")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("광고 이미지 OCR")
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppPill(text = "OCR PREVIEW")
                        Text(vm.ocrPreview, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                val result = vm.result
                val accent = when (result?.severity) {
                    "높음", "위험", "high", "HIGH" -> Danger
                    "중간", "보통", "medium", "MEDIUM" -> Gold
                    else -> Mint
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = accent.copy(alpha = 0.10f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppPill(
                            text = result?.severity ?: "안전",
                            containerColor = accent.copy(alpha = 0.16f),
                            contentColor = accent
                        )
                        if (result == null) {
                            Text("유의미한 위협이 탐지되지 않았습니다.")
                            Text(
                                "현재 입력 기준으로 즉시 차단할 패턴은 없었습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(result.label, style = MaterialTheme.typography.titleLarge)
                            Text("사유: ${result.reason}")
                            Text("조치: ${result.recommendedAction}")
                            Text(
                                "분석 엔진: ${result.modelSource}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (vm.recentRecords.isNotEmpty()) {
                item {
                    AppSectionTitle(
                        title = "최근 스캔 기록",
                        subtitle = "Room 저장 결과를 카드형 타임라인으로 더 읽기 쉽게 정리했습니다."
                    )
                }
                items(vm.recentRecords, key = { it.id }) { record ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${record.label} · ${record.severity}", style = MaterialTheme.typography.titleMedium)
                            Text("입력 경로: ${record.sourceLabel} · 엔진: ${record.modelSource}")
                            Text(
                                record.scannedTextPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                record.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
