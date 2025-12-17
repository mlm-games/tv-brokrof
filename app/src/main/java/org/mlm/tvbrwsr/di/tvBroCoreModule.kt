package org.mlm.tvbrwser.di

import org.mlm.tvbrwser.TVBro
import org.mlm.tvbrwser.Config
import org.mlm.tvbrwser.compose.vm.BrowserDataViewModel
import org.mlm.tvbrwser.compose.vm.DownloadsViewModel
import org.mlm.tvbrwser.compose.vm.HistoryViewModel
import org.mlm.tvbrwser.compose.vm.TabsViewModel
import org.mlm.tvbrwser.core.DefaultDispatcherProvider
import org.mlm.tvbrwser.core.DispatcherProvider
import org.mlm.tvbrwser.data.AdblockRepository
import org.mlm.tvbrwser.data.history.HistoryRepository
import org.mlm.tvbrwser.data.history.RoomHistoryRepository
import org.mlm.tvbrwser.data.settings.ConfigRepository
import org.mlm.tvbrwser.data.settings.provideTvBroDataStore
import org.mlm.tvbrwser.data.tabs.RoomTabsRepository
import org.mlm.tvbrwser.data.tabs.TabsRepository
import org.mlm.tvbrwser.singleton.AppDatabase
import org.mlm.tvbrwser.compose.runtime.BrowserCommandBus
import org.mlm.tvbrwser.compose.runtime.ShortcutCaptureController
import org.mlm.tvbrwser.compose.vm.BookmarkEditorViewModel
import org.mlm.tvbrwser.compose.vm.FavoritesViewModel
import org.mlm.tvbrwser.compose.vm.HomePageSlotEditorViewModel
import org.mlm.tvbrwser.compose.vm.UpdateViewModel
import org.mlm.tvbrwser.data.ApkDownloader
import org.mlm.tvbrwser.data.DownloadsRepository
import org.mlm.tvbrwser.data.FavoritesRepository
import org.mlm.tvbrwser.data.RecommendationsRepository
import org.mlm.tvbrwser.data.RoomDownloadsRepository
import org.mlm.tvbrwser.data.RoomFavoritesRepository
import org.mlm.tvbrwser.data.UpdateRepository
import org.mlm.tvbrwser.singleton.shortcuts.ShortcutMgr
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tvBroCoreModule = module {
    single { TVBro.config }
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

val updateModule = module {
    single { UpdateRepository() }
    single { ApkDownloader() }
    viewModel {
        UpdateViewModel(
            configRepo = get(),
            repo = get(),
            downloader = get(),
            dispatchers = get()
        )
    }
}

val roomModule = module {
    single { AppDatabase.db }
    single { get<AppDatabase>().tabsDao() }
    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().favoritesDao() }
    single { get<AppDatabase>().hostsDao() }
    single { get<AppDatabase>().downloadsDao() }
}

val dataStoreModule = module {
    single { provideTvBroDataStore(androidContext()) }
    single { ConfigRepository(get(), get()) }
}

val tabsModule = module {
    single<TabsRepository> { RoomTabsRepository(get()) }
    viewModel { TabsViewModel(repo = get(), config = get(), dispatchers = get()) }
}

val browserBusModule = module {
    single { BrowserCommandBus() }
}

val favoritesUiModule = module {
    viewModel { FavoritesViewModel(repo = get(), dispatchers = get()) }
    viewModel { HomePageSlotEditorViewModel(repo = get(), dispatchers = get()) }
    viewModel { BookmarkEditorViewModel(repo = get(), dispatchers = get()) }
}

val shortcutsModule = module {
    single { ShortcutMgr.getInstance() }
    single { ShortcutCaptureController() }
}

val dispatchersModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}



val browserDataModule = module {
    single<HistoryRepository> { RoomHistoryRepository(get()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get()) }
    single { RecommendationsRepository() }
    viewModel {
        BrowserDataViewModel(
            config = get(),
            historyRepo = get(),
            favoritesRepo = get(),
            recommendationsRepo = get(),
            dispatchers = get()
        )
    }
}

val adblockModule = module {
    single {
        AdblockRepository(
            appFilesDir = androidContext().filesDir,
            config = get(),
            configRepo = get(),
            dispatchers = get()
        )
    }
}

val downloadsModule = module {
    single<DownloadsRepository> { RoomDownloadsRepository(get()) }
    viewModel { DownloadsViewModel(repo = get(), dispatchers = get()) }
}

val historyUiModule = module {
    viewModel { HistoryViewModel(repo = get(), dispatchers = get()) }
}