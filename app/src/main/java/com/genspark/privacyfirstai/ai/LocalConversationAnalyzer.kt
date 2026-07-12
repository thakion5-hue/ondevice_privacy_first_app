package com.genspark.privacyfirstai.ai

import com.genspark.privacyfirstai.domain.model.JournalDraft
import com.genspark.privacyfirstai.domain.repository.MessageRepository

class LocalConversationAnalyzer(
    private val messageRepository: MessageRepository
) {
    fun buildJournalDraft(): JournalDraft {
        val snippets = messageRepository.getRecentConversations()
        val highlights = snippets.take(3).joinToString(" ") { "• ${it.content}" }
        return JournalDraft(
            headline = "오늘의 오프라인-우선 메모",
            body = "오늘 대화들을 다시 보니 핵심은 ‘더 짧고, 더 솔직하게, 더 안전하게’였다. $highlights 결국 기록도 정리도 클라우드보다 내 기기 안에서 가볍게 끝나는 경험이 더 중요하다는 생각.",
            hashtags = listOf("#온디바이스AI", "#프라이버시퍼스트", "#오늘의기록")
        )
    }
}
