package com.phlox.tvwebbrowser

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.phlox.tvwebbrowser.activity.main.MainActivity
import com.phlox.tvwebbrowser.model.HostConfig
import com.phlox.tvwebbrowser.settings.Theme
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.singleton.FaviconsPool
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.CookieHandler
import java.net.CookieManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class TVBro : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        lateinit var instance: TVBro
        const val CHANNEL_ID_DOWNLOADS: String = "downloads"
        val TAG = TVBro::class.simpleName
    }

    lateinit var threadPool: ThreadPoolExecutor
        private set

    var needToExitProcessAfterMainActivityFinish = false
    var needRestartMainActivityAfterExitingProcess = false

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        super.onCreate()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            applicationInfo.targetSdkVersion = 32
        }

        instance = this

        // Initialize AppContext with new settings system
        AppContext.init(this)

        val maxThreadsInOfflineJobsPool = Runtime.getRuntime().availableProcessors()
        threadPool = ThreadPoolExecutor(
            0, maxThreadsInOfflineJobsPool, 20,
            TimeUnit.SECONDS, ArrayBlockingQueue(maxThreadsInOfflineJobsPool)
        )

        initWebEngineStuff()
        initNotificationChannels()
        ActiveModelsRepository.init(this)

        // Apply initial theme
        applyTheme(AppContext.settings.themeEnum)

        // Observe theme changes
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            AppContext.provideSettingsManager().themeFlow.collectLatest { theme ->
                applyTheme(theme)
            }
        }

        registerActivityLifecycleCallbacks(this)
    }

    private fun applyTheme(theme: Theme) {
        when (theme) {
            Theme.BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.WHITE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Suppress("KotlinConstantConditions")
    private fun initWebEngineStuff() {
        Log.i(TAG, "initWebEngineStuff")

        try {
            Class.forName("com.phlox.tvwebbrowser.webengine.webview.WebViewWebEngine")
        } catch (e: ClassNotFoundException) {
            throw AssertionError(e)
        }
        try {
            Class.forName("com.phlox.tvwebbrowser.webengine.gecko.GeckoWebEngine")
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "GeckoWebEngine not found")
        }

        val cookieManager = CookieManager()
        CookieHandler.setDefault(cookieManager)
        FaviconsPool.databaseDelegate = object : FaviconsPool.DatabaseDelegate {
            override fun findByHostName(host: String): HostConfig? {
                return AppDatabase.db.hostsDao().findByHostName(host)
            }
            override suspend fun update(hostConfig: HostConfig) {
                AppDatabase.db.hostsDao().update(hostConfig)
            }
            override suspend fun insert(newHostConfig: HostConfig) {
                AppDatabase.db.hostsDao().insert(newHostConfig)
            }
        }
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.downloads)
            val descriptionText = getString(R.string.downloads_notifications_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_DOWNLOADS, name, importance)
            channel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        Log.i(TAG, "onActivityDestroyed: " + activity.javaClass.simpleName)
        if (needToExitProcessAfterMainActivityFinish && activity is MainActivity) {
            Log.i(TAG, "onActivityDestroyed: exiting process")
            if (needRestartMainActivityAfterExitingProcess) {
                Log.i(TAG, "onActivityDestroyed: restarting main activity")
                val intent = Intent(this@TVBro, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            exitProcess(0)
        }
    }
}