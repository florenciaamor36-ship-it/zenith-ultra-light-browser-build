package com.example.capture

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONTokener

/** Discovers page resources in the existing WebView. Never clicks controls or bypasses logins/DRM. */
object PageResourceScanner {
    const val MAX_RESOURCES = 300
    private const val MAX_SCROLL_STEPS = 20
    private val handler = Handler(Looper.getMainLooper())

    private val scanScript = """(function(){
      const found=new Map();
      window.__aeroCaptureStartY=window.scrollY;
      function add(raw,kind,mime){try{const u=new URL(raw,location.href);if(!['http:','https:'].includes(u.protocol))return;const key=u.href;if(!found.has(key))found.set(key,{url:key,kind:kind||'resource',mimeType:mime||'',fileName:(u.pathname.split('/').pop()||'resource').slice(0,120)});}catch(e){}}
      document.querySelectorAll('img').forEach(x=>add(x.currentSrc||x.src,'image','image/*'));
      document.querySelectorAll('video,audio,source,track').forEach(x=>add(x.currentSrc||x.src||x.href,x.tagName.toLowerCase(),''));
      document.querySelectorAll('script[src]').forEach(x=>add(x.src,'script','text/javascript'));
      document.querySelectorAll('link[href]').forEach(x=>{const r=(x.rel||'').toLowerCase();if(['stylesheet','icon','preload','modulepreload'].includes(r))add(x.href,r,x.type||'');});
      performance.getEntriesByType('resource').forEach(x=>add(x.name,x.initiatorType||'resource',''));
      document.querySelectorAll('a[href]').forEach(x=>{if(/\\.(pdf|zip|rar|7z|mp3|mp4|m4a|wav|webm|png|jpe?g|gif|webp|svg|woff2?|ttf|otf)(?:$|[?#])/i.test(x.href))add(x.href,'download','');});
      return JSON.stringify(Array.from(found.values()).slice(0,300));
    })();"""

    /** Scrolls a bounded number of viewports to load lazy content, restores position, then scans. */
    fun scan(webView: WebView, callback: (List<CapturedResource>) -> Unit) {
        val startY = webView.scrollY
        var step = 0
        webView.evaluateJavascript("window.__aeroCaptureStartY=window.scrollY", null)
        fun advance() {
            if (step >= MAX_SCROLL_STEPS || webView.scrollY + webView.height >= webView.contentHeight * webView.scale - 2) {
                webView.evaluateJavascript("window.scrollTo(0,window.__aeroCaptureStartY||0); $scanScript") { raw -> callback(decode(raw)) }
                return
            }
            step++
            webView.evaluateJavascript("window.scrollBy(0, Math.max(450, window.innerHeight * 0.8)); true") {
                handler.postDelayed({ advance() }, 220L)
            }
        }
        advance()
    }

    private fun decode(raw: String?): List<CapturedResource> {
        if (raw.isNullOrBlank() || raw == "null") return emptyList()
        return try {
            val text = JSONTokener(raw).nextValue() as? String ?: return emptyList()
            val arr = JSONArray(text)
            val resources = mutableListOf<CapturedResource>()
            val seen = mutableSetOf<String>()
            for (i in 0 until minOf(arr.length(), MAX_RESOURCES)) {
                val item = arr.optJSONObject(i) ?: continue
                val url = item.optString("url", "")
                if (!url.startsWith("http://") && !url.startsWith("https://")) continue
                if (!seen.add(url)) continue
                resources += CapturedResource(
                    url = url,
                    mimeType = item.optString("mimeType", ""),
                    kind = item.optString("kind", "resource"),
                    fileName = item.optString("fileName", "resource").take(120)
                )
            }
            resources
        } catch (_: Exception) {
            emptyList()
        }
    }
}
