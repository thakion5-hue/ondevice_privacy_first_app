package com.genspark.privacyfirstai.data.repository

import com.genspark.privacyfirstai.domain.model.ConversationSnippet
import com.genspark.privacyfirstai.domain.repository.MessageRepository

class InMemoryMessageRepository : MessageRepository {
    private val messages = listOf(
        ConversationSnippet("m1", "지민", "오늘 데모 반응 좋았어. 다음엔 더 캐주얼하게 써보자.", "오늘 09:10", "positive"),
        ConversationSnippet("m2", "민수", "부산 출장 정산은 영수증만 모으면 끝나.", "어제 20:12", "neutral"),
        ConversationSnippet("m3", "팀 채널", "Threads에는 짧고 솔직한 문장이 반응이 좋음.", "어제 17:42", "insightful"),
        ConversationSnippet("m4", "수연", "너 요즘 오프라인에서도 일 잘 정리하네.", "2일 전 22:01", "warm")
    )

    override fun getRecentConversations(): List<ConversationSnippet> = messages
}
