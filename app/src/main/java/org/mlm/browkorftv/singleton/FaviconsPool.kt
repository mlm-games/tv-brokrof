package org.mlm.browkorftv.singleton

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlm.browkorftv.AppContext
import org.mlm.browkorftv.model.HostConfig
import org.mlm.browkorftv.model.dao.HostsDao
import org.mlm.browkorftv.utils.FaviconExtractor
import java.io.File
import java.net.URL
import kotlin.math.abs

// 1. Add KoinComponent
object FaviconsPool : KoinComponent {
    const val FAVICONS_DIR = "favicons"
    const val FAVICON_PREFERRED_SIDE_SIZE = 120
    private val TAG: String = FaviconsPool::class.java.simpleName

    val faviconExtractor = FaviconExtractor()

    // 2. Inject dependencies directly
    private val hostsDao: HostsDao by inject()
    private val context: Context by inject()

    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    suspend fun get(urlOrHost: String): Bitmap? {
        Log.d(TAG, "get: $urlOrHost")
        if (!urlOrHost.startsWith("http://", true) && !urlOrHost.startsWith("https://", true)) {
            if (urlOrHost.contains("://")) return null
            val httpsResult = get("https://$urlOrHost")
            if (httpsResult != null) return httpsResult
            return get("http://$urlOrHost")
        }
        try {
            val urlObj = URL(urlOrHost)
            val host = urlObj.host
            if (host != null) {
                val hostBitmap = cache.get(host)
                if (hostBitmap != null) return hostBitmap

                val hostConfig = withContext(Dispatchers.IO) {
                    hostsDao.findByHostName(host)
                }

                if (hostConfig != null) {
                    val faviconFileName = hostConfig.favicon
                    if (faviconFileName != null) {
                        Log.d(TAG, "get: favicon found in db for $host")
                        val bitmap = withContext(Dispatchers.IO) {
                            val favIconsDir = File(favIconsDir())
                            if (!favIconsDir.exists() && !favIconsDir.mkdir()) return@withContext null
                            val faviconFile = File(favIconsDir, faviconFileName)
                            if (faviconFile.exists()) {
                                BitmapFactory.decodeFile(faviconFile.absolutePath)
                            } else {
                                null
                            }
                        }
                        if (bitmap != null) {
                            cache.put(host, bitmap)
                            return bitmap
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    WebView(AppContext.Companion.get()).apply {
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                super.onReceivedIcon(view, icon)
                                if (icon != null) {
                                    Log.d(TAG, "get: favicon received from webview for $host")
                                    cache.put(host, icon)
                                    runBlocking {
                                        saveFavicon(host, icon, hostConfig)
                                    }
                                }
                            }
                        }
                        loadUrl(urlOrHost)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun clear() {
        cache.evictAll()
    }

    fun favIconsDir(): String {
        // Use injected context
        return context.cacheDir.absolutePath + File.separator + FAVICONS_DIR
    }

    private suspend fun saveFavicon(host: String, bitmap: Bitmap, hostConfig: HostConfig?) =
        withContext(Dispatchers.IO) {
            val favIconsDir = File(favIconsDir())
            if (!favIconsDir.exists() && !favIconsDir.mkdir()) return@withContext
            val faviconFileName = host.hashCode().toString() + ".png"
            val faviconFile = File(favIconsDir, faviconFileName)
            if (faviconFile.exists()) {
                faviconFile.delete()
            }
            faviconFile.createNewFile()
            faviconFile.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            if (hostConfig != null) {
                hostConfig.favicon = faviconFileName
                hostsDao.update(hostConfig)
            } else {
                val newHostConfig = HostConfig(host)
                newHostConfig.favicon = faviconFileName
                hostsDao.insert(newHostConfig)
            }
        }

    private suspend fun downloadIcon(iconInfo: FaviconExtractor.IconInfo): Bitmap? =
        withContext(Dispatchers.IO) {
            val url = URL(iconInfo.src)
            val connection = url.openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(input, null, options)
            input.close()
            val width = options.outWidth
            val height = options.outHeight
            val scale = (width / 512).coerceAtLeast(height / 512)
            options.inJustDecodeBounds = false
            options.inSampleSize = scale
            val input2 = url.openConnection().getInputStream()
            val bitmap = BitmapFactory.decodeStream(input2, null, options)
            input2.close()
            return@withContext bitmap
        }

    private fun chooseNearestSizeIcon(icons: List<FaviconExtractor.IconInfo>, w: Int, h: Int): FaviconExtractor.IconInfo? {
        var nearestIcon: FaviconExtractor.IconInfo? = null
        var nearestDiff = Int.MAX_VALUE
        for (icon in icons) {
            val diff = abs(icon.width - w) + abs(icon.height - h)
            if (diff < nearestDiff) {
                nearestDiff = diff
                nearestIcon = icon
            }
        }
        return nearestIcon
    }
}