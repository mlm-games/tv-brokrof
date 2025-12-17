package org.mlm.tvbrwser.webengine

import android.content.Context
import androidx.annotation.UiThread
import org.mlm.tvbrwser.Config
import org.mlm.tvbrwser.TVBro
import org.mlm.tvbrwser.activity.main.view.CursorLayout
import org.mlm.tvbrwser.model.WebTabState
import org.mlm.tvbrwser.webengine.gecko.GeckoWebEngine
import org.mlm.tvbrwser.webengine.webview.WebViewWebEngine

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