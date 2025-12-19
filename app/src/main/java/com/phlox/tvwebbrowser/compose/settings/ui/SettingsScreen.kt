package com.phlox.tvwebbrowser.compose.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.Config

@Composable
fun ComposeSettingsScreen(
    config: Config,
    onClose: () -> Unit,
) {
    var adblock by remember { mutableStateOf(config.adBlockEnabled) }
    var keepOn by remember { mutableStateOf(config.keepScreenOn) }
    val engine = remember { config.webEngine }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onClose) { Text("Back") }
        }

        Surface(onClick = {
            adblock = !adblock
            config.adBlockEnabled = adblock
        }) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("AdBlock")
                Text(if (adblock) "ON" else "OFF")
            }
        }

        Surface(onClick = {
            keepOn = !keepOn
            config.keepScreenOn = keepOn
        }) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Keep screen on")
                Text(if (keepOn) "ON" else "OFF")
            }
        }

        Surface {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Web engine")
                Text(engine)
            }
        }

        Text(
            text = "Test, should remove and use kmp-settings module.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}