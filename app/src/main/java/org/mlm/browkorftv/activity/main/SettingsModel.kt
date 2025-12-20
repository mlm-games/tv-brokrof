package org.mlm.browkorftv.activity.main

import org.mlm.browkorftv.AppContext
import org.mlm.browkorftv.settings.*
import org.mlm.browkorftv.utils.activemodel.ActiveModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsModel : ActiveModel() {
    companion object {
        val TAG: String = SettingsModel::class.java.simpleName
        const val TV_BRO_UA_PREFIX = "Browkorf TV/1.0 "
    }

    private val settingsManager = AppContext.provideSettingsManager()

    // Settings flow
    val settings: StateFlow<AppSettings> = settingsManager.settingsState

    // Current settings (for non-reactive access)
    val current: AppSettings get() = settings.value

    // User agent configuration
    val userAgentStringTitles = arrayOf(
        "Default (recommended)", "Chrome (Desktop)", "Chrome (Mobile)",
        "Firefox (Desktop)", "Firefox (Mobile)", "Edge (Desktop)", "Custom"
    )

    val keepScreenOnFlow: Flow<Boolean> = settingsManager.keepScreenOnFlow
    val homePageModeFlow: Flow<HomePageMode> = settings.map { it.homePageModeEnum }
    val homePageLinksModeFlow: Flow<HomePageLinksMode> = settings.map { it.homePageLinksModeEnum }
    val homePageFlow: Flow<String> = settings.map { it.homePage }
    val searchEngineURLFlow: Flow<String> = settingsManager.searchEngineURLFlow

    var homePage: String
        get() = current.homePage
        set(value) {
            modelScope.launch {
                settingsManager.update { it.copy(homePage = value) }
            }
        }

    var homePageMode: HomePageMode
        get() = current.homePageModeEnum
        set(value) {
            modelScope.launch {
                settingsManager.update { it.copy(homePageMode = value.ordinal) }
            }
        }

    var homePageLinksMode: HomePageLinksMode
        get() = current.homePageLinksModeEnum
        set(value) {
            modelScope.launch {
                settingsManager.update { it.copy(homePageLinksMode = value.ordinal) }
            }
        }

    var keepScreenOn: Boolean
        get() = current.keepScreenOn
        set(value) {
            modelScope.launch {
                settingsManager.setKeepScreenOn(value)
            }
        }

    fun setSearchEngineURL(url: String) {
        modelScope.launch {
            // Find if URL matches a predefined engine
            val index = AppSettings.SearchEnginesURLs.indexOf(url)
            if (index >= 0 && index < AppSettings.SearchEnginesURLs.size - 1) {
                settingsManager.setSearchEngine(index)
            } else {
                // Custom URL
                settingsManager.setSearchEngine(
                    index = AppSettings.SearchEnginesURLs.size - 1,
                    customUrl = url
                )
            }
        }
    }

    fun setHomePageProperties(
        homePageMode: HomePageMode,
        customHomePageUrl: String?,
        homePageLinksMode: HomePageLinksMode
    ) {
        modelScope.launch {
            settingsManager.setHomePageProperties(
                mode = homePageMode,
                customUrl = customHomePageUrl,
                linksMode = homePageLinksMode
            )
        }
    }
}