package org.mlm.tvbrwser.data

import org.mlm.tvbrwser.model.FavoriteItem
import org.mlm.tvbrwser.model.dao.FavoritesDao

class RoomFavoritesRepository(private val dao: FavoritesDao) : FavoritesRepository {
    override suspend fun getAll(homePageBookmarks: Boolean) = dao.getAll(homePageBookmarks)
    override suspend fun getHomePageBookmarks() = dao.getHomePageBookmarks()
    override suspend fun getById(id: Long) = dao.getById(id)

    override suspend fun insert(item: FavoriteItem) = dao.insert(item)
    override suspend fun update(item: FavoriteItem) = dao.update(item)
    override suspend fun delete(item: FavoriteItem) = dao.delete(item)
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun markAsUseful(favoriteId: Long) = dao.markAsUseful(favoriteId)
}
