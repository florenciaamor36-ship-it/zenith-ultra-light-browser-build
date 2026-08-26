package com.example.webview

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.example.tabs.TabManager

/**
 * Robust WebChromeClient for AeroWeb.
 *
 * Supports:
 * - Fullscreen video playback (`onShowCustomView`)
 * - HTML5 Geolocation permission prompts
 * - File upload chooser (`onShowFileChooser`)
 * - Material 3 JavaScript dialog interception (Alert, Confirm, Prompt)
 * - Dynamic tab progress and favicon resolution
 */
class AeroWebChromeClient(
    private val tabId: String,
    private val tabManager: TabManager,
    private val onShowCustomViewCallback: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    private val onHideCustomViewCallback: (() -> Unit)? = null,
    private val onGeolocationPrompt: ((String, GeolocationPermissions.Callback) -> Unit)? = null,
    private val onShowFileChooserCallback: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean)? = null,
    private val onJsAlertCallback: ((String, String, JsResult) -> Boolean)? = null,
    private val onJsConfirmCallback: ((String, String, JsResult) -> Boolean)? = null,
    private val onJsPromptCallback: ((String, String, String, JsPromptResult) -> Boolean)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        tabManager.updateProgress(tabId, newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        tabManager.updateTitleAndUrl(tabId, title, view?.url)
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        tabManager.updateFavicon(tabId, icon)
    }

    // --- Fullscreen Video Handling ---
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null && onShowCustomViewCallback != null) {
            onShowCustomViewCallback.invoke(view, callback)
        } else {
            super.onShowCustomView(view, callback)
        }
    }

    override fun onHideCustomView() {
        if (onHideCustomViewCallback != null) {
            onHideCustomViewCallback.invoke()
        } else {
            super.onHideCustomView()
        }
    }

    // --- Geolocation Permissions ---
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin != null && callback != null && onGeolocationPrompt != null) {
            onGeolocationPrompt.invoke(origin, callback)
        } else {
            callback?.invoke(origin, false, false)
        }
    }

    // --- File Upload / Camera Chooser ---
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onShowFileChooserCallback?.invoke(filePathCallback, fileChooserParams)
            ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    // --- JavaScript Dialogs ---
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        if (message != null && result != null && onJsAlertCallback != null) {
            return onJsAlertCallback.invoke(url ?: "", message, result)
        }
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        if (message != null && result != null && onJsConfirmCallback != null) {
            return onJsConfirmCallback.invoke(url ?: "", message, result)
        }
        return super.onJsConfirm(view, url, message, result)
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        if (message != null && result != null && onJsPromptCallback != null) {
            return onJsPromptCallback.invoke(url ?: "", message, defaultValue ?: "", result)
        }
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }
}
