package org.mlm.browkorftv.activity

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import org.mlm.browkorftv.R
import org.mlm.browkorftv.TVBro
import org.mlm.browkorftv.AppContext
import org.mlm.browkorftv.activity.main.MainActivity
import kotlinx.coroutines.launch

class IncognitoModeMainActivity : MainActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private val settingsManager by lazy { AppContext.provideSettingsManager() }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        if (!AppContext.settings.incognitoModeHintSuppress) {
            showIncognitoModeHintDialog()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            TVBro.instance.needToExitProcessAfterMainActivityFinish = true
        }
        super.onDestroy()
    }

    private fun showIncognitoModeHintDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.incognito_mode)
            .setIcon(R.drawable.ic_incognito)
            .setMessage(R.string.incognito_mode_hint)
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .setNeutralButton(R.string.don_t_show_again) { dialog, _ ->
                lifecycleScope.launch {
                    settingsManager.setIncognitoModeHintSuppress(true)
                }
                dialog.dismiss()
            }
            .show()
    }
}