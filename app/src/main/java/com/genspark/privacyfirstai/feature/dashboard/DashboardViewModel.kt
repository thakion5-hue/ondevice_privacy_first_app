package com.genspark.privacyfirstai.feature.dashboard

import androidx.lifecycle.ViewModel
import com.genspark.privacyfirstai.di.AppContainer

data class DashboardState(
    val privacySummary: String,
    val activeModeLabel: String,
    val architectureNotes: List<String>
)

class DashboardViewModel(container: AppContainer) : ViewModel() {
    private val settings = container.preferencesStore.getSettings()

    val state = DashboardState(
        privacySummary = container.photoIndexer.buildCleanupSummary(),
        activeModeLabel = settings.spamFilterMode.label,
        architectureNotes = listOf(
            "모든 추론은 기기 내 저장소 기준으로 동작",
            "민감 데이터 클라우드 전송 없음",
            "실제 연결: ML Kit 한국어 OCR + 이미지 라벨링 + TFLite 텍스트 분류기",
            "Room DB로 OCR/스캔 결과를 영구 저장",
            "MediaStore 사진 인덱싱으로 실기기 갤러리 후보 탐색",
            "설정 화면에서 권한 UX와 스팸 엔진 모드를 전환 가능",
            "설정에서 진단 정보 확인 · DB 항목별 비우기 · JSON 로그 내보내기 지원",
            "내보낸 JSON 로그는 FileProvider로 다른 앱에 공유 가능",
            "공유 시트 실행 성공/실패를 스낵바와 토스트로 정리해 즉시 피드백 제공",
            "Gemini Nano · Qualcomm · Samsung 런타임 어댑터 스캐폴딩 추가",
            "런타임 카드와 Clean Guard 엔진 상태에 상태 뱃지 디자인 적용",
            "Gemini Nano 실제 연결 경계를 별도 connector 인터페이스로 분리",
            "벤더 런타임 미연결/실패 시 기본 TFLite 및 휴리스틱으로 안전 폴백"
        )
    )
}
