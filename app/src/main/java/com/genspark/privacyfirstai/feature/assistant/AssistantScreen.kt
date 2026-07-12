package com.genspark.privacyfirstai.feature.assistant

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
import com.genspark.privacyfirstai.di.AppContainer

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("오프라인 개인 비서", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            OutlinedTextField(
                value = vm.prompt,
                onValueChange = vm::updatePrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("로컬 명령 입력") }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = vm::run) { Text("기기 내에서 실행") }
                Button(onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text("영수증 이미지 선택")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("실제 온디바이스 OCR 상태", style = MaterialTheme.typography.titleMedium)
                    Text(vm.importStatus)
                    Text("Room 영수증 ${vm.importedReceipts.size}건 · MediaStore 인덱스 ${vm.devicePhotos.size}건")
                    if (vm.busy) {
                        Text("분석 중... 모델 실행은 기기 내부에서만 진행됩니다.")
                    }
                }
            }
        }
        vm.answer?.let { result ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(result.title, style = MaterialTheme.typography.titleLarge)
                        Text(result.body)
                        Text("로컬 참조: ${result.references.joinToString()}")
                    }
                }
            }
        }
        if (vm.importedReceipts.isNotEmpty()) {
            item {
                Text("Room 저장 OCR 영수증", style = MaterialTheme.typography.titleMedium)
            }
            items(vm.importedReceipts, key = { it.id }) { receipt ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(receipt.merchant, style = MaterialTheme.typography.titleMedium)
                        Text("${receipt.amountLabel} · ${receipt.dateLabel}")
                        Text("파일: ${receipt.sourceLabel}")
                        Text(receipt.rawText.take(180))
                    }
                }
            }
        }
    }
}
