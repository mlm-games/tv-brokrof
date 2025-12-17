package org.mlm.tvbrwser.data.tabs

import org.mlm.tvbrwser.model.WebTabState

interface TabsRepository {
    suspend fun loadTabs(incognito: Boolean): List<WebTabState>
    suspend fun insert(tab: WebTabState): Long
    suspend fun update(tab: WebTabState)
    suspend fun delete(tab: WebTabState)
    suspend fun deleteAll(incognito: Boolean)
    suspend fun unselectAll(incognito: Boolean)
    suspend fun updatePositions(tabs: List<WebTabState>)
}