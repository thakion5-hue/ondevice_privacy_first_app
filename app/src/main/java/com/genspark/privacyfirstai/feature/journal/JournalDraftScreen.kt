package com.genspark.privacyfirstai.feature.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genspark.privacyfirstai.di.AppContainer

@Composable
fun JournalDraftRoute(container: AppContainer, paddingValues: PaddingValues) {
    val vm = JournalDraftViewModel(container)
    JournalDraftScreen(vm = vm, paddingValues = paddingValues)
}

@Composable
fun JournalDraftScreen(vm: JournalDraftViewModel, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("대화 기반 오늘 일기", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(vm.draft.headline, style = MaterialTheme.typography.titleLarge)
                Text(vm.draft.body)
                Text(vm.draft.hashtags.joinToString(" "))
            }
        }
    }
}
