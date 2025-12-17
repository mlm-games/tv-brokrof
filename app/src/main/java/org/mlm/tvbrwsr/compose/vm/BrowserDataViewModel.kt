package org.mlm.tvbrwser.compose.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.mlm.tvbrwser.BuildConfig
import org.mlm.tvbrwser.Config
import org.mlm.tvbrwser.TVBro
import org.mlm.tvbrwser.core.DispatcherProvider
import org.mlm.tvbrwser.data.FavoritesRepository
import org.mlm.tvbrwser.data.RecommendationsRepository
import org.mlm.tvbrwser.data.history.HistoryRepository
import org.mlm.tvbrwser.model.HistoryItem
import org.mlm.tvbrwser.model.HomePageLink
import org.mlm.tvbrwser.utils.UpdateChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class BrowserDataViewModel(
    private val config: Config,
    private val historyRepo: HistoryRepository,
    private val favoritesRepo: FavoritesRepository,
    private val recommendationsRepo: RecommendationsRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _homePageLinks = MutableStateFlow<List<HomePageLink>>(emptyList())
    val homePageLinks: StateFlow<List<HomePageLink>> = _homePageLinks.asStateFlow()
    private var lastHistoryItem: HistoryItem? = null
    private var lastHistorySaveJob: Job? = null
    private var loaded = false

    fun loadOnce() {
        if (loaded) return
        loaded = true
        viewModelScope.launch(dispatchers.main) {
            checkVersionCodeAndRunMigrations()
            withContext(dispatchers.io) {
                val last = historyRepo.last(1)
                if (last.isNotEmpty()) lastHistoryItem = last[0]
            }
            reloadHomePageLinks()
        }
    }

    private suspend fun checkVersionCodeAndRunMigrations() {
        if (config.appVersionCodeMark != BuildConfig.VERSION_CODE) {
            config.appVersionCodeMark = BuildConfig.VERSION_CODE
            withContext(dispatchers.io) { UpdateChecker.clearTempFilesIfAny(TVBro.instance) }
        }
    }

    fun reloadHomePageLinks() {
        viewModelScope.launch(dispatchers.main) {
            val links = withContext(dispatchers.io) {
                if (config.homePageMode != Config.HomePageMode.HOME_PAGE) return@withContext emptyList()
                val bookmarks = favoritesRepo.getHomePageBookmarks()
                if (bookmarks.isEmpty() && !config.initialBookmarksSuggestionsLoaded) {
                     val recs = recommendationsRepo.fetch(Locale.getDefault().country)
                     recs?.forEach { it.id = favoritesRepo.insert(it) }
                     config.initialBookmarksSuggestionsLoaded = true
                     favoritesRepo.getHomePageBookmarks().map { HomePageLink.fromBookmarkItem(it) }
                } else {
                     bookmarks.map { HomePageLink.fromBookmarkItem(it) }
                }
            }
            _homePageLinks.value = links
        }
    }

    fun logVisitedHistory(title: String?, url: String, faviconHash: String?) {
        if (config.incognitoMode || !url.startsWith("http")) return
        if (url == Config.HOME_PAGE_URL || url == lastHistoryItem?.url) return

        val now = System.currentTimeMillis()
        val item = HistoryItem().apply { this.url = url; this.title = title?:""; time = now; favicon = faviconHash }
        lastHistoryItem = item

        lastHistorySaveJob?.cancel()
        lastHistorySaveJob = viewModelScope.launch(dispatchers.main) {
            delay(5000)
            withContext(dispatchers.io) {
                item.id = historyRepo.insert(item)
                item.saved = true
            }
        }
    }

    fun onTabTitleUpdated(currentUrl: String, newTitle: String) {
        val last = lastHistoryItem ?: return
        if (currentUrl == last.url && last.saved) {
            last.title = newTitle
            viewModelScope.launch(dispatchers.io) { historyRepo.updateTitle(last.id, newTitle) }
        }
    }

    fun markBookmarkRecommendationAsUseful(order: Int) {
        val link = _homePageLinks.value.find { it.order == order } ?: return
        val favoriteId = link.favoriteId ?: return
        if (link.validUntil == null) return
        viewModelScope.launch(dispatchers.io) { favoritesRepo.markAsUseful(favoriteId) }
    }
}