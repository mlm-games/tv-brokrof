package org.mlm.tvbrwser.data

import org.mlm.tvbrwser.model.FavoriteItem

interface FavoritesRepository {
    suspend fun getAll(homePageBookmarks: Boolean = false): List<FavoriteItem>
    suspend fun getHomePageBookmarks(): List<FavoriteItem>
    suspend fun getById(id: Long): FavoriteItem?

    suspend fun insert(item: FavoriteItem): Long
    suspend fun update(item: FavoriteItem)
    suspend fun delete(item: FavoriteItem)
    suspend fun delete(id: Long)
    suspend fun markAsUseful(favoriteId: Long)
}