package com.genspark.privacyfirstai

import android.app.Application
import com.genspark.privacyfirstai.di.AppContainer

class OnDeviceAiApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(applicationContext)
    }
}
