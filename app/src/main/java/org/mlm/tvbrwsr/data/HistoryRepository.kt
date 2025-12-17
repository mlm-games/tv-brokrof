package org.mlm.tvbrwser.data.history

import org.mlm.tvbrwser.model.HistoryItem

interface HistoryRepository {
    suspend fun count(): Int
    suspend fun last(limit: Int = 1): List<HistoryItem>
    suspend fun insert(item: HistoryItem): Long
    suspend fun updateTitle(id: Long, title: String)
    suspend fun deleteWhereTimeLessThan(time: Long)
    suspend fun frequentlyUsedUrls(): List<HistoryItem>
    suspend fun delete(vararg items: HistoryItem)
    suspend fun clearAll()
    suspend fun page(offset: Long): List<HistoryItem>
    suspend fun search(query: String): List<HistoryItem>
}