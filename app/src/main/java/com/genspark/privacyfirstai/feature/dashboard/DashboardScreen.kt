package com.genspark.privacyfirstai.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genspark.privacyfirstai.di.AppContainer

@Composable
fun DashboardRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = remember { DashboardViewModel(container) }
    DashboardScreen(state = vm.state, paddingValues = paddingValues)
}

@Composable
fun DashboardScreen(state: DashboardState, paddingValues: PaddingValues) {
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
                    Text("온디바이스 AI 프라이버시 허브", style = MaterialTheme.typography.headlineSmall)
                    Text("인터넷 없이도 사진, 문자, 일정의 로컬 컨텍스트를 이해하는 유틸리티 앱 프로토타입")
                    Text("현재 로컬 분석 요약: ${state.privacySummary}")
                    Text("현재 클린 가드 엔진: ${state.activeModeLabel}")
                }
            }
        }
        items(state.architectureNotes) { note ->
            Card {
                Text(note, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
