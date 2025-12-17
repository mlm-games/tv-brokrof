package org.mlm.tvbrwser.data

import org.mlm.tvbrwser.model.Download

interface DownloadsRepository {
    suspend fun page(offset: Long): List<Download>
    suspend fun delete(download: Download)
}