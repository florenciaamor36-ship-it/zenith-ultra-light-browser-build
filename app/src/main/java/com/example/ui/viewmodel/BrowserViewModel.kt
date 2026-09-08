package com.example.ui.viewmodel

import android.app.Application
import android.net.http.SslError
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AeroWebApplication
import com.example.adblock.AdBlocker
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.tabs.WebTab
import com.example.util.BrowserTools
import com.example.util.SearchEngine
import com.example.webview.WebViewFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JsAlertState(val message: String, val result: JsResult)
data class JsConfirmState(val message: String, val result: JsResult)
data class JsPromptState(val message: String, val defaultValue: String, val result: JsPromptResult)
data class GeolocationState(val origin: String, val callback: GeolocationPermissions.Callback)
data class SslErrorState(val handler: SslErrorHandler, val error: SslError)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AeroWebApplication
    val repository = app.repository
    val tabManager = app.tabManager

    // Tabs
    val tabs: StateFlow<List<WebTab>> = tabManager.tabs
    val activeTabId: StateFlow<String> = tabManager.activeTabId

    // URL Bar & Search
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isEditingUrl = MutableStateFlow(false)
    val isEditingUrl: StateFlow<String> = _urlInput.asStateFlow()

    // Find in Page
    private val _isFindInPageActive = MutableStateFlow(false)
    val isFindInPageActive: StateFlow<Boolean> = _isFindInPageActive.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _findMatchIndex = MutableStateFlow(0)
    val findMatchIndex: StateFlow<Int> = _findMatchIndex.asStateFlow()

    private val _findTotalMatches = MutableStateFlow(0)
    val findTotalMatches: StateFlow<Int> = _findTotalMatches.asStateFlow()

    // Settings
    private val _isAdBlockerEnabled = MutableStateFlow(AdBlocker.isEnabled)
    val isAdBlockerEnabled: StateFlow<Boolean> = _isAdBlockerEnabled.asStateFlow()

    private val _searchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    // La barra de navegación queda arriba por defecto, como en un navegador tradicional.
    private val _isBottomToolbar = MutableStateFlow(false)
    val isBottomToolbar: StateFlow<Boolean> = _isBottomToolbar.asStateFlow()

    // Sheet / Dialog Visibility
    val isTabSwitcherOpen = MutableStateFlow(false)
    val isMenuOpen = MutableStateFlow(false)
    val isHistoryOpen = MutableStateFlow(false)
    val isBookmarksOpen = MutableStateFlow(false)
    val isDownloadsOpen = MutableStateFlow(false)
    val isQrScannerOpen = MutableStateFlow(false)
    val isSettingsOpen = MutableStateFlow(false)
    val isAboutOpen = MutableStateFlow(false)

    // JS and System Dialogs
    val jsAlert = MutableStateFlow<JsAlertState?>(null)
    val jsConfirm = MutableStateFlow<JsConfirmState?>(null)
    val jsPrompt = MutableStateFlow<JsPromptState?>(null)
    val geolocationPrompt = MutableStateFlow<GeolocationState?>(null)
    val sslErrorPrompt = MutableStateFlow<SslErrorState?>(null)

    // Fullscreen Custom Video View
    val customVideoView = MutableStateFlow<View?>(null)
    var customVideoCallback: WebChromeClient.CustomViewCallback? = null

    // Room Database Streams
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Ensure at least one tab is created on startup
        if (tabManager.tabs.value.isEmpty()) {
            tabManager.createTab("https://www.google.com")
        }
    }

    // --- Tab Operations ---
    fun openNewTab(url: String = "https://www.google.com", isIncognito: Boolean = false) {
        val tab = tabManager.createTab(url, isIncognito)
        isTabSwitcherOpen.value = false
    }

    fun selectTab(tabId: String) {
        tabManager.switchTab(tabId)
        isTabSwitcherOpen.value = false
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId, getApplication())
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        tabManager.closeAllTabs(incognitoOnly, getApplication())
        isTabSwitcherOpen.value = false
    }

    // --- Navigation ---
    fun loadUrl(urlOrQuery: String, webView: WebView?) {
        val targetUrl = BrowserTools.formatInputToUrl(urlOrQuery, _searchEngine.value)
        _urlInput.value = targetUrl
        val currentTabId = tabManager.activeTabId.value
        tabManager.updateTitleAndUrl(currentTabId, null, targetUrl)
        webView?.loadUrl(targetUrl)
    }

    fun reloadCurrentPage(webView: WebView?) {
        webView?.reload()
    }

    fun goBack(webView: WebView?) {
        if (webView?.canGoBack() == true) {
            webView.goBack()
        }
    }

    fun goForward(webView: WebView?) {
        if (webView?.canGoForward() == true) {
            webView.goForward()
        }
    }

    fun stopLoading(webView: WebView?) {
        webView?.stopLoading()
    }

    // --- Features & Toggles ---
    fun toggleAdBlocker() {
        val newValue = !_isAdBlockerEnabled.value
        _isAdBlockerEnabled.value = newValue
        AdBlocker.isEnabled = newValue
        // Interception only affects future requests. Reload so the page can
        // recover immediately when the setting is changed.
        tabManager.activeTab?.webView?.reload()
    }

    fun toggleDarkMode(webView: WebView?) {
        val tab = tabManager.activeTab ?: return
        val newDark = !tab.isDarkModeEnabled
        tabManager.updateTab(tab.id) { it.copy(isDarkModeEnabled = newDark) }
        webView?.let { wv ->
            WebViewFactory.applyDarkMode(wv.settings, newDark)
            wv.reload()
        }
    }

    fun toggleDesktopMode(webView: WebView?) {
        val tab = tabManager.activeTab ?: return
        val newDesktop = !tab.isDesktopMode
        tabManager.updateTab(tab.id) { it.copy(isDesktopMode = newDesktop) }
        webView?.let { wv ->
            wv.settings.userAgentString = if (newDesktop) {
                WebViewFactory.DESKTOP_USER_AGENT
            } else {
                null // Reverts to system default
            }
            wv.reload()
        }
    }

    fun setTextZoom(zoomPercent: Int, webView: WebView?) {
        val tab = tabManager.activeTab ?: return
        val safeZoom = zoomPercent.coerceIn(50, 200)
        tabManager.updateTab(tab.id) { it.copy(textZoom = safeZoom) }
        webView?.settings?.textZoom = safeZoom
    }

    fun toggleToolbarPosition() {
        _isBottomToolbar.value = !_isBottomToolbar.value
    }

    fun setSearchEngine(engine: SearchEngine) {
        _searchEngine.value = engine
    }

    // --- Find in Page ---
    fun startFindInPage() {
        _isFindInPageActive.value = true
        _findQuery.value = ""
        _findMatchIndex.value = 0
        _findTotalMatches.value = 0
    }

    fun updateFindQuery(query: String, webView: WebView?) {
        _findQuery.value = query
        if (webView != null) {
            if (query.isNotBlank()) {
                webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                    _findMatchIndex.value = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                    _findTotalMatches.value = numberOfMatches
                }
                webView.findAllAsync(query)
            } else {
                webView.clearMatches()
                _findMatchIndex.value = 0
                _findTotalMatches.value = 0
            }
        }
    }

    fun findNextMatch(forward: Boolean, webView: WebView?) {
        webView?.findNext(forward)
    }

    fun closeFindInPage(webView: WebView?) {
        _isFindInPageActive.value = false
        _findQuery.value = ""
        webView?.clearMatches()
    }

    // --- Bookmarks & History ---
    fun toggleBookmarkCurrentPage(title: String, url: String) {
        viewModelScope.launch {
            repository.toggleBookmark(title, url)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmarkById(id)
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearDownloads()
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.deleteDownloadById(id)
        }
    }

    // --- Fullscreen Video Controls ---
    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        customVideoView.value = view
        customVideoCallback = callback
    }

    fun hideCustomView() {
        customVideoCallback?.onCustomViewHidden()
        customVideoCallback = null
        customVideoView.value = null
    }
}
