# Project specific ProGuard rules.
#
# release 빌드에서 minify를 켜더라도 아래 라이브러리들이 안전하게 동작하도록
# v5 정리 단계에서 보존 규칙을 명시했다. 현재 debug/release 모두 minify는
# 꺼져 있으므로 실제 반영은 minifyEnabled=true 로 전환하는 시점부터다.

# ---- TensorFlow Lite ----
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ---- Google ML Kit ----
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# ---- AndroidX Room 관련 프록시 ----
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ---- Jetpack Compose ViewModel 리플렉션 사용에 대비 ----
-keepclassmembers class androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ---- 프로젝트 도메인 모델 (Kotlin data class) ----
-keep class com.genspark.privacyfirstai.domain.model.** { *; }

# ---- Kotlin 기본 최적화 ----
-dontwarn kotlin.**
-dontwarn kotlinx.**
