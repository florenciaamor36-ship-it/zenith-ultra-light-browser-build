package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Factory for creating and fine-tuning Android WebView instances.
 *
 * Performance Tuning & Battery Optimizations:
 * 1. Hardware Acceleration: Enables GPU compositing (`LAYER_TYPE_HARDWARE`) for 60fps scrolling
 *    while offloading layout work from the CPU.
 * 2. Smart Disk Cache: Configures `LOAD_DEFAULT` caching on disk to avoid re-requesting assets
 *    over cellular/Wi-Fi radios, substantially reducing battery consumption.
 * 3. Restricts Auto-Playing Media: `mediaPlaybackRequiresUserGesture = true` prevents unprompted
 *    background video rendering and audio playback from draining battery.
 * 4. Popup & Pre-raster Throttling: `offscreenPreRaster = false` saves GPU memory; rogue popups blocked.
 * 5. Aggressive Teardown: Provides `destroyWebViewSafely()` to detach from parent, stop timers,
 *    clear references, and prevent memory leaks.
 */
object WebViewFactory {

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        isIncognito: Boolean = false,
        isDesktop: Boolean = false,
        isForceDark: Boolean = false,
        textZoom: Int = 100
    ): WebView {
        // Use activity context for UI views but ensure proper lifecycle teardown
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            isScrollbarFadingEnabled = true
        }

        configureSettings(webView.settings, isIncognito, isDesktop, isForceDark, textZoom)

        // Incognito cookie isolation
        if (isIncognito) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(webView, false)
            }
        } else {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
        }

        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(
        settings: WebSettings,
        isIncognito: Boolean,
        isDesktop: Boolean,
        isForceDark: Boolean,
        textZoom: Int
    ) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = !isIncognito
            databaseEnabled = !isIncognito
            setGeolocationEnabled(!isIncognito)
            saveFormData = !isIncognito

            // Battery saving: prevent auto-playing background media & unsolicited popups
            mediaPlaybackRequiresUserGesture = true
            javaScriptCanOpenWindowsAutomatically = false

            // Smooth zoom support
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Responsive viewport
            useWideViewPort = true
            loadWithOverviewMode = true

            // Low-RAM caching: offload to disk cache rather than holding massive bitmaps in RAM
            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

            // Text zoom accessibility
            this.textZoom = textZoom

            // Desktop vs Mobile User-Agent
            if (isDesktop) {
                userAgentString = DESKTOP_USER_AGENT
            }

            // Mixed content security
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // RAM optimization: do not pre-rasterize offscreen layers until visible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                offscreenPreRaster = false
            }
        }

        // Smart Dark Mode (WebSettingsCompat)
        applyDarkMode(settings, isForceDark)
    }

    /**
     * Applies Force Dark mode to web content using AndroidX WebKit.
     */
    fun applyDarkMode(settings: WebSettings, isDarkMode: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val mode = if (isDarkMode) {
                WebSettingsCompat.FORCE_DARK_ON
            } else {
                WebSettingsCompat.FORCE_DARK_OFF
            }
            WebSettingsCompat.setForceDark(settings, mode)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                settings,
                WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
            )
        }
    }

    /**
     * Complete and safe destruction of a WebView instance to prevent native memory leaks.
     */
    fun destroyWebViewSafely(webView: WebView?, clearBrowsingData: Boolean = false) {
        if (webView == null) return
        try {
            webView.stopLoading()
            webView.onPause()
            webView.pauseTimers()
            (webView.parent as? ViewGroup)?.removeView(webView)
            if (clearBrowsingData) {
                webView.clearHistory()
                webView.clearCache(true)
            }
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            webView.destroy()
        } catch (_: Exception) {
            // Ignored to guarantee safety during teardown
        }
    }
}
