package com.phlox.tvwebbrowser.compose.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.settings.AdBlock
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.AppSettingsSchema
import com.phlox.tvwebbrowser.settings.General
import com.phlox.tvwebbrowser.settings.HomePage
import com.phlox.tvwebbrowser.settings.Search
import com.phlox.tvwebbrowser.settings.Updates
import com.phlox.tvwebbrowser.settings.UserAgent
import com.phlox.tvwebbrowser.settings.WebEngine
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.ui.AutoSettingsScreen
import io.github.mlmgames.settings.ui.CategoryConfig
import io.github.mlmgames.settings.ui.ProvideStringResources
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { AppContext.provideSettingsManager() }
    val settings by settingsManager.settings.collectAsState(AppSettings())
    val scope = rememberCoroutineScope()

    ProvideStringResources(AndroidStringResourceProvider(context)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            AutoSettingsScreen(
                schema = AppSettingsSchema,
                value = settings,
                modifier = Modifier.padding(padding),
                onSet = { name, value ->
                    scope.launch {
                        settingsManager.set(name, value)
                    }
                },
                categoryConfigs = listOf(
                    CategoryConfig(General::class, "General"),
                    CategoryConfig(HomePage::class, "Home Page"),
                    CategoryConfig(Search::class, "Search"),
                    CategoryConfig(UserAgent::class, "User Agent"),
                    CategoryConfig(WebEngine::class, "Web Engine"),
                    CategoryConfig(AdBlock::class, "Ad Blocker"),
                    CategoryConfig(Updates::class, "Updates"),
                )
            )
        }
    }
}