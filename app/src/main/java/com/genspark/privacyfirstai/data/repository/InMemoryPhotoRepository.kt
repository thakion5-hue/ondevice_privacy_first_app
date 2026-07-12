package com.genspark.privacyfirstai.data.repository

import com.genspark.privacyfirstai.domain.model.PhotoAsset
import com.genspark.privacyfirstai.domain.repository.PhotoRepository

class InMemoryPhotoRepository : PhotoRepository {
    private val photos = listOf(
        PhotoAsset("p1", "부산 출장 영수증", "부산", "지난주 금요일", listOf("receipt", "hotel", "busan"), true, false, qualityScore = 96),
        PhotoAsset("p2", "광안리 야경", "부산", "지난주 목요일", listOf("sea", "night", "busan"), false, false, qualityScore = 98),
        PhotoAsset("p3", "택시 영수증", "부산", "지난주 목요일", listOf("receipt", "taxi", "busan"), true, false, qualityScore = 88),
        PhotoAsset("p4", "메신저 스크린샷", "서울", "어제", listOf("screenshot", "chat"), false, true, duplicateGroup = "dup-a", qualityScore = 72),
        PhotoAsset("p5", "메신저 스크린샷 복사본", "서울", "어제", listOf("screenshot", "chat"), false, true, duplicateGroup = "dup-a", qualityScore = 68),
        PhotoAsset("p6", "흐린 화이트보드", "판교", "2일 전", listOf("whiteboard", "blur"), false, false, qualityScore = 51)
    )

    override fun getAll(): List<PhotoAsset> = photos

    override fun findReceipts(cityKeyword: String): List<PhotoAsset> {
        return photos.filter { it.isReceipt && (cityKeyword.isBlank() || it.city.contains(cityKeyword, true)) }
    }

    override fun getCleanupCandidates(): List<PhotoAsset> {
        return photos.filter { it.isScreenshot || it.duplicateGroup != null || it.qualityScore < 70 }
    }
}
