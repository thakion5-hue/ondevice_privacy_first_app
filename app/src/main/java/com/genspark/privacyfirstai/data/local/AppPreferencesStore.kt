package com.genspark.privacyfirstai.data.local

import android.content.Context
import com.genspark.privacyfirstai.BuildConfig
import com.genspark.privacyfirstai.domain.model.AppSettings
import com.genspark.privacyfirstai.domain.model.GeminiNanoConnectorMode
import com.genspark.privacyfirstai.domain.model.SpamFilterMode
import com.genspark.privacyfirstai.domain.model.VendorRuntimeOption

class AppPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): AppSettings = AppSettings(
        spamFilterMode = SpamFilterMode.fromKey(
            prefs.getString(KEY_SPAM_FILTER_MODE, SpamFilterMode.Hybrid.storageKey)
        ),
        autoIndexAfterPermissionGrant = prefs.getBoolean(KEY_AUTO_INDEX_AFTER_PERMISSION, true),
        preferredRuntime = VendorRuntimeOption.fromKey(
            prefs.getString(KEY_PREFERRED_RUNTIME, VendorRuntimeOption.TfliteBuiltin.storageKey)
        ),
        geminiNanoConnectorMode = GeminiNanoConnectorMode.fromKey(
            prefs.getString(
                KEY_GEMINI_NANO_CONNECTOR_MODE,
                BuildConfig.GEMINI_NANO_DEFAULT_CONNECTOR_MODE
            )
        )
    )

    fun setSpamFilterMode(mode: SpamFilterMode) {
        prefs.edit().putString(KEY_SPAM_FILTER_MODE, mode.storageKey).apply()
    }

    fun setAutoIndexAfterPermissionGrant(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_INDEX_AFTER_PERMISSION, enabled).apply()
    }

    fun setPreferredRuntime(runtime: VendorRuntimeOption) {
        prefs.edit().putString(KEY_PREFERRED_RUNTIME, runtime.storageKey).apply()
    }

    fun setGeminiNanoConnectorMode(mode: GeminiNanoConnectorMode) {
        prefs.edit().putString(KEY_GEMINI_NANO_CONNECTOR_MODE, mode.storageKey).apply()
    }

    companion object {
        private const val PREFS_NAME = "privacy_first_ai_preferences"
        private const val KEY_SPAM_FILTER_MODE = "spam_filter_mode"
        private const val KEY_AUTO_INDEX_AFTER_PERMISSION = "auto_index_after_permission"
        private const val KEY_PREFERRED_RUNTIME = "preferred_runtime"
        private const val KEY_GEMINI_NANO_CONNECTOR_MODE = "gemini_nano_connector_mode"
    }
}
