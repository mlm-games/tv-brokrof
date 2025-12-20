package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.activity.main.FavoritesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPickUrl: (String) -> Unit,
    onAddBookmark: () -> Unit,
    onEditBookmark: (Long) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val loading by viewModel.loading.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Favorites", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onAddBookmark) { Text("Add bookmark") }
            Button(onClick = onBack) { Text("Back") }
        }

        if (loading) {
            Text("Loading…")
            return
        }

        Spacer(Modifier.height(8.dp))
        Text("Bookmarks", style = MaterialTheme.typography.titleMedium)

        if (bookmarks.isEmpty()) {
            Text("No bookmarks yet")
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookmarks.take(40).forEach { b ->
                Surface(onClick = { b.url?.let(onPickUrl) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(b.title ?: b.url.orEmpty(), maxLines = 1)
                            Text(b.url.orEmpty(), maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onEditBookmark(b.id) }) { Text("Edit") }
                    }
                }
            }
        }
    }
}