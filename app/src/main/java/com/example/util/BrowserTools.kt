package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import java.net.URLEncoder

enum class SearchEngine(val displayName: String, val queryUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCK_DUCK_GO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=")
}

/**
 * Utility functions for advanced browser tools:
 * - PDF Generation
 * - Home Screen Shortcuts
 * - Google Page Translation
 * - Search / URL Parsing
 * - Native Sharing
 */
object BrowserTools {

    /**
     * Prints or exports current web page to PDF.
     */
    fun savePageAsPdf(context: Context, webView: WebView?, title: String) {
        if (webView == null) {
            Toast.makeText(context, "Cannot save PDF: page not loaded", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val jobName = "AeroWeb_${title.take(30).replace(Regex("[^a-zA-Z0-9]"), "_")}"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager?.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Adds current webpage as an interactive shortcut on the device home screen.
     */
    fun addShortcutToHomeScreen(
        context: Context,
        title: String,
        url: String,
        favicon: Bitmap?
    ) {
        try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                Toast.makeText(context, "Pin shortcuts not supported on this launcher", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val iconBitmap = favicon ?: createFallbackIcon(title.firstOrNull()?.uppercase() ?: "W")
            val iconCompat = IconCompat.createWithBitmap(iconBitmap)

            val cleanTitle = if (title.isNotBlank()) title.take(25) else "Web Page"

            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "shortcut_${url.hashCode()}")
                .setShortLabel(cleanTitle)
                .setLongLabel(cleanTitle)
                .setIcon(iconCompat)
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
            Toast.makeText(context, "Shortcut added to home screen", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to create shortcut: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Native share sheet for URL and page title.
     */
    fun shareUrl(context: Context, title: String, url: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Web Page")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    /**
     * Translates page using Google Translate proxy engine.
     */
    fun translatePage(webView: WebView?, currentUrl: String, targetLanguage: String = "es") {
        if (webView == null || currentUrl.isBlank()) return
        try {
            val encoded = URLEncoder.encode(currentUrl, "UTF-8")
            val translationUrl = "https://translate.google.com/translate?sl=auto&tl=$targetLanguage&u=$encoded"
            webView.loadUrl(translationUrl)
        } catch (_: Exception) {
            Toast.makeText(webView.context, "Failed to initiate translation", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Normalizes user query or URL input.
     */
    fun formatInputToUrl(input: String, searchEngine: SearchEngine = SearchEngine.GOOGLE): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"

        // Check if input is a valid URL or host
        val isUrl = (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true) ||
                trimmed.startsWith("about:", ignoreCase = true))

        if (isUrl) return trimmed

        val looksLikeDomain = !trimmed.contains(" ") && (
                trimmed.contains(".") ||
                trimmed.startsWith("localhost", ignoreCase = true)
        )

        return if (looksLikeDomain) {
            "https://$trimmed"
        } else {
            val query = URLEncoder.encode(trimmed, "UTF-8")
            "${searchEngine.queryUrl}$query"
        }
    }

    private fun createFallbackIcon(letter: String): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val textPaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = 64f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val yPos = (size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bitmap
    }
}
