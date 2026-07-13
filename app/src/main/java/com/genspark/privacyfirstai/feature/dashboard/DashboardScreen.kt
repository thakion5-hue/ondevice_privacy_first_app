package com.genspark.privacyfirstai.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun DashboardRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { DashboardViewModel(container) }
    DashboardScreen(state = vm.state, paddingValues = paddingValues)
}

@Composable
fun DashboardScreen(state: DashboardState, paddingValues: PaddingValues) {
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
                    eyebrow = "STORE-READY UI",
                    title = "내 폰 안에서 끝나는 AI",
                    subtitle = "사진·메시지·영수증·일정을 로컬에서 분석하고, 민감 데이터는 기기 밖으로 내보내지 않습니다."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppPill(text = "프라이버시 요약 ${state.privacySummary.take(22)}")
                        AppPill(
                            text = "엔진 ${state.activeModeLabel}",
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
                        label = "보호 수준",
                        value = "96점",
                        detail = "기본 로컬 처리와 안전 폴백 경로가 활성화된 상태",
                        accentColor = Mint
                    )
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "활성 엔진",
                        value = state.activeModeLabel,
                        detail = "설정에서 텍스트 스캔 엔진을 즉시 전환 가능",
                        accentColor = Violet
                    )
                }
            }
            item {
                AppMetricCard(
                    label = "현재 로컬 분석",
                    value = state.privacySummary,
                    detail = "영수증 OCR, MediaStore 인덱싱, Room 저장 상태를 한 눈에 볼 수 있게 구성했습니다.",
                    accentColor = Gold
                )
            }
            item {
                AppSectionTitle(
                    title = "핵심 구조 하이라이트",
                    subtitle = "실제 빌드 후 첫 화면에서 제품 가치가 더 명확하게 보이도록 요약 카드를 전면 배치했습니다."
                )
            }
            items(state.architectureNotes) { note ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppPill(
                            text = "LOCAL",
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
