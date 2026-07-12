package com.genspark.privacyfirstai.feature.journal

import androidx.lifecycle.ViewModel
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.domain.model.JournalDraft

class JournalDraftViewModel(container: AppContainer) : ViewModel() {
    val draft: JournalDraft = container.conversationAnalyzer.buildJournalDraft()
}
