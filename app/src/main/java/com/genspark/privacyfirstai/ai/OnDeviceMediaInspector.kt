package com.genspark.privacyfirstai.ai

import android.content.Context
import android.net.Uri
import com.genspark.privacyfirstai.domain.model.ImportedMediaInsight
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.roundToInt

class OnDeviceMediaInspector {
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    suspend fun inspect(context: Context, uri: Uri): ImportedMediaInsight {
        val bitmap = BitmapLoader.loadBitmap(context, uri)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val labels = labeler.process(inputImage).await()
            .sortedByDescending { it.confidence }
            .take(5)
            .map { "${it.text} ${"%.0f".format(it.confidence * 100)}%" }

        val plainLabels = labels.joinToString(" ").lowercase()
        val portraitDocRatio = bitmap.height > bitmap.width * 1.20
        val screenRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val screenshotLike = abs(screenRatio - (9f / 19.5f)) < 0.12f || abs(screenRatio - (19.5f / 9f)) < 0.12f
        val documentLike = portraitDocRatio || listOf("text", "document", "paper", "font").any { plainLabels.contains(it) }
        val receiptLike = documentLike && (bitmap.height >= bitmap.width || plainLabels.contains("paper"))
        val summary = when {
            receiptLike -> "문서/영수증 후보"
            screenshotLike -> "스크린샷 가능성 높음"
            documentLike -> "문서 이미지 가능성"
            else -> "일반 사진 또는 그래픽"
        }

        return ImportedMediaInsight(
            id = "media-${uri.hashCode()}",
            sourceLabel = BitmapLoader.resolveDisplayName(context, uri),
            labels = if (labels.isEmpty()) listOf("라벨 없음") else labels,
            summary = summary,
            dimensions = "${bitmap.width}x${bitmap.height}",
            receiptLike = receiptLike,
            screenshotLike = screenshotLike,
            documentLike = documentLike
        )
    }
}
