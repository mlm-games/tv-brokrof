package org.mlm.browkorftv.webengine.gecko

import android.content.Context
import androidx.startup.Initializer
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.settings.Theme
import org.mlm.browkorftv.webengine.*
import org.mlm.browkorftv.widgets.cursor.CursorLayout
import org.mozilla.geckoview.BuildConfig

class GeckoEngineInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        WebEngineFactory.registerProvider(
            WebEngineProvider(GeckoWebEngine.ENGINE_NAME, object : WebEngineProviderCallback {
                override suspend fun initialize(context: Context, webViewContainer: CursorLayout) {
                    GeckoWebEngine.initialize(context, webViewContainer)
                }

                override fun createWebEngine(tab: WebTabState): WebEngine =
                    GeckoWebEngine(tab)

                override suspend fun clearCache(ctx: Context) {
                    GeckoWebEngine.clearCache()
                }

                override fun onThemeSettingUpdated(value: Theme) {
                    GeckoWebEngine.onThemeSettingUpdated(value)
                }

                override fun getWebEngineVersionString(): String =
                    "${BuildConfig.LIBRARY_PACKAGE_NAME}:${BuildConfig.MOZ_APP_VERSION}.${BuildConfig.MOZ_APP_BUILDID}-${BuildConfig.MOZ_UPDATE_CHANNEL}"
            })
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}