package com.example

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.download.AeroDownloadManager
import com.example.qr.QrScannerSheet
import com.example.tabs.WebTab
import com.example.ui.components.AboutBottomSheet
import com.example.ui.components.BookmarksBottomSheet
import com.example.ui.components.BrowserAddressBar
import com.example.ui.components.BrowserMenuSheet
import com.example.ui.components.DownloadsBottomSheet
import com.example.ui.components.FindInPageBar
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.JsDialogHost
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TabSwitcherSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BrowserViewModel
import com.example.ui.viewmodel.GeolocationState
import com.example.ui.viewmodel.JsAlertState
import com.example.ui.viewmodel.JsConfirmState
import com.example.ui.viewmodel.JsPromptState
import com.example.ui.viewmodel.SslErrorState
import com.example.util.BrowserTools
import com.example.webview.AeroWebChromeClient
import com.example.webview.AeroWebViewClient
import com.example.webview.WebViewFactory

/**
 * AeroWeb - High-Performance Android Web Browser.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var mediaSession: MediaSession? = null
    private val mediaChannelId = "clave_web_media"

    // Activity Result Launcher for WebView File Uploads
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            val results = WebChromeClient.FileChooserParams.parseResult(result.resultCode, intentData)
            fileUploadCallback?.onReceiveValue(results)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupMediaControls()

        // Handle incoming URL intents (from shortcuts or other apps)
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                val tabs by viewModel.tabs.collectAsState()
                val activeTabId by viewModel.activeTabId.collectAsState()
                val isAdBlockerEnabled by viewModel.isAdBlockerEnabled.collectAsState()
                val searchEngine by viewModel.searchEngine.collectAsState()
                val isBottomToolbar by viewModel.isBottomToolbar.collectAsState()

                // Find in Page state
                val isFindInPageActive by viewModel.isFindInPageActive.collectAsState()
                val findQuery by viewModel.findQuery.collectAsState()
                val findMatchIndex by viewModel.findMatchIndex.collectAsState()
                val findTotalMatches by viewModel.findTotalMatches.collectAsState()

                // Sheet visibility
                val isTabSwitcherOpen by viewModel.isTabSwitcherOpen.collectAsState()
                val isMenuOpen by viewModel.isMenuOpen.collectAsState()
                val isHistoryOpen by viewModel.isHistoryOpen.collectAsState()
                val isBookmarksOpen by viewModel.isBookmarksOpen.collectAsState()
                val isDownloadsOpen by viewModel.isDownloadsOpen.collectAsState()
                val isQrScannerOpen by viewModel.isQrScannerOpen.collectAsState()
                val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
                val isAboutOpen by viewModel.isAboutOpen.collectAsState()

                // Database records
                val bookmarks by viewModel.bookmarks.collectAsState()
                val history by viewModel.history.collectAsState()
                val downloads by viewModel.downloads.collectAsState()

                // Active Tab
                val activeTab = remember(tabs, activeTabId) {
                    tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
                }

                val isCurrentBookmarked = remember(bookmarks, activeTab?.url) {
                    bookmarks.any { it.url == activeTab?.url }
                }

                // JS & System dialog states
                val jsAlertState by viewModel.jsAlert.collectAsState()
                val jsConfirmState by viewModel.jsConfirm.collectAsState()
                val jsPromptState by viewModel.jsPrompt.collectAsState()
                val geoState by viewModel.geolocationPrompt.collectAsState()
                val sslState by viewModel.sslErrorPrompt.collectAsState()
                val customVideoView by viewModel.customVideoView.collectAsState()

                val downloadManager = remember {
                    AeroDownloadManager(context, viewModel.repository, coroutineScope)
                }

                // Back Button Interception
                BackHandler(enabled = true) {
                    when {
                        customVideoView != null -> {
                            viewModel.hideCustomView()
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                        isFindInPageActive -> {
                            viewModel.closeFindInPage(activeTab?.webView)
                        }
                        isAboutOpen -> {
                            viewModel.isAboutOpen.value = false
                        }
                        isMenuOpen -> {
                            viewModel.isMenuOpen.value = false
                        }
                        isTabSwitcherOpen -> {
                            viewModel.isTabSwitcherOpen.value = false
                        }
                        isBookmarksOpen -> {
                            viewModel.isBookmarksOpen.value = false
                        }
                        isHistoryOpen -> {
                            viewModel.isHistoryOpen.value = false
                        }
                        isDownloadsOpen -> {
                            viewModel.isDownloadsOpen.value = false
                        }
                        isQrScannerOpen -> {
                            viewModel.isQrScannerOpen.value = false
                        }
                        isSettingsOpen -> {
                            viewModel.isSettingsOpen.value = false
                        }
                        activeTab?.webView?.canGoBack() == true -> {
                            activeTab.webView?.goBack()
                        }
                        tabs.size > 1 -> {
                            viewModel.closeTab(activeTab?.id ?: "")
                        }
                        else -> {
                            finish()
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isBottomToolbar && customVideoView == null) {
                            BrowserAddressBar(
                                tab = activeTab,
                                tabCount = tabs.size,
                                isBottomBar = true,
                                isAdBlockActive = isAdBlockerEnabled,
                                onNavigate = { url -> viewModel.loadUrl(url, activeTab?.webView) },
                                onReload = { viewModel.reloadCurrentPage(activeTab?.webView) },
                                onStop = { viewModel.stopLoading(activeTab?.webView) },
                                onGoBack = { viewModel.goBack(activeTab?.webView) },
                                onGoForward = { viewModel.goForward(activeTab?.webView) },
                                onOpenTabs = { viewModel.isTabSwitcherOpen.value = true },
                                onOpenMenu = { viewModel.isMenuOpen.value = true },
                                onOpenQrScanner = { viewModel.isQrScannerOpen.value = true }
                            )
                        }
                    },
                    topBar = {
                        if (!isBottomToolbar && customVideoView == null) {
                            BrowserAddressBar(
                                tab = activeTab,
                                tabCount = tabs.size,
                                isBottomBar = false,
                                isAdBlockActive = isAdBlockerEnabled,
                                onNavigate = { url -> viewModel.loadUrl(url, activeTab?.webView) },
                                onReload = { viewModel.reloadCurrentPage(activeTab?.webView) },
                                onStop = { viewModel.stopLoading(activeTab?.webView) },
                                onGoBack = { viewModel.goBack(activeTab?.webView) },
                                onGoForward = { viewModel.goForward(activeTab?.webView) },
                                onOpenTabs = { viewModel.isTabSwitcherOpen.value = true },
                                onOpenMenu = { viewModel.isMenuOpen.value = true },
                                onOpenQrScanner = { viewModel.isQrScannerOpen.value = true }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main WebView container or Quick Start Dashboard
                        if (activeTab != null) {
                            key(activeTab.id) {
                                TabWebViewHost(
                                    tab = activeTab,
                                    isAdBlockerActive = isAdBlockerEnabled,
                                    viewModel = viewModel,
                                    downloadManager = downloadManager,
                                    onShowFileChooser = { callback, params ->
                                        fileUploadCallback = callback
                                        try {
                                            val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = "*/*"
                                            }
                                            fileChooserLauncher.launch(intent)
                                            true
                                        } catch (_: Exception) {
                                            fileUploadCallback?.onReceiveValue(null)
                                            fileUploadCallback = null
                                            false
                                        }
                                    },
                                    onOpenUrl = { url -> viewModel.loadUrl(url, activeTab.webView) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Find in Page overlay floating bar
                        AnimatedVisibility(
                            visible = isFindInPageActive,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            FindInPageBar(
                                query = findQuery,
                                matchIndex = findMatchIndex,
                                totalMatches = findTotalMatches,
                                onQueryChange = { q -> viewModel.updateFindQuery(q, activeTab?.webView) },
                                onNextMatch = { viewModel.findNextMatch(true, activeTab?.webView) },
                                onPreviousMatch = { viewModel.findNextMatch(false, activeTab?.webView) },
                                onClose = { viewModel.closeFindInPage(activeTab?.webView) }
                            )
                        }

                        // Fullscreen Custom Video Player Layer
                        if (customVideoView != null) {
                            AndroidView(
                                factory = { customVideoView!! },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            )
                        }
                    }
                }

                // --- Overlays & Bottom Sheets ---

                // 1. Tab Switcher
                if (isTabSwitcherOpen) {
                    TabSwitcherSheet(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { id -> viewModel.selectTab(id) },
                        onCloseTab = { id -> viewModel.closeTab(id) },
                        onNewTab = { incognito -> viewModel.openNewTab(isIncognito = incognito) },
                        onCloseAllTabs = { incognitoOnly -> viewModel.closeAllTabs(incognitoOnly) },
                        onDismiss = { viewModel.isTabSwitcherOpen.value = false }
                    )
                }

                // 2. Main Browser Tools Menu
                if (isMenuOpen) {
                    BrowserMenuSheet(
                        activeTab = activeTab,
                        isBookmarked = isCurrentBookmarked,
                        isAdBlockerActive = isAdBlockerEnabled,
                        onToggleBookmark = {
                            if (activeTab != null) {
                                viewModel.toggleBookmarkCurrentPage(activeTab.title, activeTab.url)
                            }
                        },
                        onToggleAdBlocker = { viewModel.toggleAdBlocker() },
                        onToggleDarkMode = { viewModel.toggleDarkMode(activeTab?.webView) },
                        onToggleDesktopMode = { viewModel.toggleDesktopMode(activeTab?.webView) },
                        onSetTextZoom = { zoom -> viewModel.setTextZoom(zoom, activeTab?.webView) },
                        onNewTab = { viewModel.openNewTab(isIncognito = false) },
                        onNewIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                        onOpenHistory = { viewModel.isHistoryOpen.value = true },
                        onOpenBookmarks = { viewModel.isBookmarksOpen.value = true },
                        onOpenDownloads = { viewModel.isDownloadsOpen.value = true },
                        onFindInPage = { viewModel.startFindInPage() },
                        onTranslate = {
                            if (activeTab != null) {
                                BrowserTools.translatePage(activeTab.webView, activeTab.url)
                            }
                        },
                        onSavePdf = {
                            if (activeTab != null) {
                                BrowserTools.savePageAsPdf(context, activeTab.webView, activeTab.title)
                            }
                        },
                        onAddShortcut = {
                            if (activeTab != null) {
                                BrowserTools.addShortcutToHomeScreen(context, activeTab.title, activeTab.url, activeTab.favicon)
                            }
                        },
                        onShare = {
                            if (activeTab != null) {
                                BrowserTools.shareUrl(context, activeTab.title, activeTab.url)
                            }
                        },
                        onOpenSettings = { viewModel.isSettingsOpen.value = true },
                        onOpenAbout = { viewModel.isAboutOpen.value = true },
                        onDismiss = { viewModel.isMenuOpen.value = false }
                    )
                }

                // 3. Bookmarks Sheet
                if (isBookmarksOpen) {
                    BookmarksBottomSheet(
                        bookmarks = bookmarks,
                        onSelectBookmark = { url -> viewModel.loadUrl(url, activeTab?.webView) },
                        onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                        onDismiss = { viewModel.isBookmarksOpen.value = false }
                    )
                }

                // 4. History Sheet
                if (isHistoryOpen) {
                    HistoryBottomSheet(
                        historyList = history,
                        onSelectHistory = { url -> viewModel.loadUrl(url, activeTab?.webView) },
                        onDeleteHistory = { id -> viewModel.deleteHistoryEntry(id) },
                        onClearAllHistory = { viewModel.clearAllHistory() },
                        onDismiss = { viewModel.isHistoryOpen.value = false }
                    )
                }

                // 5. Downloads Sheet
                if (isDownloadsOpen) {
                    DownloadsBottomSheet(
                        downloads = downloads,
                        onDeleteDownload = { id -> viewModel.deleteDownload(id) },
                        onClearAll = { viewModel.clearAllDownloads() },
                        onDismiss = { viewModel.isDownloadsOpen.value = false }
                    )
                }

                // 6. QR Code Scanner Sheet
                if (isQrScannerOpen) {
                    QrScannerSheet(
                        onDismiss = { viewModel.isQrScannerOpen.value = false },
                        onQrCodeDetected = { code ->
                            viewModel.loadUrl(code, activeTab?.webView)
                        }
                    )
                }

                // 7. Settings Dialog
                if (isSettingsOpen) {
                    SettingsDialog(
                        currentSearchEngine = searchEngine,
                        isBottomToolbar = isBottomToolbar,
                        isAdBlockerActive = isAdBlockerEnabled,
                        onSelectSearchEngine = { engine -> viewModel.setSearchEngine(engine) },
                        onToggleToolbarPosition = { viewModel.toggleToolbarPosition() },
                        onToggleAdBlocker = { viewModel.toggleAdBlocker() },
                        onOpenAbout = { viewModel.isAboutOpen.value = true },
                        onDismiss = { viewModel.isSettingsOpen.value = false }
                    )
                }

                // 8. About & Legal Sheet
                if (isAboutOpen) {
                    AboutBottomSheet(
                        onDismiss = { viewModel.isAboutOpen.value = false }
                    )
                }

                // 9. JS Alerts & Geolocation & SSL Dialogs
                JsDialogHost(
                    alertState = jsAlertState,
                    confirmState = jsConfirmState,
                    promptState = jsPromptState,
                    geolocationState = geoState,
                    sslErrorState = sslState,
                    onDismissAlert = { viewModel.jsAlert.value = null },
                    onDismissConfirm = { viewModel.jsConfirm.value = null },
                    onDismissPrompt = { viewModel.jsPrompt.value = null },
                    onDismissGeo = { viewModel.geolocationPrompt.value = null },
                    onDismissSsl = { viewModel.sslErrorPrompt.value = null }
                )
            }
        }
    }

    private fun setupMediaControls() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(mediaChannelId, "Reproducción multimedia", NotificationManager.IMPORTANCE_LOW))
        }
        mediaSession = MediaSession(this, "ClaveWebMedia").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { viewModel.tabManager.activeTab?.webView?.evaluateJavascript("document.querySelectorAll('video,audio').forEach(v=>v.play())", null) }
                override fun onPause() { viewModel.tabManager.activeTab?.webView?.evaluateJavascript("document.querySelectorAll('video,audio').forEach(v=>v.pause())", null) }
            })
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE).setState(PlaybackState.STATE_PAUSED, 0L, 1f).build())
            isActive = true
        }
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onPause() {
        // No pausar el WebView activo: permite que el audio iniciado por el usuario continúe
        // cuando la app queda en segundo plano o se apaga la pantalla.
        viewModel.tabManager.activeTab?.webView?.onResume()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.tabManager.activeTab?.webView?.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) {
            val urlString = uri.toString()
            viewModel.openNewTab(url = urlString, isIncognito = false)
        }
    }
}

