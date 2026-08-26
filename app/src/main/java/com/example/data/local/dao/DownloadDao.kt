package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for downloaded files and active download monitoring.
 */
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, fileSize = :fileSize WHERE downloadId = :downloadId")
    suspend fun updateDownloadStatus(downloadId: Long, status: Int, filePath: String?, fileSize: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
