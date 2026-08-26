package com.example.tabs

import android.graphics.Bitmap
import android.webkit.WebView

/**
 * Represents an isolated web browser tab.
 */
data class WebTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "https://www.google.com",
    val favicon: Bitmap? = null,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isAdBlockEnabled: Boolean = true,
    val isDarkModeEnabled: Boolean = false,
    val textZoom: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    @Transient var webView: WebView? = null
)
