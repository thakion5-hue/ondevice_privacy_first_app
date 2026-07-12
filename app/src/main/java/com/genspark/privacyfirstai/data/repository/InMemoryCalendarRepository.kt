package com.genspark.privacyfirstai.data.repository

import com.genspark.privacyfirstai.domain.model.CalendarEvent
import com.genspark.privacyfirstai.domain.repository.CalendarRepository

class InMemoryCalendarRepository : CalendarRepository {
    private val events = listOf(
        CalendarEvent("c1", "부산 파트너 미팅", "지난주 목요일", "부산 해운대", "영수증 촬영 및 경비 정리 필요"),
        CalendarEvent("c2", "런치 1:1", "어제", "판교", "Threads용 근황 소재 수집"),
        CalendarEvent("c3", "제품 데모", "오늘", "서울", "보안 필터 데모 강조")
    )

    override fun getRecentEvents(): List<CalendarEvent> = events
}
