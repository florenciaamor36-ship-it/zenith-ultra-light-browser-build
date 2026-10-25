package com.example.control

import android.content.Context
import android.webkit.WebView
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Authenticated, local HTTP API for the browser currently open in AeroWeb. */
class BrowserControlServer(
    context: Context,
    private val actions: Actions,
    port: Int = 8787
) : NanoHTTPD("0.0.0.0", port) {
    interface Actions {
        fun status(): JSONObject
        fun navigate(url: String): JSONObject
        fun back(): JSONObject
        fun page(): JSONObject
        fun resources(): JSONArray
        fun scroll(direction: String): JSONObject
    }

    private val token = getOrCreateToken(context)

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) return json(Response.Status.OK, JSONObject().put("ok", true))
        if (session.uri == "/health") return json(Response.Status.OK, JSONObject().put("ok", true).put("service", "aeroweb"))
        val supplied = session.headers["authorization"].orEmpty().removePrefix("Bearer ").toByteArray()
        if (!MessageDigest.isEqual(token.toByteArray(), supplied)) {
            return json(Response.Status.UNAUTHORIZED, JSONObject().put("error", "Use Authorization: Bearer <token>"))
        }
        return try {
            when (session.method to session.uri) {
                Method.GET to "/v1/status" -> json(Response.Status.OK, actions.status())
                Method.GET to "/v1/page" -> json(Response.Status.OK, actions.page())
                Method.GET to "/v1/resources" -> json(Response.Status.OK, JSONObject().put("resources", actions.resources()))
                Method.POST to "/v1/navigate" -> {
                    val body = readBody(session)
                    json(Response.Status.OK, actions.navigate(body.optString("url")))
                }
                Method.POST to "/v1/back" -> json(Response.Status.OK, actions.back())
                Method.POST to "/v1/scroll" -> {
                    val body = readBody(session)
                    json(Response.Status.OK, actions.scroll(body.optString("direction", "down")))
                }
                else -> json(Response.Status.NOT_FOUND, JSONObject().put("error", "Unknown endpoint"))
            }
        } catch (e: Exception) {
            json(Response.Status.INTERNAL_ERROR, JSONObject().put("error", e.message ?: "Request failed"))
        }
    }

    private fun readBody(session: IHTTPSession): JSONObject {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return JSONObject(files["postData"].orEmpty().ifBlank { "{}" })
    }

    private fun json(status: Response.IStatus, payload: JSONObject): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", payload.toString()).apply {
            addHeader("Access-Control-Allow-Origin", "*")
            addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type")
            addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        }

    companion object {
        const val PORT = 8787
        private const val PREFS = "aeroweb_remote_api"
        private const val KEY = "bearer_token"

        fun getOrCreateToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getString(KEY, null)?.let { return it }
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val token = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            prefs.edit().putString(KEY, token).apply()
            return token
        }

        fun localIpv4Addresses(): List<String> = try {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
                .mapNotNull { it.hostAddress }.distinct()
        } catch (_: Exception) { emptyList() }

        /** Runs WebView operations on the UI thread and returns their value to an API worker. */
        fun <T> onUi(webView: WebView, block: () -> T): T? {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var result: T? = null
            val latch = CountDownLatch(1)
            webView.post { try { result = block() } finally { latch.countDown() } }
            return if (latch.await(15, TimeUnit.SECONDS)) result else null
        }
    }
}
