package org.mlm.tvbrwser.singleton

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.mlm.tvbrwser.TVBro
import org.mlm.tvbrwser.model.*
import org.mlm.tvbrwser.model.dao.*
import org.mlm.tvbrwser.model.util.Converters

@Database(
    entities = [
        WebTabState::class,
        HistoryItem::class,
        Download::class,
        FavoriteItem::class,
        HostConfig::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabsDao(): TabsDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadsDao(): DownloadDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun hostsDao(): HostsDao

    companion object {
        val db: AppDatabase by lazy {
            Room.databaseBuilder(
                TVBro.instance,
                AppDatabase::class.java,
                "main.db"
            )
                .build()
        }
    }
}