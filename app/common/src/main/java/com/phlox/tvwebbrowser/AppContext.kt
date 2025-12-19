package com.phlox.tvwebbrowser

import android.app.Application
import android.content.Context
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.SettingsManager
import kotlinx.coroutines.runBlocking

class AppContext {
    companion object {
        private var instance: Application? = null
        private var settingsManager: SettingsManager? = null

        fun init(app: Application) {
            this.instance = app
            this.settingsManager = SettingsManager.getInstance(app)
        }

        fun get(): Context {
            return instance ?: throw IllegalStateException("AppContext is not initialized")
        }

        fun provideSettingsManager(): SettingsManager {
            return settingsManager ?: throw IllegalStateException("AppContext is not initialized")
        }

        // Convenience accessor for current settings
        val settings: AppSettings
            get() = provideSettingsManager().current
    }
}