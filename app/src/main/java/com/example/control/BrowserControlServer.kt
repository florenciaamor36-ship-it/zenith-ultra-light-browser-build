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
        if (session.method == Method.GET && (session.uri == "/" || session.uri == "/control")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", CONTROL_PANEL)
        }
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
        private val CONTROL_PANEL = """<!doctype html>
<html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>AeroWeb remoto</title><style>
body{font:16px system-ui;max-width:850px;margin:24px auto;padding:0 16px;background:#10131a;color:#eef2f7}input,button{font:inherit;padding:12px;border-radius:10px;border:1px solid #384252;background:#1a2230;color:inherit}input{width:100%;box-sizing:border-box;margin:6px 0}button{cursor:pointer;margin:5px 4px 5px 0;background:#263b59}#url{width:calc(100% - 24px)}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#171d27;padding:14px;border-radius:10px;min-height:120px}.row{display:flex;gap:6px;flex-wrap:wrap}</style>
<h2>AeroWeb remoto</h2><p>Ingresá el token que muestra el menú «Conector para asistentes».</p>
<input id="token" type="password" placeholder="Token de acceso"><button onclick="save()">Conectar</button>
<div class="row"><input id="url" placeholder="https://sitio.com"><button onclick="go()">Navegar</button></div>
<div class="row"><button onclick="readPage()">Leer página</button><button onclick="back()">Atrás</button><button onclick="scrollPage('down')">Bajar</button><button onclick="scrollPage('up')">Subir</button><button onclick="scan()">Capturar recursos (500 máx.)</button></div>
<pre id="out">Sin conectar</pre>
<script>
const out=document.getElementById('out'),key='aeroweb-token';
document.getElementById('token').value=sessionStorage.getItem(key)||'';
function save(){sessionStorage.setItem(key,document.getElementById('token').value);status()}
async function api(path,method='GET',body){let t=document.getElementById('token').value;if(!t){out.textContent='Ingresá el token.';return null}try{let r=await fetch(path,{method,headers:{'Authorization':'Bearer '+t,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});let x=await r.json();if(!r.ok)throw Error(x.error||r.status);return x}catch(e){out.textContent='Error: '+e.message;return null}}
async function status(){let x=await api('/v1/status');if(x)out.textContent=JSON.stringify(x,null,2)}
async function go(){let x=await api('/v1/navigate','POST',{url:document.getElementById('url').value});if(x){out.textContent='Navegando… '+JSON.stringify(x);setTimeout(readPage,2500)}}
async function readPage(){let x=await api('/v1/page');if(x)out.textContent=JSON.stringify(x,null,2)}
async function back(){let x=await api('/v1/back','POST',{});if(x){out.textContent=JSON.stringify(x);setTimeout(readPage,1200)}}
async function scrollPage(direction){let x=await api('/v1/scroll','POST',{direction});if(x){out.textContent=JSON.stringify(x);setTimeout(readPage,700)}}
async function scan(){out.textContent='Buscando recursos…';let x=await api('/v1/resources');if(x)out.textContent=JSON.stringify(x,null,2)}
</script></html>"""

        fun <T> onUi(webView: WebView, block: () -> T): T? {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var result: T? = null
            val latch = CountDownLatch(1)
            webView.post { try { result = block() } finally { latch.countDown() } }
            return if (latch.await(15, TimeUnit.SECONDS)) result else null
        }
    }
}
