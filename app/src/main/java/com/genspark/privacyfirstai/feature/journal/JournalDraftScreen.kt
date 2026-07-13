package com.genspark.privacyfirstai.feature.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.genspark.privacyfirstai.ui.theme.Mint
import com.genspark.privacyfirstai.ui.theme.Violet

@Composable
fun JournalDraftRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { JournalDraftViewModel(container) }
    JournalDraftScreen(vm = vm, paddingValues = paddingValues)
}

@Composable
fun JournalDraftScreen(vm: JournalDraftViewModel, paddingValues: PaddingValues) {
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
                    eyebrow = "LOCAL JOURNAL",
                    title = "오늘 기록도 내 기기에서",
                    subtitle = "대화와 사진의 맥락을 바탕으로 로컬 일기 초안을 만들고, 민감한 메모는 기기 안에만 남깁니다."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppPill(text = "로컬 초안 생성")
                        AppPill(
                            text = "해시태그 ${vm.draft.hashtags.size}개",
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
                        label = "초안 길이",
                        value = "${vm.draft.body.length}자",
                        detail = "사진·대화 맥락으로 자동 생성된 본문",
                        accentColor = Mint
                    )
                    AppMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "톤",
                        value = vm.draft.headline,
                        detail = "기록용 문장과 감정 키워드를 함께 구성",
                        accentColor = Violet
                    )
                }
            }
            item {
                AppSectionTitle(
                    title = "일기 초안 미리보기",
                    subtitle = "실제 앱에서 더 편안하고 읽기 좋은 카드 레이아웃으로 개선했습니다."
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppPill(
                            text = "DRAFT",
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(vm.draft.headline, style = MaterialTheme.typography.headlineSmall)
                        Text(vm.draft.body, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            vm.draft.hashtags.take(3).forEach { tag ->
                                AppPill(
                                    text = tag,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
