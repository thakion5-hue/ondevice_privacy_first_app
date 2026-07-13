package com.genspark.privacyfirstai.feature.gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.ui.component.AppHeroCard
import com.genspark.privacyfirstai.ui.component.AppMetricCard
import com.genspark.privacyfirstai.ui.component.AppPill
import com.genspark.privacyfirstai.ui.component.AppScreen
import com.genspark.privacyfirstai.ui.component.AppSectionTitle
import com.genspark.privacyfirstai.ui.theme.Gold
import com.genspark.privacyfirstai.ui.theme.Mint
import com.genspark.privacyfirstai.ui.theme.Violet

@Composable
fun GalleryCleanerRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { GalleryCleanerViewModel(container) }
    GalleryCleanerScreen(vm = vm, paddingValues = paddingValues)
}

@Composable
fun GalleryCleanerScreen(vm: GalleryCleanerViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    var hasMediaPermission by remember { mutableStateOf(hasGalleryPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
        vm.onMediaPermissionResult(context, granted)
    }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        if (uris.isNotEmpty()) vm.analyzeSelected(context, uris)
    }

    val receiptHints = vm.devicePhotos.count { it.receiptLike }
    val screenshotHints = vm.devicePhotos.count { it.isScreenshot }
    val documentHints = vm.devicePhotos.count { it.documentLike }

    AppScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppHeroCard(
                    eyebrow = "SMART GALLERY",
                    title = "갤러리도 더 똑똑하게",
                    subtitle = "스크린샷·문서·영수증 후보를 기기 안에서 분류하고, 정리 대상만 빠르게 추려줍니다."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppPill(text = if (hasMediaPermission) "사진 권한 허용됨" else "권한 필요")
                        AppPill(
                            text = if (vm.autoIndexAfterPermissionGrant) "자동 인덱싱 ON" else "자동 인덱싱 OFF",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Room 분석",
                        value = "${vm.importedAnalyses.size}건",
                        detail = "선택 이미지 라벨 분석 결과가 로컬 DB에 저장됨",
                        accentColor = Mint
                    )
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "MediaStore",
                        value = "${vm.devicePhotos.size}건",
                        detail = "기기 사진 인덱싱 결과와 후보 분류 상태",
                        accentColor = Violet
                    )
                }
            }
            item {
                AppMetricCard(
                    label = "정리 후보 요약",
                    value = "스크린샷 ${screenshotHints} · 영수증 ${receiptHints} · 문서 ${documentHints}",
                    detail = vm.runtimeSummary,
                    accentColor = Gold
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppSectionTitle(
                            title = "로컬 정리 작업",
                            subtitle = "권한 확인, 인덱싱, 선택 이미지 분석 버튼을 상단 작업 카드로 재배치했습니다."
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(readImagesPermission()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (hasMediaPermission) "권한 다시 확인" else "기기 사진 권한")
                            }
                            Button(
                                enabled = hasMediaPermission,
                                onClick = { vm.indexDeviceLibrary(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("기기 사진 인덱싱")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    pickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("선택 이미지 분석")
                            }
                            Button(
                                onClick = {
                                    hasMediaPermission = hasGalleryPermission(context)
                                    vm.refreshSettings()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("상태 새로고침")
                            }
                        }
                        Button(onClick = { openAppSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                            Text("앱 설정 열기")
                        }
                        if (vm.busy) {
                            Text(
                                "MediaStore 조회 또는 온디바이스 이미지 라벨링 실행 중...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (vm.devicePhotos.isNotEmpty()) {
                item {
                    AppSectionTitle(
                        title = "실기기 MediaStore 인덱스",
                        subtitle = "실제 기기에서 스캔한 사진 메타데이터가 카드형 레이아웃으로 표시됩니다."
                    )
                }
                items(vm.devicePhotos.take(12), key = { it.id }) { photo ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(photo.title, style = MaterialTheme.typography.titleMedium)
                            Text("${photo.album} · ${photo.dateLabel}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppPill(text = "${photo.width}x${photo.height}")
                                AppPill(
                                    text = "${photo.sizeBytes / 1024}KB",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                buildString {
                                    append(if (photo.isScreenshot) "스크린샷" else "일반 사진")
                                    if (photo.documentLike) append(" · 문서성")
                                    if (photo.receiptLike) append(" · 영수증 후보")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (vm.importedAnalyses.isNotEmpty()) {
                item {
                    AppSectionTitle(
                        title = "Room 저장 이미지 분석",
                        subtitle = "라벨 결과를 쉽게 비교하도록 배지 스타일을 강화했습니다."
                    )
                }
                items(vm.importedAnalyses, key = { it.id }) { analysis ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(analysis.sourceLabel, style = MaterialTheme.typography.titleMedium)
                            Text("${analysis.summary} · ${analysis.dimensions}")
                            Text(
                                "라벨: ${analysis.labels.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                AppSectionTitle(
                    title = "샘플 로컬 후보",
                    subtitle = "프로토타입 샘플 데이터도 같은 디자인 톤으로 맞췄습니다."
                )
            }
            items(vm.items) { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text("${item.dateLabel} · ${item.city}")
                        Text(
                            "태그: ${item.tags.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AppPill(
                            text = "품질 점수 ${item.qualityScore}",
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun hasGalleryPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        readImagesPermission()
    ) == PackageManager.PERMISSION_GRANTED

private fun readImagesPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
