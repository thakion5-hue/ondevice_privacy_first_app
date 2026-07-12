package com.genspark.privacyfirstai.domain.repository

import com.genspark.privacyfirstai.domain.model.CalendarEvent
import com.genspark.privacyfirstai.domain.model.ConversationSnippet
import com.genspark.privacyfirstai.domain.model.PhotoAsset

interface PhotoRepository {
    fun getAll(): List<PhotoAsset>
    fun findReceipts(cityKeyword: String): List<PhotoAsset>
    fun getCleanupCandidates(): List<PhotoAsset>
}

interface MessageRepository {
    fun getRecentConversations(): List<ConversationSnippet>
}

interface CalendarRepository {
    fun getRecentEvents(): List<CalendarEvent>
}
