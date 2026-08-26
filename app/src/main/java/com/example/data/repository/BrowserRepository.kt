package com.example.data.repository

import com.example.data.local.BrowserDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository layer for database operations.
 * Isolates data management from the UI and ViewModel.
 * All write/update transactions run off the main thread (Dispatchers.IO).
 */
class BrowserRepository(private val database: BrowserDatabase) {

    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val downloadDao = database.downloadDao()

    // --- Bookmarks ---
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun isBookmarked(url: String): Flow<Boolean> = bookmarkDao.isBookmarked(url)

    suspend fun addBookmark(title: String, url: String) = withContext(Dispatchers.IO) {
        val cleanTitle = title.ifBlank { url }
        bookmarkDao.insert(BookmarkEntity(title = cleanTitle, url = url))
    }

    suspend fun removeBookmarkByUrl(url: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteByUrl(url)
    }

    suspend fun deleteBookmarkById(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteById(id)
    }

    suspend fun toggleBookmark(title: String, url: String): Boolean = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkByUrl(url)
        if (existing != null) {
            bookmarkDao.deleteById(existing.id)
            false
        } else {
            val cleanTitle = title.ifBlank { url }
            bookmarkDao.insert(BookmarkEntity(title = cleanTitle, url = url))
            true
        }
    }

    // --- History ---
    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecentHistory()

    fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)

    suspend fun recordHistoryVisit(title: String, url: String) = withContext(Dispatchers.IO) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:") || url.startsWith("javascript:")) {
            return@withContext
        }
        val cleanTitle = title.ifBlank { url }
        val updatedRows = historyDao.updateVisit(url, cleanTitle, System.currentTimeMillis())
        if (updatedRows == 0) {
            historyDao.insert(
                HistoryEntity(
                    title = cleanTitle,
                    url = url,
                    timestamp = System.currentTimeMillis(),
                    visitCount = 1
                )
            )
        }
    }

    suspend fun deleteHistoryById(id: Long) = withContext(Dispatchers.IO) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    // --- Downloads ---
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun recordDownload(
        downloadId: Long,
        fileName: String,
        url: String,
        mimeType: String?,
        fileSize: Long
    ) = withContext(Dispatchers.IO) {
        downloadDao.insert(
            DownloadEntity(
                downloadId = downloadId,
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                fileSize = fileSize,
                status = 0
            )
        )
    }

    suspend fun updateDownloadStatus(
        downloadId: Long,
        status: Int,
        filePath: String?,
        fileSize: Long
    ) = withContext(Dispatchers.IO) {
        downloadDao.updateDownloadStatus(downloadId, status, filePath, fileSize)
    }

    suspend fun deleteDownloadById(id: Long) = withContext(Dispatchers.IO) {
        downloadDao.deleteById(id)
    }

    suspend fun clearDownloads() = withContext(Dispatchers.IO) {
        downloadDao.clearAll()
    }
}
