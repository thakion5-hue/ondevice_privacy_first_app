package com.genspark.privacyfirstai.ai.gemininano.provider

import android.os.Build

/**
 * v6.4에서 provider probe 를 별도 파일로 분리했다.
 *
 * 책임:
 *  - Android OS / OEM / 기기 조건만 확인 (entitlement 는 별도 gate 에서 담당)
 *  - 실제 SDK 호출은 하지 않고 조건만 평가
 *  - 반환값은 provider diagnostic 에서 그대로 요약 문자열로 사용될 수 있어야 함
 */
data class AiCoreProviderProbeResult(
    val meetsOsBaseline: Boolean,
    val osSdkInt: Int,
    val manufacturer: String,
    val brand: String,
    val summary: String,
    val checklist: List<String>
)

class AiCoreProviderProbe {

    fun probe(): AiCoreProviderProbeResult {
        val sdkInt = Build.VERSION.SDK_INT
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val meetsOsBaseline = sdkInt >= MIN_SDK_FOR_AICORE
        val summary = if (meetsOsBaseline) {
            "OS 조건 충족 (SDK=$sdkInt, brand=$brand)"
        } else {
            "OS 조건 미달 (SDK=$sdkInt, 필요=$MIN_SDK_FOR_AICORE)"
        }
        return AiCoreProviderProbeResult(
            meetsOsBaseline = meetsOsBaseline,
            osSdkInt = sdkInt,
            manufacturer = manufacturer,
            brand = brand,
            summary = summary,
            checklist = listOf(
                "check Build.VERSION.SDK_INT >= $MIN_SDK_FOR_AICORE",
                "record Build.MANUFACTURER / BRAND for OEM allowlist",
                "surface capability result to entitlement gate"
            )
        )
    }

    companion object {
        const val MIN_SDK_FOR_AICORE: Int = 34
    }
}
