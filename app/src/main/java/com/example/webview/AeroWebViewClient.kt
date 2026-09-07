package com.example.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.adblock.AdBlocker
import com.example.data.repository.BrowserRepository
import com.example.tabs.TabManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * High-Efficiency WebViewClient for AeroWeb.
 *
 * Performance Tuning & Battery Optimizations:
 * 1. Low-overhead Request Interception: Intercepts ad/tracker network requests at the native C++
 *    layer before TCP sockets or DNS requests are dispatched, saving radio battery & RAM.
 * 2. Asynchronous History Logging: Offloads database writes to Kotlin Coroutines on Dispatchers.IO.
 * 3. Deep Link Routing: Seamlessly delegates `tel:`, `mailto:`, `market:`, and custom schemes
 *    to native Android activities without stalling the web rendering pipeline.
 */
class AeroWebViewClient(
    private val context: Context,
    private val tabId: String,
    private val isIncognito: Boolean,
    private val tabManager: TabManager,
    private val repository: BrowserRepository,
    private val coroutineScope: CoroutineScope,
    private val onSslErrorPrompt: ((SslErrorHandler, SslError) -> Unit)? = null
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val uri = request?.url ?: return null
        if (AdBlocker.shouldBlock(uri)) {
            // Drop ad/tracker instantly without firing network radio
            return AdBlocker.createEmptyResponse()
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        // Standard HTTP / HTTPS navigation stays in WebView
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "javascript") {
            return false
        }

        // External protocols (tel, mailto, maps, intent, market)
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            true // Handled to avoid crash
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        tabManager.updateTitleAndUrl(tabId, null, url)
        tabManager.updateProgress(tabId, 10)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val canGoBack = view?.canGoBack() ?: false
        val canGoForward = view?.canGoForward() ?: false
        tabManager.updateNavigationState(tabId, canGoBack, canGoForward)
        tabManager.updateProgress(tabId, 100)

        // Cosmetic protection for ads rendered by the page itself (including common YouTube overlays).
        view?.evaluateJavascript("""(function(){
            const s='[id*=ad],[class*=ad-],[class*=ads-],[class*=advert],[class*=sponsor],[class*=promo],.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-text';
            document.querySelectorAll(s).forEach(function(e){e.remove();});
            document.querySelectorAll('.video-ads,.ytp-ad-player-overlay').forEach(function(e){e.style.display='none';});
            const skip=document.querySelector('.ytp-ad-skip-button,.ytp-skip-ad-button'); if(skip) skip.click();
        })();""", null)
        val title = view?.title
        if (!url.isNullOrBlank()) {
            tabManager.updateTitleAndUrl(tabId, title, url)

            // Persist to history database only if NOT in incognito
            if (!isIncognito) {
                coroutineScope.launch(Dispatchers.IO) {
                    repository.recordHistoryVisit(title ?: url, url)
                }
            }
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        if (handler != null && error != null && onSslErrorPrompt != null) {
            onSslErrorPrompt.invoke(handler, error)
        } else {
            // Default safe behavior: cancel insecure connection
            handler?.cancel()
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
    }
}
