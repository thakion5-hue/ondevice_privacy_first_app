package com.genspark.privacyfirstai.feature.assistant

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.domain.model.AssistantAnswer
import com.genspark.privacyfirstai.domain.model.DevicePhotoSnapshot
import com.genspark.privacyfirstai.domain.model.ReceiptMemory
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val container: AppContainer
) : ViewModel() {
    var prompt by mutableStateOf("지난주 부산 출장 갔을 때 찍은 영수증 사진 찾아줘")
    var answer by mutableStateOf<AssistantAnswer?>(null)
        private set
    var importedReceipts by mutableStateOf<List<ReceiptMemory>>(emptyList())
        private set
    var devicePhotos by mutableStateOf<List<DevicePhotoSnapshot>>(emptyList())
        private set
    var importStatus by mutableStateOf("Room 저장소를 불러오는 중...")
        private set
    var busy by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            refreshLocalState()
            answer = container.assistantOrchestrator.answer(prompt, importedReceipts, devicePhotos)
        }
    }

    fun updatePrompt(value: String) {
        prompt = value
    }

    fun run() {
        answer = container.assistantOrchestrator.answer(prompt, importedReceipts, devicePhotos)
    }

    fun importReceipt(context: Context, uri: Uri) {
        viewModelScope.launch {
            busy = true
            importStatus = "영수증 이미지를 기기 내 OCR로 분석하는 중..."
            runCatching {
                container.receiptOcr.extractReceipt(context, uri)
            }.onSuccess { receipt ->
                container.localStore.saveReceipt(receipt)
                refreshLocalState()
                importStatus = "${receipt.sourceLabel} 분석 완료 · ${receipt.merchant} · ${receipt.amountLabel} · Room 저장 완료"
                answer = AssistantAnswer(
                    title = "실제 OCR 영수증 가져오기 완료",
                    body = "매장: ${receipt.merchant}\n금액: ${receipt.amountLabel}\n날짜: ${receipt.dateLabel}\n원문 미리보기:\n${receipt.rawText.take(220)}",
                    references = listOf(receipt.id)
                )
            }.onFailure { error ->
                importStatus = "OCR 실패: ${error.message ?: "알 수 없는 오류"}"
            }
            busy = false
        }
    }

    private suspend fun refreshLocalState() {
        importedReceipts = container.localStore.getReceipts()
        devicePhotos = container.localStore.getDevicePhotos()
        importStatus = if (importedReceipts.isEmpty()) {
            if (devicePhotos.isEmpty()) {
                "저장된 OCR 영수증이 없습니다. 영수증을 가져오거나 갤러리 탭에서 MediaStore 인덱싱을 먼저 실행하세요."
            } else {
                "저장된 OCR 영수증 없음 · MediaStore 인덱스 ${devicePhotos.size}건 로드됨"
            }
        } else {
            "Room 저장 영수증 ${importedReceipts.size}건 · MediaStore 인덱스 ${devicePhotos.size}건 로드됨"
        }
    }
}
