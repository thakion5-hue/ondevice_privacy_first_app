package com.genspark.privacyfirstai.data.local

import android.content.Context
import com.genspark.privacyfirstai.domain.model.RuntimeManifest
import com.genspark.privacyfirstai.domain.model.RuntimeManifestEntry
import org.json.JSONObject

class ModelManifestStore(
    private val context: Context
) {
    fun load(): RuntimeManifest {
        val raw = context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
        val json = JSONObject(raw)
        val runtimesJson = json.optJSONArray("active_runtimes")
        val runtimes = buildList {
            if (runtimesJson != null) {
                for (index in 0 until runtimesJson.length()) {
                    val item = runtimesJson.getJSONObject(index)
                    add(
                        RuntimeManifestEntry(
                            id = item.optString("id"),
                            type = item.optString("type"),
                            status = item.optString("status"),
                            purpose = item.optString("purpose"),
                            notes = item.optString("notes")
                        )
                    )
                }
            }
        }
        return RuntimeManifest(
            appProfile = json.optString("app_profile", "privacy_first_ondevice_ai"),
            runtimes = runtimes
        )
    }

    companion object {
        private const val MANIFEST_ASSET = "model_manifest.json"
    }
}
