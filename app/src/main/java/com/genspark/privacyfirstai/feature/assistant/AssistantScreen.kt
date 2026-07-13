package com.genspark.privacyfirstai.feature.assistant

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
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.ui.component.AppHeroCard
import com.genspark.privacyfirstai.ui.component.AppMetricCard
import com.genspark.privacyfirstai.ui.component.AppPill
import com.genspark.privacyfirstai.ui.component.AppScreen
import com.genspark.privacyfirstai.ui.component.AppSectionTitle
import com.genspark.privacyfirstai.ui.theme.Mint
import com.genspark.privacyfirstai.ui.theme.Violet

@Composable
fun AssistantRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { AssistantViewModel(container) }
    AssistantScreen(vm = vm, paddingValues = paddingValues)
}

@Composable
fun AssistantScreen(vm: AssistantViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) vm.importReceipt(context, uri)
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
                    eyebrow = "ON-DEVICE ASSISTANT",
                    title = "오프라인 개인 비서",
                    subtitle = "클라우드 업로드 없이 영수증 OCR, 사진 메타, 로컬 문맥을 묶어 필요한 정보를 즉시 찾아줍니다."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppPill(text = "영수증 ${vm.importedReceipts.size}건")
                        AppPill(
                            text = "사진 인덱스 ${vm.devicePhotos.size}건",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
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
                        label = "OCR 상태",
                        value = if (vm.busy) "분석 중" else "대기",
                        detail = vm.importStatus,
                        accentColor = Mint
                    )
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "로컬 참조",
                        value = "${vm.importedReceipts.size + vm.devicePhotos.size}개",
                        detail = "Room 저장 영수증과 MediaStore 인덱스를 함께 사용",
                        accentColor = Violet
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
                            title = "로컬 명령 입력",
                            subtitle = "실제 빌드 시 앱 내부에서 바로 적용되는 새 Compose 입력 카드입니다."
                        )
                        OutlinedTextField(
                            value = vm.prompt,
                            onValueChange = vm::updatePrompt,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("찾고 싶은 내용") },
                            placeholder = { Text("예: 지난주 부산 출장 때 찍은 영수증 찾아줘") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = vm::run, modifier = Modifier.weight(1f)) {
                                Text("기기 내에서 실행")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("영수증 이미지 선택")
                            }
                        }
                    }
                }
            }
            vm.answer?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPill(
                                text = "LOCAL ANSWER",
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                            Text(result.title, style = MaterialTheme.typography.titleLarge)
                            Text(result.body, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "로컬 참조: ${result.references.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (vm.importedReceipts.isNotEmpty()) {
                item {
                    AppSectionTitle(
                        title = "저장된 OCR 영수증",
                        subtitle = "온디바이스 OCR이 끝난 결과가 카드형 레이아웃으로 정리됩니다."
                    )
                }
                items(vm.importedReceipts, key = { it.id }) { receipt ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(receipt.merchant, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppPill(text = receipt.amountLabel)
                                AppPill(
                                    text = receipt.dateLabel,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text("파일: ${receipt.sourceLabel}")
                            Text(
                                receipt.rawText.take(180),
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
