package com.genspark.privacyfirstai.ai

import com.genspark.privacyfirstai.domain.model.PhotoAsset
import com.genspark.privacyfirstai.domain.repository.PhotoRepository

class LocalPhotoIndexer(
    private val photoRepository: PhotoRepository
) {
    fun findReceiptMemories(cityKeyword: String): List<PhotoAsset> = photoRepository.findReceipts(cityKeyword)

    fun buildCleanupSummary(): String {
        val candidates = photoRepository.getCleanupCandidates()
        val duplicates = candidates.count { it.duplicateGroup != null }
        val screenshots = candidates.count { it.isScreenshot }
        val lowQuality = candidates.count { it.qualityScore < 70 }
        return "정리 후보 ${candidates.size}장 · 중복 ${duplicates}장 · 스크린샷 ${screenshots}장 · 저화질 ${lowQuality}장"
    }
}
