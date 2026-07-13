package com.genspark.privacyfirstai.navigation

sealed class AppDestination(
    val route: String,
    val label: String,
    val navSymbol: String,
    val subtitle: String
) {
    data object Dashboard : AppDestination(
        route = "dashboard",
        label = "대시보드",
        navSymbol = "홈",
        subtitle = "로컬 AI 상태와 프라이버시 요약"
    )

    data object Assistant : AppDestination(
        route = "assistant",
        label = "오프라인 비서",
        navSymbol = "비서",
        subtitle = "영수증·문맥 기반 로컬 검색"
    )

    data object Gallery : AppDestination(
        route = "gallery",
        label = "갤러리 정리",
        navSymbol = "사진",
        subtitle = "스크린샷·영수증·중복 사진 분류"
    )

    data object Journal : AppDestination(
        route = "journal",
        label = "일기 초안",
        navSymbol = "기록",
        subtitle = "대화와 사진 맥락으로 초안 작성"
    )

    data object Security : AppDestination(
        route = "security",
        label = "클린 가드",
        navSymbol = "보안",
        subtitle = "스팸·피싱 위험 문구 즉시 감지"
    )

    data object Settings : AppDestination(
        route = "settings",
        label = "설정",
        navSymbol = "설정",
        subtitle = "런타임·권한·내보내기 관리"
    )
}