/**
 * AndroidView host wrapping the Tab's WebView.
 */
@Composable
fun TabWebViewHost(
    tab: WebTab,
    isAdBlockerActive: Boolean,
    viewModel: BrowserViewModel,
    downloadManager: AeroDownloadManager,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Render Webview
    AndroidView(
        modifier = modifier.testTag("tab_webview_${tab.id}"),
        factory = { ctx ->
            val webView = tab.webView ?: WebViewFactory.createWebView(
                context = ctx,
                isIncognito = tab.isIncognito,
                isDesktop = tab.isDesktopMode,
                isForceDark = tab.isDarkModeEnabled,
                textZoom = tab.textZoom
            ).also { tab.webView = it }

            webView.webViewClient = AeroWebViewClient(
                context = ctx,
                tabId = tab.id,
                isIncognito = tab.isIncognito,
                tabManager = viewModel.tabManager,
                repository = viewModel.repository,
                coroutineScope = coroutineScope,
                onSslErrorPrompt = { handler, error ->
                    viewModel.sslErrorPrompt.value = SslErrorState(handler, error)
                }
            )

            webView.webChromeClient = AeroWebChromeClient(
                tabId = tab.id,
                tabManager = viewModel.tabManager,
                onShowCustomViewCallback = { view, callback ->
                    (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    viewModel.showCustomView(view, callback)
                },
                onHideCustomViewCallback = {
                    viewModel.hideCustomView()
                    (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                },
                onGeolocationPrompt = { origin, callback ->
                    viewModel.geolocationPrompt.value = GeolocationState(origin, callback)
                },
                onShowFileChooserCallback = onShowFileChooser,
                onJsAlertCallback = { _, message, result ->
                    viewModel.jsAlert.value = JsAlertState(message, result)
                    true
                },
                onJsConfirmCallback = { _, message, result ->
                    viewModel.jsConfirm.value = JsConfirmState(message, result)
                    true
                },
                onJsPromptCallback = { _, message, defaultVal, result ->
                    viewModel.jsPrompt.value = JsPromptState(message, defaultVal, result)
                    true
                }
            )

            webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                downloadManager.enqueueDownload(url, userAgent, contentDisposition, mimetype, contentLength)
            }

            if (tab.url.isNotBlank()) {
                webView.loadUrl(tab.url)
            }

            webView
        },
        update = { webView ->
            tab.webView = webView
            // Settings update
            WebViewFactory.configureSettings(
                webView.settings,
                tab.isIncognito,
                tab.isDesktopMode,
                tab.isDarkModeEnabled,
                tab.textZoom
            )
        }
    )
}
