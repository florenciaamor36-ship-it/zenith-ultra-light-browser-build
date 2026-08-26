package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an intercepted or active download.
 */
@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["downloadId"]),
        Index(value = ["timestamp"])
    ]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadId: Long,
    val fileName: String,
    val url: String,
    val filePath: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val status: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
