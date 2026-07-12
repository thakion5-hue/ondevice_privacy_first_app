package com.genspark.privacyfirstai.feature.gallery

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.domain.model.DevicePhotoSnapshot
import com.genspark.privacyfirstai.domain.model.ImportedMediaInsight
import com.genspark.privacyfirstai.domain.model.PhotoAsset
import kotlinx.coroutines.launch

class GalleryCleanerViewModel(private val container: AppContainer) : ViewModel() {
    val items: List<PhotoAsset> = container.photoRepository.getCleanupCandidates()
    private val baseSummary: String = container.photoIndexer.buildCleanupSummary()

    var importedAnalyses by mutableStateOf<List<ImportedMediaInsight>>(emptyList())
        private set
    var devicePhotos by mutableStateOf<List<DevicePhotoSnapshot>>(emptyList())
        private set
    var runtimeSummary by mutableStateOf(baseSummary)
        private set
    var autoIndexAfterPermissionGrant by mutableStateOf(container.preferencesStore.getSettings().autoIndexAfterPermissionGrant)
        private set
    var busy by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            importedAnalyses = container.localStore.getMediaInsights()
            devicePhotos = container.localStore.getDevicePhotos()
            refreshSettings()
            updateSummary()
        }
    }

    fun refreshSettings() {
        autoIndexAfterPermissionGrant = container.preferencesStore.getSettings().autoIndexAfterPermissionGrant
    }

    fun analyzeSelected(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            busy = true
            val results = uris.mapNotNull { uri ->
                runCatching { container.mediaInspector.inspect(context, uri) }.getOrNull()
            }
            if (results.isNotEmpty()) {
                container.localStore.saveMediaInsights(results)
                importedAnalyses = container.localStore.getMediaInsights()
            }
            updateSummary()
            busy = false
        }
    }

    fun onMediaPermissionResult(context: Context, granted: Boolean) {
        refreshSettings()
        if (granted && autoIndexAfterPermissionGrant) {
            indexDeviceLibrary(context)
        } else {
            updateSummary()
        }
    }

    fun indexDeviceLibrary(context: Context) {
        viewModelScope.launch {
            busy = true
            val scanned = runCatching {
                container.mediaStorePhotoScanner.scanRecent(context)
            }.getOrDefault(emptyList())
            container.localStore.replaceDevicePhotos(scanned)
            devicePhotos = container.localStore.getDevicePhotos()
            updateSummary()
            busy = false
        }
    }

    private fun updateSummary() {
        val receiptLike = importedAnalyses.count { it.receiptLike }
        val screenshotLike = importedAnalyses.count { it.screenshotLike }
        val deviceScreenshots = devicePhotos.count { it.isScreenshot }
        val deviceReceiptHints = devicePhotos.count { it.receiptLike }
        runtimeSummary = "$baseSummary · Room 분석 ${importedAnalyses.size}건 · 문서/영수증 ${receiptLike}건 · 스크린샷 ${screenshotLike}건 · MediaStore ${devicePhotos.size}건 · 기기 스크린샷 ${deviceScreenshots}건 · 영수증 후보 ${deviceReceiptHints}건"
    }
}
