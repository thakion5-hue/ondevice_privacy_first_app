package com.genspark.privacyfirstai.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Dashboard : AppDestination("dashboard", "대시보드")
    data object Assistant : AppDestination("assistant", "오프라인 비서")
    data object Gallery : AppDestination("gallery", "갤러리 정리")
    data object Journal : AppDestination("journal", "일기 초안")
    data object Security : AppDestination("security", "클린 가드")
    data object Settings : AppDestination("settings", "설정")
}
