package org.mlm.browkorftv.activity.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.utils.UpdateChecker
import org.mlm.browkorftv.utils.sameDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AutoUpdateViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val updateChecker = UpdateChecker(BuildConfig.VERSION_CODE)

    // Derived properties
    val lastUpdateNotificationTime: Calendar
        get() {
            val timestamp = settingsManager.current.lastUpdateUserNotificationTime
            return if (timestamp > 0) {
                Calendar.getInstance().apply { timeInMillis = timestamp }
            } else {
                Calendar.getInstance()
            }
        }

    val needAutoCheckUpdates: Boolean
        get() = settingsManager.current.autoCheckUpdates && BuildConfig.BUILT_IN_AUTO_UPDATE

    fun checkUpdate(force: Boolean, onDoneCallback: () -> Unit) = viewModelScope.launch(Dispatchers.Main) {
        if (updateChecker.versionCheckResult == null || force) {
            launch(Dispatchers.IO) {
                try {
                    updateChecker.check(
                        "https://raw.githubusercontent.com/truefedex/tv-bro/master/latest_version.json",
                        arrayOf(settingsManager.current.updateChannel)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.join()
        }
        onDoneCallback()
    }

    fun showUpdateDialogIfNeeded(activity: Activity, force: Boolean = false) {
        val now = Calendar.getInstance()
        if (lastUpdateNotificationTime.sameDay(now) && !force) {
            return
        }
        if (!updateChecker.hasUpdate()) {
            return // Or throw logic error if forced
        }

        viewModelScope.launch {
            settingsManager.update {
                it.copy(lastUpdateUserNotificationTime = now.timeInMillis)
            }
        }

        updateChecker.showUpdateDialog(activity, object : UpdateChecker.DialogCallback {
            override fun download() {
                if (activity.isFinishing) return
                if (updateChecker.versionCheckResult == null) return

                // Note: Logic to request permissions/install is handling in UpdateChecker or Activity
                // For pure ViewModel separation, we'd emit an event, but bridging legacy code:
                viewModelScope.launch {
                    updateChecker.downloadUpdate(activity, this)
                }
            }

            override fun later() {}
            override fun settings() {
                // Callback to open settings
            }
        })
    }

    fun saveAutoCheckUpdates(need: Boolean) {
        viewModelScope.launch {
            settingsManager.update { it.copy(autoCheckUpdates = need) }
        }
    }
}