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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("갤러리 정리 허브", style = MaterialTheme.typography.headlineSmall)
                    Text(vm.runtimeSummary)
                    Text(
                        if (hasMediaPermission) {
                            "이미지 접근 권한 허용됨 · 최근 사진 인덱싱을 바로 실행할 수 있습니다."
                        } else {
                            "이미지 접근 권한 없음 · 포토 피커 분석은 가능하지만 MediaStore 전체 인덱싱은 제한됩니다."
                        }
                    )
                    Text(
                        if (vm.autoIndexAfterPermissionGrant) {
                            "설정: 권한 허용 직후 자동 인덱싱 켜짐"
                        } else {
                            "설정: 권한 허용 직후 자동 인덱싱 꺼짐"
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            permissionLauncher.launch(readImagesPermission())
                        }) {
                            Text(if (hasMediaPermission) "권한 다시 확인" else "기기 사진 권한")
                        }
                        Button(
                            enabled = hasMediaPermission,
                            onClick = { vm.indexDeviceLibrary(context) }
                        ) {
                            Text("기기 사진 인덱싱")
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) {
                                Text("선택 이미지 라벨 분석")
                            }
                            Button(onClick = {
                                hasMediaPermission = hasGalleryPermission(context)
                                vm.refreshSettings()
                            }) {
                                Text("상태 새로고침")
                            }
                        }
                        Button(onClick = { openAppSettings(context) }) {
                            Text("앱 설정")
                        }
                    }
                    if (vm.busy) {
                        Text("MediaStore 조회 또는 온디바이스 이미지 라벨링 실행 중...")
                    }
                }
            }
        }
        if (vm.devicePhotos.isNotEmpty()) {
            item {
                Text("실기기 MediaStore 인덱스", style = MaterialTheme.typography.titleMedium)
            }
            items(vm.devicePhotos.take(12), key = { it.id }) { photo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(photo.title, style = MaterialTheme.typography.titleMedium)
                        Text("${photo.album} · ${photo.dateLabel}")
                        Text("${photo.width}x${photo.height} · ${photo.sizeBytes / 1024}KB")
                        Text(
                            buildString {
                                append(if (photo.isScreenshot) "스크린샷" else "일반 사진")
                                if (photo.documentLike) append(" · 문서성")
                                if (photo.receiptLike) append(" · 영수증 후보")
                            }
                        )
                    }
                }
            }
        }
        if (vm.importedAnalyses.isNotEmpty()) {
            item {
                Text("Room 저장 이미지 분석", style = MaterialTheme.typography.titleMedium)
            }
            items(vm.importedAnalyses, key = { it.id }) { analysis ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(analysis.sourceLabel, style = MaterialTheme.typography.titleMedium)
                        Text("${analysis.summary} · ${analysis.dimensions}")
                        Text("라벨: ${analysis.labels.joinToString()}")
                    }
                }
            }
        }
        item {
            Text("샘플 로컬 후보", style = MaterialTheme.typography.titleMedium)
        }
        items(vm.items) { item ->
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text("${item.dateLabel} · ${item.city}")
                    Text("태그: ${item.tags.joinToString()}")
                    Text("품질 점수: ${item.qualityScore}")
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
