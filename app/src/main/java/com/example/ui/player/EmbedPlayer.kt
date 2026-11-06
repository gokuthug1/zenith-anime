package com.example.ui.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** State reported back by the embedded player's hijacked <video> element. */
data class EmbedPlayerState(
    val ready: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = true,
    val positionSec: Long = 0L,
    val durationSec: Long = 0L,
    val bufferedSec: Long = 0L,
    val introStartSec: Long = -1L,
    val introEndSec: Long = -1L,
    val outroStartSec: Long = -1L,
    val outroEndSec: Long = -1L,
    val muted: Boolean = false,
    val volume: Float = 1f,
    /** The real media URL the embed is playing, when it exposes one. */
    val detectedSource: String? = null,
    /** True when the embed uses MediaSource (blob:) so the URL cannot be downloaded directly. */
    val drmOrMse: Boolean = false
)

/** Imperative handle the player UI uses to drive the embedded WebView player. */
class EmbedPlayerController {
    internal var webView: WebView? = null

    fun send(name: String, arg: Any? = null) {
        val formattedArg = when (arg) {
            null -> ""
            is Number, is Boolean -> ",$arg"
            is String -> {
                if (arg.isEmpty()) ""
                else if (
                    arg.toDoubleOrNull() != null ||
                    arg == "true" ||
                    arg == "false" ||
                    (arg.startsWith("\"") && arg.endsWith("\"")) ||
                    (arg.startsWith("'") && arg.endsWith("'"))
                ) {
                    ",$arg"
                } else {
                    ",${JSONObject.quote(arg)}"
                }
            }
            else -> ",${JSONObject.quote(arg.toString())}"
        }
        val call = "window.__zenith&&window.__zenith.cmd('$name'$formattedArg)"
        webView?.evaluateJavascript(call, null)
    }
}

private val BLOCKED_AD_TOKENS = listOf(
    "acscdn.com",
    "aclib.js",
    "popads",
    "popcash",
    "adsterra",
    "onclickads",
    "propellerads",
    "doubleclick.net",
    "googlesyndication.com",
    "histats.com",
    "chatango"
)

/**
 * Fallback playback path: loads a third-party embed inside a WebView and hijacks its video element.
 * Used when no direct stream can be resolved, or when the user explicitly picks the built-in player.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun EmbedPlayerView(
    embedUrl: String,
    wrapEmbeds: Boolean,
    state: MutableState<EmbedPlayerState>,
    controller: EmbedPlayerController,
    modifier: Modifier = Modifier,
    adBlockEnabled: Boolean = true
) {
    val wrap by rememberUpdatedState(wrapEmbeds)
    val currentAdBlock by rememberUpdatedState(adBlockEnabled)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                isFocusable = false
                isFocusableInTouchMode = false
                isLongClickable = false
                setOnLongClickListener { true }
                // Consume raw WebView touches so the embedded player's own touch/gesture UI never wakes up;
                // Zenith Anime's Compose gesture layer and native controls handle all user interactions.
                setOnTouchListener { _, _ -> true }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    allowFileAccess = true
                    allowContentAccess = true
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val intercepted = request ?: return null
                        val reqUrl = intercepted.url?.toString() ?: return null
                        if (!reqUrl.startsWith("http")) return null
                        if (currentAdBlock) {
                            val lowerUrl = reqUrl.lowercase()
                            if (BLOCKED_AD_TOKENS.any { lowerUrl.contains(it) }) {
                                return WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    204,
                                    "No Content",
                                    emptyMap(),
                                    ByteArrayInputStream(ByteArray(0))
                                )
                            }
                        }
                        if (intercepted.method != "GET") return null
                        if (!EmbedHijack.looksLikeDocument(intercepted)) return null
                        return EmbedHijack.rewrite(
                            reqUrl,
                            intercepted.requestHeaders,
                            intercepted.isForMainFrame,
                            wrap
                        )
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val scheme = request?.url?.scheme?.lowercase() ?: ""
                        if (scheme != "http" && scheme != "https") return true
                        if (currentAdBlock && request?.isForMainFrame == true) {
                            val host = request.url?.host?.lowercase() ?: ""
                            val trusted = listOf(
                                "vidlink", "vidsrc", "tryembed", "megaplay", "kaido", "megacloud",
                                "rabbitstream", "rapid-cloud", "streamwish", "filemoon", "jwplayer",
                                "anikoto", "aniembed", "animex", "vidcloud", "viduki", "animegg",
                                "anineko", "allanime", "gogoanime", "hianime", "zoro"
                            )
                            if (trusted.none { host.contains(it) }) return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(EmbedHijack.controllerScript, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(EmbedHijack.controllerScript, null)
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postState(json: String) {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                val payload = JSONObject(json)
                                if (payload.optBoolean("diag", false)) return@post
                                val current = state.value
                                val introStart = payload.optDouble("introStart", -1.0).toLong()
                                val introEnd = payload.optDouble("introEnd", -1.0).toLong()
                                val outroStart = payload.optDouble("outroStart", -1.0).toLong()
                                val outroEnd = payload.optDouble("outroEnd", -1.0).toLong()
                                state.value = current.copy(
                                    ready = payload.optBoolean("ready", false),
                                    playing = payload.optBoolean("playing", current.playing),
                                    buffering = payload.optBoolean("waiting", false),
                                    positionSec = payload.optDouble("t", current.positionSec.toDouble()).toLong(),
                                    durationSec = payload.optDouble("d", current.durationSec.toDouble()).toLong(),
                                    bufferedSec = payload.optDouble("b", current.bufferedSec.toDouble()).toLong(),
                                    introStartSec = if (introEnd > introStart && introStart >= 0) introStart else current.introStartSec,
                                    introEndSec = if (introEnd > introStart && introStart >= 0) introEnd else current.introEndSec,
                                    outroStartSec = if (outroEnd > outroStart && outroStart >= 0) outroStart else current.outroStartSec,
                                    outroEndSec = if (outroEnd > outroStart && outroStart >= 0) outroEnd else current.outroEndSec,
                                    muted = payload.optBoolean("muted", current.muted),
                                    volume = payload.optDouble("volume", current.volume.toDouble()).toFloat(),
                                    detectedSource = payload.optString("src", "").takeIf { it.startsWith("http") }
                                        ?: current.detectedSource,
                                    drmOrMse = payload.optBoolean("drm", current.drmOrMse)
                                )
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }, "AndroidPlayer")

                if (embedUrl.isNotBlank()) {
                    tag = embedUrl
                    loadUrl(embedUrl)
                }
                controller.webView = this
            }
        },
        update = { webView ->
            if (embedUrl.isNotBlank() && webView.tag != embedUrl) {
                webView.tag = embedUrl
                state.value = EmbedPlayerState()
                webView.loadUrl(embedUrl)
            }
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(controller) {
        onDispose {
            controller.webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
            controller.webView = null
        }
    }
}

@Composable
fun rememberEmbedPlayerController(): EmbedPlayerController = remember { EmbedPlayerController() }
