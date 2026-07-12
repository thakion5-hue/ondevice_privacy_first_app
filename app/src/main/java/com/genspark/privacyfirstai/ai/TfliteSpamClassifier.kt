package com.genspark.privacyfirstai.ai

import android.content.Context
import android.util.Log
import com.genspark.privacyfirstai.domain.model.SpamModelResult
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max

/**
 * v5 정리 사항:
 *  - Interpreter 생성 실패에도 앱이 죽지 않도록 안전 초기화
 *  - `isReady`, `lastInitError` 노출: 설정 화면에서 진단 정보로 사용
 *  - Regex 리터럴 이스케이프 버그 수정 (`\\.` -> `\.`)
 */
class TfliteSpamClassifier(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    var lastInitError: String? = null
        private set

    val isReady: Boolean get() = interpreter != null

    init {
        // 첫 호출 시점이 아니라 앱 부팅 직후 컨테이너 초기화 때 시도해서
        // 설정 화면에서 실제 상태를 보여줄 수 있도록 한다.
        tryInitialize()
    }

    private fun tryInitialize() {
        if (interpreter != null) return
        runCatching {
            val buffer = loadModelBuffer(context, MODEL_ASSET_NAME)
            interpreter = Interpreter(buffer, Interpreter.Options().apply {
                setNumThreads(2)
            })
            lastInitError = null
        }.onFailure {
            interpreter = null
            lastInitError = it.message ?: it::class.java.simpleName
            Log.w(TAG, "TFLite init failed: $lastInitError", it)
        }
    }

    fun classify(text: String): SpamModelResult? {
        val runner = interpreter ?: run {
            tryInitialize()
            interpreter
        } ?: return null

        return runCatching {
            val featureVector = encode(text)
            val input = arrayOf(featureVector)
            val output = Array(1) { FloatArray(3) }
            runner.run(input, output)
            val logits = output[0].toList()
            val probabilities = softmax(output[0])
            val bestIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val label = when (bestIndex) {
                2 -> "fraud"
                1 -> "caution"
                else -> "safe"
            }
            SpamModelResult(
                label = label,
                score = probabilities[bestIndex],
                logits = logits
            )
        }.onFailure {
            Log.w(TAG, "TFLite classify failed", it)
        }.getOrNull()
    }

    private fun encode(text: String): FloatArray {
        val normalized = text.lowercase()
        val tokens = FEATURE_GROUPS.map { group ->
            val hits = group.count { normalized.contains(it) }
            when {
                hits >= 2 -> 1.5f
                hits == 1 -> 1f
                else -> 0f
            }
        }.toMutableList()

        val suspiciousLink = if (SUSPICIOUS_LINK_REGEX.containsMatchIn(normalized)) 1f else 0f
        val urgencyPunctuation = if (normalized.count { it == '!' } >= 2) 1f else 0f
        val safeTone = if (SAFE_TONE_TOKENS.any { normalized.contains(it) }) 1f else 0f

        tokens += suspiciousLink
        tokens += urgencyPunctuation
        tokens += safeTone
        return tokens.toFloatArray()
    }

    private fun softmax(values: FloatArray): FloatArray {
        val maxValue = values.maxOrNull() ?: 0f
        val exps = values.map { exp((it - maxValue).toDouble()).toFloat() }
        val sum = max(exps.sum(), 1e-6f)
        return exps.map { it / sum }.toFloatArray()
    }

    private fun loadModelBuffer(context: Context, assetName: String): ByteBuffer {
        val descriptor = context.assets.openFd(assetName)
        FileInputStream(descriptor.fileDescriptor).use { inputStream ->
            return inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.length
            ).order(ByteOrder.nativeOrder())
        }
    }

    companion object {
        private const val TAG = "TfliteSpamClassifier"
        private const val MODEL_ASSET_NAME = "spam_keyword_linear.tflite"

        private val SUSPICIOUS_LINK_REGEX =
            Regex("""https?://|bit\.ly|tinyurl|cutt\.ly|t\.co|open\.kakao""")

        private val SAFE_TONE_TOKENS =
            listOf("회의", "안건", "일정", "초안", "고마워", "내일", "문서")

        private val FEATURE_GROUPS = listOf(
            listOf("지금 클릭", "즉시", "긴급", "바로 확인"),
            listOf("무료 지급", "전액 지원", "혜택 즉시"),
            listOf("당첨", "축하", "경품"),
            listOf("투자 보장", "원금 보장", "고수익", "100% 수익"),
            listOf("가상자산", "코인", "투자방"),
            listOf("송금", "입금", "수수료 결제"),
            listOf("지원금", "민생지원", "환급"),
            listOf("계정 정지", "보안 경고", "로그인 차단"),
            listOf("본인 확인", "인증 갱신", "비밀번호 재설정"),
            listOf("택배 오류", "배송 실패", "주소 재확인"),
            listOf("카드 정지", "결제 제한", "이상 거래"),
            listOf("오픈채팅", "텔레그램"),
            listOf("whatsapp", "dm", "비공개 상담"),
        )
    }
}
