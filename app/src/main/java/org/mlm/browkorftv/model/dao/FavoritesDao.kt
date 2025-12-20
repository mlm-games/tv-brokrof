package org.mlm.browkorftv.model.dao

import androidx.room.*
import org.mlm.browkorftv.model.FavoriteItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites WHERE parent = 0 ORDER BY id DESC")
    suspend fun getAll(): List<FavoriteItem>

    @Insert
    suspend fun insert(item: FavoriteItem): Long

    @Update
    suspend fun update(item: FavoriteItem)

    @Delete
    suspend fun delete(item: FavoriteItem)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getById(id: Long): FavoriteItem?

    @Query("UPDATE favorites SET useful = 1 WHERE id = :favoriteId")
    suspend fun markAsUseful(favoriteId: Long)
}