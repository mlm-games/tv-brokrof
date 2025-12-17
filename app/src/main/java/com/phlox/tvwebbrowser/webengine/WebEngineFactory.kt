package com.phlox.tvwebbrowser.webengine

import android.app.Activity
import android.content.Context
import androidx.annotation.UiThread
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.main.view.CursorLayout
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.webengine.gecko.GeckoWebEngine
import com.phlox.tvwebbrowser.webengine.webview.WebViewWebEngine

object WebEngineFactory {
    @UiThread
    suspend fun initialize(context: Context, webViewContainer: CursorLayout) {
        if (TVBro.config.isWebEngineGecko()) {
            GeckoWebEngine.initialize(context, webViewContainer)
            //HomePageHelper.prepareHomePageFiles()
        }
    }

    fun createWebEngine(tab: WebTabState): WebEngine {
        return if (TVBro.config.isWebEngineGecko())
            GeckoWebEngine(tab)
        else
            WebViewWebEngine(tab)
    }

    suspend fun clearCache(ctx: Context) {
        if (TVBro.config.isWebEngineGecko()) {
            GeckoWebEngine.clearCache(ctx)
        } else {
            WebViewWebEngine.clearCache(ctx)
        }
    }

    fun onThemeSettingUpdated(value: Config.Theme) {
        if (TVBro.config.isWebEngineGecko()) {
            GeckoWebEngine.onThemeSettingUpdated(value)
        }
    }
}