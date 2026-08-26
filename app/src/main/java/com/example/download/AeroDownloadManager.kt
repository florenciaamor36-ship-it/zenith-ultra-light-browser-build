package com.example.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Advanced Native Download Manager for AeroWeb.
 *
 * Performance Tuning & Battery Optimizations:
 * 1. Delegates large payload transfers to Android OS native `DownloadManager` which optimizes
 *    network batching, handles automatic pause/resume, and respects OS battery saver constraints.
 * 2. Asynchronously logs download history into Room Database without blocking UI thread.
 */
class AeroDownloadManager(
    private val context: Context,
    private val repository: BrowserRepository,
    private val coroutineScope: CoroutineScope
) {

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    /**
     * Intercepts and schedules a file download.
     */
    fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        if (downloadManager == null) {
            Toast.makeText(context, "Download service unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val guessedFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val uri = Uri.parse(url)

            val request = DownloadManager.Request(uri).apply {
                setTitle(guessedFileName)
                setDescription("Downloading file via AeroWeb...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessedFileName)

                // Pass existing cookies & User-Agent for authenticated downloads
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }
                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                if (!mimeType.isNullOrBlank()) {
                    setMimeType(mimeType)
                }
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Download started: $guessedFileName", Toast.LENGTH_SHORT).show()

            // Record into database
            coroutineScope.launch(Dispatchers.IO) {
                repository.recordDownload(
                    downloadId = downloadId,
                    fileName = guessedFileName,
                    url = url,
                    mimeType = mimeType,
                    fileSize = contentLength.coerceAtLeast(0L)
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Download error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
