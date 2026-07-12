package com.genspark.privacyfirstai.ai

import com.genspark.privacyfirstai.domain.model.AssistantAnswer
import com.genspark.privacyfirstai.domain.model.DevicePhotoSnapshot
import com.genspark.privacyfirstai.domain.model.ReceiptMemory
import com.genspark.privacyfirstai.domain.repository.CalendarRepository
import com.genspark.privacyfirstai.domain.repository.MessageRepository
import com.genspark.privacyfirstai.domain.repository.PhotoRepository

class LocalAssistantOrchestrator(
    private val photoRepository: PhotoRepository,
    private val messageRepository: MessageRepository,
    private val calendarRepository: CalendarRepository,
    private val photoIndexer: LocalPhotoIndexer,
    private val conversationAnalyzer: LocalConversationAnalyzer,
    private val threatGuard: LocalThreatGuard
) {
    fun answer(
        prompt: String,
        importedReceipts: List<ReceiptMemory> = emptyList(),
        devicePhotos: List<DevicePhotoSnapshot> = emptyList()
    ): AssistantAnswer {
        val lower = prompt.lowercase()

        if (listOf("영수증", "receipt").any { lower.contains(it) }) {
            val queryTokens = lower.split(" ")
                .map { it.trim() }
                .filter { it.length >= 2 }

            val matchedImported = importedReceipts.filter { receipt ->
                val haystack = listOf(receipt.sourceLabel, receipt.merchant, receipt.dateLabel, receipt.rawText)
                    .joinToString(" ")
                    .lowercase()
                queryTokens.isEmpty() || queryTokens.any { haystack.contains(it) }
            }
            val city = when {
                lower.contains("부산") -> "부산"
                lower.contains("판교") -> "판교"
                else -> ""
            }
            val receipts = photoIndexer.findReceiptMemories(city)
            val mediaStoreMatches = devicePhotos.filter { snapshot ->
                val haystack = listOf(snapshot.title, snapshot.album, snapshot.dateLabel).joinToString(" ").lowercase()
                snapshot.receiptLike || queryTokens.any { haystack.contains(it) }
            }

            val importedBody = if (matchedImported.isEmpty()) {
                "- Room DB에 저장된 OCR 영수증이 없습니다."
            } else {
                matchedImported.joinToString("\n") {
                    "- ${it.merchant} / ${it.amountLabel} / ${it.dateLabel} / 파일: ${it.sourceLabel}"
                }
            }
            val mediaStoreBody = if (mediaStoreMatches.isEmpty()) {
                "- MediaStore 인덱스에서 영수증 후보를 찾지 못했습니다."
            } else {
                mediaStoreMatches.take(8).joinToString("\n") {
                    "- ${it.title} / ${it.album} / ${it.dateLabel} / ${it.width}x${it.height}"
                }
            }
            val sampleBody = if (receipts.isEmpty()) {
                "- 샘플 저장소에서 일치하는 영수증 이미지를 찾지 못했습니다."
            } else {
                receipts.joinToString("\n") { "- ${it.title} / ${it.dateLabel} / 태그: ${it.tags.joinToString()}" }
            }
            return AssistantAnswer(
                title = "오프라인 영수증 검색 결과",
                body = "[Room 저장 영수증]\n$importedBody\n\n[실기기 MediaStore 후보]\n$mediaStoreBody\n\n[샘플 로컬 인덱스]\n$sampleBody",
                references = matchedImported.map { it.id } + mediaStoreMatches.map { it.id } + receipts.map { it.id }
            )
        }

        if (listOf("일기", "threads", "초안").any { lower.contains(it) }) {
            val draft = conversationAnalyzer.buildJournalDraft()
            return AssistantAnswer(
                title = draft.headline,
                body = "${draft.body}\n\n${draft.hashtags.joinToString(" ")}",
                references = messageRepository.getRecentConversations().map { it.id }
            )
        }

        threatGuard.scan(prompt)?.let { scan ->
            return AssistantAnswer(
                title = scan.label,
                body = "위험도: ${scan.severity}\n사유: ${scan.reason}\n권장 조치: ${scan.recommendedAction}\n분석 엔진: ${scan.modelSource}",
                references = listOf("local-threat-guard")
            )
        }

        val nextEvent = calendarRepository.getRecentEvents().firstOrNull()
        val summary = photoIndexer.buildCleanupSummary()
        return AssistantAnswer(
            title = "로컬 컨텍스트 브리핑",
            body = buildString {
                appendLine("- 다음 참고 일정: ${nextEvent?.title ?: "없음"} (${nextEvent?.dateLabel ?: "-"})")
                appendLine("- 갤러리 정리 현황: $summary")
                appendLine("- 최근 대화 수: ${messageRepository.getRecentConversations().size}개")
                appendLine("- Room 영수증 저장소와 MediaStore 사진 인덱스 연동")
                appendLine("- 실제 연결된 추론: ML Kit 한국어 OCR, ML Kit 이미지 라벨링, TFLite 텍스트 분류기")
                appendLine("- 향후 교체 포인트: NNAPI / Qualcomm AI Engine / Samsung 온디바이스 런타임")
            },
            references = listOf("calendar", "photos", "messages", "room", "mediastore")
        )
    }
}
