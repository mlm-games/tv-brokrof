package com.phlox.tvwebbrowser.compose.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.compose.settings.ui.ComposeSettingsScreen
import com.phlox.tvwebbrowser.compose.theme.TvBroComposeTheme

class ComposeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            TvBroComposeTheme {
                ComposeSettingsScreen(
                    config = AppContext.provideConfig(),
                    onClose = { finish() }
                )
            }
        }
    }
}