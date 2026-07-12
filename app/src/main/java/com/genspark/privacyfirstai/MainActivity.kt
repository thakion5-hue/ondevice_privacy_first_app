package com.genspark.privacyfirstai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.genspark.privacyfirstai.navigation.PrivacyFirstNavGraph
import com.genspark.privacyfirstai.ui.theme.PrivacyFirstTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as OnDeviceAiApp).appContainer
        setContent {
            PrivacyFirstTheme {
                PrivacyFirstNavGraph(container = container)
            }
        }
    }
}
