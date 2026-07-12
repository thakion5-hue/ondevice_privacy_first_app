package com.genspark.privacyfirstai.ai

import android.content.Context
import android.net.Uri
import com.genspark.privacyfirstai.domain.model.ReceiptMemory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class OnDeviceReceiptOcr {
    private val recognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    suspend fun extractReceipt(context: Context, uri: Uri): ReceiptMemory {
        val rawText = extractText(context, uri)
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = lines.firstOrNull { line ->
            line.length in 2..32 && !line.matches(Regex("""[0-9 ./:-]+"""))
        } ?: "미확인 매장"

        val amountRegex = Regex("""(?<!\d)(\d{1,3}(,\d{3})+|\d{4,8})\s?(원|KRW)?""")
        val amountLabel = amountRegex.findAll(rawText)
            .map { it.value.trim() }
            .maxByOrNull { token ->
                token.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
            } ?: "금액 미추출"

        val dateRegex = Regex("""(20\d{2}[./-]\d{1,2}[./-]\d{1,2})|(\d{4}[./-]\d{1,2}[./-]\d{1,2})""")
        val dateLabel = dateRegex.find(rawText)?.value ?: "날짜 미추출"

        return ReceiptMemory(
            id = "receipt-${uri.hashCode()}-${max(lines.size, 1)}",
            sourceLabel = BitmapLoader.resolveDisplayName(context, uri),
            merchant = merchant,
            amountLabel = amountLabel,
            dateLabel = dateLabel,
            rawText = rawText.ifBlank { "텍스트를 인식하지 못했습니다." }
        )
    }

    suspend fun extractText(context: Context, uri: Uri): String {
        val bitmap = BitmapLoader.loadBitmap(context, uri)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(inputImage).await()
        return result.text
    }
}
