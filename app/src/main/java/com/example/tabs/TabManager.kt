package com.example.tabs

import android.content.Context
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.example.webview.WebViewFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Advanced Tab Manager for AeroWeb.
 *
 * Performance Tuning & Battery Optimizations:
 * 1. Independent Tab State: Each tab isolates its own back/forward stack and lifecycle.
 * 2. Inactive Tab Throttling: When switching away from a tab, `webView.onPause()` is invoked
 *    to halt JavaScript execution loops, CSS animations, and render threads.
 * 3. Aggressive onTrimMemory(): Destroys background WebViews or clears their memory caches
 *    under high RAM pressure while preserving the URL and scroll state for lazy reconstruction.
 * 4. Incognito Data Purge: Complete erasure of cookies, DOM storage, history, and cache when
 *    incognito tabs are closed.
 */
class TabManager(private val context: Context) {
    private val statePrefs = context.getSharedPreferences("aero_web_state", Context.MODE_PRIVATE)

    private val _tabs = MutableStateFlow<List<WebTab>>(emptyList())
    val tabs: StateFlow<List<WebTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: WebTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    /**
     * Creates a new web tab.
     */
    fun createTab(
        url: String = "https://www.google.com",
        isIncognito: Boolean = false,
        isForceDark: Boolean = false
    ): WebTab {
        val initialUrl = if (!isIncognito && url == "https://www.google.com") statePrefs.getString("last_url", url) ?: url else url
        val newTab = WebTab(
            id = UUID.randomUUID().toString(),
            title = if (isIncognito) "Incognito Tab" else "New Tab",
            url = initialUrl,
            isIncognito = isIncognito,
            isDarkModeEnabled = isForceDark
        )

        // Pause current active tab webView before switching
        activeTab?.webView?.onPause()

        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        return newTab
    }

    /**
     * Switches to an existing tab.
     */
    fun switchTab(tabId: String) {
        if (_activeTabId.value == tabId) return

        // Pause previous tab
        activeTab?.webView?.onPause()

        _activeTabId.value = tabId

        // Resume new tab
        activeTab?.webView?.onResume()
    }

    /**
     * Closes a tab and completely cleans up its WebView resources.
     */
    fun closeTab(tabId: String, context: Context) {
        val tabToClose = _tabs.value.find { it.id == tabId } ?: return
        val currentTabs = _tabs.value
        val index = currentTabs.indexOf(tabToClose)

        // Destroy WebView and clean native memory
        WebViewFactory.destroyWebViewSafely(tabToClose.webView, clearBrowsingData = tabToClose.isIncognito)
        tabToClose.webView = null

        // If incognito, execute aggressive privacy & storage purge
        if (tabToClose.isIncognito) {
            purgeIncognitoData()
        }

        val updatedTabs = currentTabs.filter { it.id != tabId }
        _tabs.value = updatedTabs

        // If we closed the active tab, switch to adjacent or create a new one
        if (_activeTabId.value == tabId) {
            if (updatedTabs.isNotEmpty()) {
                val nextIndex = (index - 1).coerceAtLeast(0)
                val newActive = updatedTabs[nextIndex]
                _activeTabId.value = newActive.id
                newActive.webView?.onResume()
            } else {
                // Keep at least one default tab
                createTab(url = "https://www.google.com")
            }
        }
    }

    /**
     * Closes all tabs or incognito tabs.
     */
    fun closeAllTabs(incognitoOnly: Boolean = false, context: Context) {
        val tabsToClose = if (incognitoOnly) {
            _tabs.value.filter { it.isIncognito }
        } else {
            _tabs.value
        }

        tabsToClose.forEach { tab ->
            WebViewFactory.destroyWebViewSafely(tab.webView, clearBrowsingData = tab.isIncognito)
            tab.webView = null
        }

        if (incognitoOnly) {
            purgeIncognitoData()
            val remaining = _tabs.value.filterNot { it.isIncognito }
            _tabs.value = remaining
            if (remaining.isNotEmpty()) {
                if (activeTab?.isIncognito == true) {
                    _activeTabId.value = remaining.first().id
                    remaining.first().webView?.onResume()
                }
            } else {
                createTab()
            }
        } else {
            purgeIncognitoData()
            _tabs.value = emptyList()
            createTab()
        }
    }

    /**
     * Updates an attribute of a specific tab.
     */
    fun updateTab(tabId: String, transform: (WebTab) -> WebTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    fun updateProgress(tabId: String, progress: Int) {
        updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
    }

    fun updateTitleAndUrl(tabId: String, title: String?, url: String?) {
        updateTab(tabId) { current ->
            current.copy(
                title = if (!title.isNullOrBlank()) title else current.title,
                url = if (!url.isNullOrBlank()) url else current.url
            ).also { updated ->
                if (!updated.isIncognito && !updated.url.startsWith("about:") && !updated.url.startsWith("data:")) {
                    statePrefs.edit().putString("last_url", updated.url).apply()
                }
            }
        }
    }

    fun updateFavicon(tabId: String, favicon: Bitmap?) {
        updateTab(tabId) { it.copy(favicon = favicon) }
    }

    fun updateNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    /**
     * Memory optimization handler called on system onTrimMemory.
     */
    fun onTrimMemory(level: Int) {
        val currentActiveId = _activeTabId.value

        _tabs.value.forEach { tab ->
            if (tab.id != currentActiveId) {
                // Background tab: aggressively reduce memory footprint
                tab.webView?.let { wv ->
                    wv.onPause()
                    wv.pauseTimers()
                    wv.clearCache(false)

                    // Under critical memory pressure, destroy background WebViews
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                        level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE
                    ) {
                        WebViewFactory.destroyWebViewSafely(wv, clearBrowsingData = false)
                        tab.webView = null
                    }
                }
            }
        }
    }

    private fun purgeIncognitoData() {
        try {
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeSessionCookies(null)
            CookieManager.getInstance().flush()
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
