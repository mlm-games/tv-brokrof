package org.mlm.browkorftv.webengine.webview

import android.content.Context
import androidx.startup.Initializer
import androidx.webkit.WebViewCompat
import org.mlm.browkorftv.settings.Theme
import org.mlm.browkorftv.webengine.*

class WebViewEngineInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val appCtx = context.applicationContext

        WebEngineFactory.registerProvider(
            WebEngineProvider("WebView", object : WebEngineProviderCallback {
                override suspend fun initialize(context: Context, webViewContainer: org.mlm.browkorftv.widgets.cursor.CursorLayout) {
                    // no-op for WebView
                }

                override fun createWebEngine(tab: org.mlm.browkorftv.model.WebTabState): WebEngine =
                    WebViewWebEngine(tab)

                override suspend fun clearCache(ctx: Context) {
                    android.webkit.WebView(ctx).clearCache(true)
                }

                override fun onThemeSettingUpdated(value: Theme) {
                    // TODO: handle webview dark mode globally? or let the current one be as it is?
                }

                override fun getWebEngineVersionString(): String {
                    val pkg = WebViewCompat.getCurrentWebViewPackage(appCtx)
                    return "${pkg?.packageName ?: "unknown"}:${pkg?.versionName ?: "unknown"}"
                }
            })
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}