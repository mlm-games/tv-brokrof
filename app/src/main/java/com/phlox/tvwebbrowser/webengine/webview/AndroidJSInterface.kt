package com.phlox.tvwebbrowser.webengine.webview

import android.net.http.SslError
import android.webkit.JavascriptInterface
import androidx.webkit.URLUtilCompat
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.AppSettings.Companion.HOME_PAGE_URL
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray

class AndroidJSInterface(private val webEngine: WebViewWebEngine) {

    private val settingsManager = AppContext.provideSettingsManager()
    private val settings: AppSettings get() = AppContext.settings

    @JavascriptInterface
    fun currentUrl(): String {
        if (!webEngine.tab.url.startsWith(WebViewEx.INTERNAL_SCHEME)) return ""
        return webEngine.tab.url
    }

    @JavascriptInterface
    fun reloadWithSslTrust() {
        val callback = webEngine.callback ?: return
        if ((webEngine.getView() as WebViewEx).currentOriginalUrl?.scheme != "file") return
        callback.getActivity().runOnUiThread {
            val webview = webEngine.getView() as? WebViewEx ?: return@runOnUiThread
            webview.trustSsl = true
            webEngine.tab.url.apply { webEngine.loadUrl(this) }
        }
    }

    @JavascriptInterface
    fun getStringByName(name: String): String {
        val ctx = TVBro.instance
        return when (name) {
            "connection_isnt_secure" -> ctx.getString(R.string.connection_isnt_secure)
            "hostname" -> ctx.getString(R.string.hostname)
            "err_desk" -> ctx.getString(R.string.err_desk)
            "details" -> ctx.getString(R.string.details)
            "back_to_safety" -> ctx.getString(R.string.back_to_safety)
            "go_im_aware" -> ctx.getString(R.string.go_im_aware)
            else -> ""
        }
    }

    @JavascriptInterface
    fun startVoiceSearch() {
        if (webEngine.tab.url != HOME_PAGE_URL) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.initiateVoiceSearch() }
    }

    @JavascriptInterface
    fun setSearchEngine(engine: String, customSearchEngineURL: String) {
        if (webEngine.tab.url != HOME_PAGE_URL) return

        // Find the index for the engine or use custom
        val index = AppSettings.SearchEnginesURLs.indexOfFirst {
            it.contains(engine, ignoreCase = true)
        }

        GlobalScope.launch {
            if (index >= 0 && index < AppSettings.SearchEnginesURLs.size - 1) {
                settingsManager.setSearchEngine(index)
            } else {
                settingsManager.setSearchEngine(
                    index = AppSettings.SearchEnginesURLs.size - 1,
                    customUrl = customSearchEngineURL
                )
            }
        }
    }

    @JavascriptInterface
    fun onEditBookmark(index: Int) {
        if (webEngine.tab.url != HOME_PAGE_URL) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.onEditHomePageBookmarkSelected(index) }
    }

    @JavascriptInterface
    fun onHomePageLoaded() {
        if (webEngine.tab.url != HOME_PAGE_URL) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread {
            val cfg = settings
            val jsArr = JSONArray()
            for (item in callback.getHomePageLinks()) {
                jsArr.put(item.toJsonObj())
            }
            var links = jsArr.toString()
            links = links.replace("'", "\\'")

            webEngine.evaluateJavascript(
                "renderLinks('${cfg.homePageLinksModeEnum.name}', $links)"
            )
            webEngine.evaluateJavascript(
                "applySearchEngine(\"${cfg.guessSearchEngineName()}\", \"${cfg.searchEngineURL}\")"
            )
        }
    }

    @JavascriptInterface
    fun lastSSLError(getDetails: Boolean): String {
        val lastSSLError = (webEngine.getView() as? WebViewEx)?.lastSSLError ?: return "unknown"
        return if (getDetails) {
            lastSSLError.toString()
        } else {
            when (lastSSLError.primaryError) {
                SslError.SSL_EXPIRED -> TVBro.instance.getString(R.string.ssl_expired)
                SslError.SSL_IDMISMATCH -> TVBro.instance.getString(R.string.ssl_idmismatch)
                SslError.SSL_DATE_INVALID -> TVBro.instance.getString(R.string.ssl_date_invalid)
                SslError.SSL_INVALID -> TVBro.instance.getString(R.string.ssl_invalid)
                else -> "unknown"
            }
        }
    }

    @JavascriptInterface
    fun takeBlobDownloadData(base64BlobData: String, fileName: String?, url: String, mimetype: String) {
        val callback = webEngine.callback ?: return
        val finalFileName = fileName ?: URLUtilCompat.guessFileName(url, null, mimetype)
        callback.onDownloadRequested(
            url, "",
            finalFileName, "TV Bro",
            mimetype, Download.OperationAfterDownload.NOP, base64BlobData
        )
    }

    @JavascriptInterface
    fun markBookmarkRecommendationAsUseful(bookmarkOrder: Int) {
        if (webEngine.tab.url != HOME_PAGE_URL) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.markBookmarkRecommendationAsUseful(bookmarkOrder) }
    }
}