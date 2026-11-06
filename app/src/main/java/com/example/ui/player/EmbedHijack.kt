package com.example.ui.player

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

object EmbedHijack {

    private const val TAG = "ZenithHijack"
    private const val MAX_HTML_CHARS = 6 * 1024 * 1024
    private const val EMBED_MARKER = "__zenith_embed=1"
    private const val FALLBACK_UA =
        "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val forwardedHeaders = setOf(
        "accept",
        "accept-language",
        "user-agent",
        "referer",
        "origin",
        "cookie",
        "sec-fetch-dest",
        "sec-fetch-mode",
        "sec-fetch-site",
        "sec-fetch-user",
        "sec-ch-ua",
        "sec-ch-ua-mobile",
        "sec-ch-ua-platform",
        "priority",
        "dnt"
    )

    private val headTag = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)

    /**
     * Pre-render CSS injected directly into <head> before <body> parses so that AniEmbed
     * (SvelteKit + Vidstack/VJS-ejected custom elements) and other embed player UIs never
     * flash or render underneath Zenith Anime's native player controls.
     */
    private const val PREHIDE_CSS = """
html, body, main#main-content, main#main-content > div, main#main-content > div > div,
video-player, media-container, .vjs-ejected, hlsjs-video, dash-video, .video-provider {
  width: 100% !important;
  height: 100% !important;
  max-width: 100% !important;
  max-height: 100% !important;
  aspect-ratio: unset !important;
  border-radius: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  background: #000 !important;
  overflow: hidden !important;
}
video::-webkit-media-controls,
video::-webkit-media-controls-enclosure,
video::-webkit-media-controls-panel,
video::-webkit-media-controls-overlay-play-button,
media-controls,
media-tooltip-group,
media-tooltip,
media-tooltip-label,
media-tooltip-shortcut,
media-time-slider,
media-slider-track,
media-slider-fill,
media-slider-buffer,
media-slider-thumb,
media-slider-preview,
media-slider-value,
media-slider-thumbnail,
media-volume-slider,
media-play-button,
media-seek-button,
media-mute-button,
media-captions-button,
media-fullscreen-button,
media-pip-button,
media-cast-button,
media-time-group,
media-time,
media-time-separator,
media-gesture,
media-hotkey,
media-status-announcer,
media-volume-indicator,
media-status-indicator,
media-seek-indicator,
media-poster,
media-buffering-indicator,
google-cast,
.mobile-ctrl-icons,
.mobile-time-slider,
.vjs-input-feedback,
.vjs-feedback-island,
.vjs-feedback-bubble,
.vjs-volume-feedback,
.vjs-brightness-feedback,
.vjs-selection-feedback,
.vjs-status-feedback,
.vjs-seek-feedback,
.vjs-play-feedback,
.vjs-center-play,
.vjs-play-button,
.vjs-mute-button,
.vjs-captions-button,
.vjs-pip-button,
.vjs-fullscreen-button,
.vjs-cast-button,
.vjs-airplay-button,
.vjs-remote-group,
.vjs-slider,
.vjs-slider-track,
.vjs-slider-fill,
.vjs-slider-buffer,
.vjs-slider-thumb,
.vjs-slider-preview,
.vjs-hold-surface,
.vjs-hold-speed,
.vjs-settings-drawer,
.vjs-tooltip,
.settings-root,
.settings-panel,
.settings-trigger,
button[aria-label*="Next Episode"],
button[aria-label*="Previous Episode"],
button[aria-label*="Skip"],
[data-interactive],
media-container + div[aria-hidden="true"],
[aria-busy="true"],
.jw-controls, .jw-overlays, .jw-display-icon-container, .jw-dock, .jw-title, .jw-rightclick,
.vjs-control-bar, .vjs-big-play-button, .vjs-modal-dialog, .vjs-loading-spinner,
.plyr__controls, .plyr__control, .plyr__overlay,
.art-bottom, .art-control, .art-layer-auto-playback, .art-mask, .art-loading, .art-contextmenus,
.dplayer-controller, .dplayer-controller-mask, .dplayer-bezel, .dplayer-menu,
div[id*="ad"], div[class*="ad-"], div[class*="banner"], div[id*="pop"], iframe[src*="ad"], script[src*="acscdn"] {
  display: none !important;
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none !important;
  width: 0 !important;
  height: 0 !important;
  max-width: 0 !important;
  max-height: 0 !important;
  overflow: hidden !important;
  z-index: -9999 !important;
}
.captions {
  z-index: 5 !important;
  pointer-events: none !important;
}
"""

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun looksLikeDocument(request: WebResourceRequest): Boolean {
        val headers = request.requestHeaders
        val destination = headers.entries
            .firstOrNull { it.key.equals("Sec-Fetch-Dest", ignoreCase = true) }
            ?.value
            ?.lowercase()
        when (destination) {
            "document", "iframe", "frame" -> return true
            "script", "style", "image", "video", "audio", "font", "empty", "object",
            "embed", "manifest", "worker", "sharedworker", "track" -> return false
        }
        val accept = headers.entries
            .firstOrNull { it.key.equals("Accept", ignoreCase = true) }
            ?.value
            ?.lowercase()
            ?: ""
        if (accept.contains("text/html")) return true
        return request.isForMainFrame
    }

    fun rewrite(
        url: String,
        requestHeaders: Map<String, String>,
        isMainFrame: Boolean,
        wrapMainFrame: Boolean
    ): WebResourceResponse? {
        if (url.contains(EMBED_MARKER)) {
            val inner = stripMarker(url)
            Log.i(TAG, "marker inner=$inner")
            return serveDocument(inner, requestHeaders)
        }
        if (isMainFrame && wrapMainFrame) {
            return serveWrapper(url)
        }
        return serveDocument(url, requestHeaders)
    }

    private fun serveWrapper(embedUrl: String): WebResourceResponse? {
        return try {
            val wrapper = wrapperHtml(embedUrl)
            Log.i(TAG, "wrap url=$embedUrl bytes=${wrapper.length}")
            htmlResponse(wrapper, 200, "OK", Charsets.UTF_8)
        } catch (t: Throwable) {
            Log.i(TAG, "wrap failed url=$embedUrl ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    private fun serveDocument(url: String, requestHeaders: Map<String, String>): WebResourceResponse? {
        return try {
            val builder = Request.Builder().url(url).get()
            var hasAgent = false
            for ((key, value) in requestHeaders) {
                val name = key.lowercase()
                if (name !in forwardedHeaders) continue
                if (name == "user-agent") hasAgent = true
                try {
                    builder.header(key, value)
                } catch (t: Throwable) {
                    Log.i(TAG, "skip header $key ${t.javaClass.simpleName}")
                }
            }
            if (!hasAgent) builder.header("User-Agent", FALLBACK_UA)

            client.newCall(builder.build()).execute().use { response ->
                val body = response.body
                if (body == null) {
                    Log.i(TAG, "no body url=$url code=${response.code}")
                    return null
                }
                if (!response.isSuccessful) {
                    Log.i(TAG, "not ok url=$url code=${response.code} ${response.message}")
                    return null
                }
                val contentType = body.contentType()
                val type = contentType?.type?.lowercase() ?: ""
                val subtype = contentType?.subtype?.lowercase() ?: ""
                val isHtml = (type == "text" && subtype == "html") ||
                    (type == "application" && subtype == "xhtml+xml")
                if (!isHtml) {
                    Log.i(TAG, "not html url=$url mime=$type/$subtype")
                    return null
                }
                if (body.contentLength() > MAX_HTML_CHARS) {
                    Log.i(TAG, "too large url=$url length=${body.contentLength()}")
                    return null
                }
                val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val html = body.string()
                if (html.length > MAX_HTML_CHARS) {
                    Log.i(TAG, "too large body url=$url chars=${html.length}")
                    return null
                }
                for (cookie in response.headers("Set-Cookie")) {
                    try {
                        CookieManager.getInstance().setCookie(url, cookie)
                    } catch (_: Throwable) {
                    }
                }
                val finalUrl = response.request.url.toString()
                val injected = inject(html, if (finalUrl == url) null else finalUrl)
                val reason = if (response.message.isEmpty()) "OK" else response.message
                Log.i(TAG, "served url=$url bytes=${injected.length} code=${response.code}")
                htmlResponse(injected, response.code, reason, charset)
            }
        } catch (t: Throwable) {
            Log.i(TAG, "fetch failed url=$url ${t.javaClass.name}: ${t.message}")
            null
        }
    }

    private fun htmlResponse(
        html: String,
        code: Int,
        message: String,
        charset: Charset
    ): WebResourceResponse {
        return WebResourceResponse(
            "text/html",
            charset.name(),
            code,
            message,
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-store"
            ),
            ByteArrayInputStream(html.toByteArray(charset))
        )
    }

    private fun wrapperHtml(embedUrl: String): String {
        val inner = markedUrl(embedUrl)
        return "<!DOCTYPE html><html><head>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}" +
            "iframe{position:fixed;top:0;left:0;width:100%;height:100%;border:0}</style></head><body>" +
            "<iframe src=\"" + inner + "\" allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture\" allowfullscreen></iframe>" +
            "<script>" + controllerScript + "</script></body></html>"
    }

    private fun markedUrl(url: String): String =
        url + (if (url.contains("?")) "&" else "?") + EMBED_MARKER

    private fun stripMarker(url: String): String {
        val cleaned = url.replace(EMBED_MARKER, "").replace("?&", "?").replace("&&", "&")
        return if (cleaned.endsWith("?") || cleaned.endsWith("&")) cleaned.dropLast(1) else cleaned
    }

    private fun inject(html: String, baseUrl: String?): String {
        var output = html
        val styleTag = "<style id=\"__zenith_prehide\">" + PREHIDE_CSS.trimIndent() + "</style>"
        val head = headTag.find(output)
        output = if (head != null) {
            val baseTag = if (baseUrl != null) "<base href=\"$baseUrl\">" else ""
            output.substring(0, head.range.last + 1) + baseTag + styleTag + output.substring(head.range.last + 1)
        } else {
            val baseTag = if (baseUrl != null) "<base href=\"$baseUrl\">" else ""
            baseTag + styleTag + output
        }
        val payload = "<script>" + controllerScript + "</script>"
        val bodyEnd = output.lastIndexOf("</body>", ignoreCase = true)
        return if (bodyEnd >= 0) {
            output.substring(0, bodyEnd) + payload + output.substring(bodyEnd)
        } else {
            output + payload
        }
    }

    val controllerScript: String = """
(function () {
  if (window.__zenithInstalled) {
    if (window.__zenith && window.__zenith.refresh) { window.__zenith.refresh(); }
    return;
  }
  window.__zenithInstalled = true;

  var AD_CSS = 'div[id*="ad"],div[class*="ad-"],div[class*="banner"],div[id*="pop"],iframe[src*="ad"],script[src*="acscdn"]{display:none!important;opacity:0!important;pointer-events:none!important;}';
  var MEDIA_CSS = [
    'html,body,main#main-content,main#main-content>div,main#main-content>div>div,video-player,media-container,.vjs-ejected,hlsjs-video,dash-video,.video-provider{width:100%!important;height:100%!important;max-width:100%!important;max-height:100%!important;aspect-ratio:unset!important;border-radius:0!important;margin:0!important;padding:0!important;background:#000!important;overflow:hidden!important;}',
    'video::-webkit-media-controls,video::-webkit-media-controls-enclosure,video::-webkit-media-controls-panel,video::-webkit-media-controls-overlay-play-button,',
    'media-controls,media-tooltip-group,media-tooltip,media-tooltip-label,media-tooltip-shortcut,',
    'media-time-slider,media-slider-track,media-slider-fill,media-slider-buffer,media-slider-thumb,media-slider-preview,media-slider-value,media-slider-thumbnail,media-volume-slider,',
    'media-play-button,media-seek-button,media-mute-button,media-captions-button,media-fullscreen-button,media-pip-button,media-cast-button,',
    'media-time-group,media-time,media-time-separator,media-gesture,media-hotkey,media-status-announcer,media-volume-indicator,media-status-indicator,media-seek-indicator,media-poster,media-buffering-indicator,google-cast,',
    '.mobile-ctrl-icons,.mobile-time-slider,.vjs-input-feedback,.vjs-feedback-island,.vjs-feedback-bubble,.vjs-volume-feedback,.vjs-brightness-feedback,.vjs-selection-feedback,.vjs-status-feedback,.vjs-seek-feedback,.vjs-play-feedback,',
    '.vjs-center-play,.vjs-play-button,.vjs-mute-button,.vjs-captions-button,.vjs-pip-button,.vjs-fullscreen-button,.vjs-cast-button,.vjs-airplay-button,.vjs-remote-group,',
    '.vjs-slider,.vjs-slider-track,.vjs-slider-fill,.vjs-slider-buffer,.vjs-slider-thumb,.vjs-slider-preview,.vjs-hold-surface,.vjs-hold-speed,.vjs-settings-drawer,.vjs-tooltip,',
    '.settings-root,.settings-panel,.settings-trigger,button[aria-label*="Next Episode"],button[aria-label*="Previous Episode"],button[aria-label*="Skip"],[data-interactive],media-container+div[aria-hidden="true"],[aria-busy="true"],',
    '.jw-controls,.jw-overlays,.jw-display-icon-container,.jw-dock,.jw-title,.jw-rightclick,.vjs-control-bar,.vjs-big-play-button,.vjs-modal-dialog,.vjs-loading-spinner,',
    '.plyr__controls,.plyr__control,.plyr__overlay,.art-bottom,.art-control,.art-layer-auto-playback,.art-mask,.art-loading,.art-contextmenus,.dplayer-controller,.dplayer-controller-mask,.dplayer-bezel,.dplayer-menu',
    '{display:none!important;opacity:0!important;visibility:hidden!important;pointer-events:none!important;width:0!important;height:0!important;max-width:0!important;max-height:0!important;overflow:hidden!important;z-index:-9999!important;}',
    '.captions{z-index:5!important;pointer-events:none!important;}'
  ].join('');

  var CHROME_SELECTORS = [
    'media-controls', 'media-tooltip-group', 'media-time-slider', 'media-volume-slider',
    'media-play-button', 'media-seek-button', 'media-mute-button', 'media-captions-button',
    'media-fullscreen-button', 'media-pip-button', 'media-cast-button', 'media-time-group',
    'media-gesture', 'media-hotkey', 'media-status-announcer', 'media-volume-indicator',
    'media-status-indicator', 'media-seek-indicator', 'media-poster', 'media-buffering-indicator', 'google-cast',
    '.mobile-ctrl-icons', '.mobile-time-slider', '.vjs-input-feedback', '.vjs-feedback-island',
    '.vjs-feedback-bubble', '.vjs-center-play', '.vjs-play-button', '.vjs-slider', '.vjs-hold-surface',
    '.vjs-hold-speed', '.vjs-settings-drawer', '.vjs-remote-group', '.vjs-tooltip',
    '.settings-root', '.settings-panel', '.settings-trigger', '[data-interactive]', '[aria-busy="true"]',
    '.jw-controls', '.jw-overlays', '.jw-display-icon-container', '.vjs-control-bar', '.vjs-big-play-button',
    '.plyr__controls', '.plyr__control', '.plyr__overlay',
    '.art-bottom', '.art-control', '.art-layer-auto-playback', '.art-mask',
    '.dplayer-controller', '.dplayer-controller-mask', '.dplayer-bezel',
    '[class*="control-bar"]', '[class*="controlbar"]', '[class*="controls"]', '[id*="control-bar"]', '[id*="controls"]',
    '[class*="player-bar"]', '[class*="bottom-bar"]', '[class*="top-bar"]',
    '[class*="player-title"]', '[class*="video-title"]', '[class*="overlay-title"]', '[class*="episode-title"]',
    '[class*="watermark"]', '[class*="logo"]', '[class*="brand"]',
    '[class*="next-episode"]', '[class*="nextep"]', '[class*="recommend"]', '[class*="notice"]',
    '[class*="comment"]', '[class*="chat"]', '[class*="discuss"]',
    '[aria-label*="omment"]', '[aria-label*="hat"]', '[title*="omment"]', '[title*="hat"]',
    '[class*="popup"]', '[class*="ad-"]', '[id*="ad-"]'
  ];

  var INTERCEPTOR_SELECTORS = 'media-gesture, media-hotkey, .vjs-hold-surface, media-controls';

  var video = null;
  var hadVideo = false;
  var currentFit = 'fit';
  var subtitlesEnabled = true;
  var scanPending = false;

  function pageRoot() { return document.head || document.documentElement; }

  function ensureStyle(id, css, rootScope) {
    var scope = rootScope || document;
    var container = rootScope || pageRoot();
    if (!container) { return; }
    var node = scope.getElementById ? scope.getElementById(id) : container.querySelector('#' + id);
    if (!node) {
      node = document.createElement('style');
      node.id = id;
      container.appendChild(node);
    }
    if (node.textContent !== css) { node.textContent = css; }
  }

  function applyFitCss(mode) {
    var f = String(mode || 'fit').toLowerCase();
    var objFit = 'contain';
    var transform = 'none';
    var aspect = 'unset';
    if (f === 'stretch' || f === 'fill') {
      objFit = 'fill';
    } else if (f === 'cover') {
      objFit = 'cover';
    } else if (f === 'zoom') {
      objFit = 'contain';
      transform = 'scale(1.16)';
    } else if (f === '16:9') {
      objFit = 'fill';
      aspect = '16 / 9';
    } else if (f === '4:3') {
      objFit = 'fill';
      aspect = '4 / 3';
    }
    var css = 'video, hlsjs-video video, dash-video video, .video-provider video {' +
      'width: 100% !important; height: 100% !important; max-width: 100% !important; max-height: 100% !important;' +
      'object-fit: ' + objFit + ' !important;' +
      'transform: ' + transform + ' !important;' +
      'transform-origin: center center !important;' +
      (aspect !== 'unset' ? ('aspect-ratio: ' + aspect + ' !important;') : '') +
      '}';
    ensureStyle('__zenith_fit', css);
    if (video && video.isConnected) {
      video.style.setProperty('object-fit', objFit, 'important');
      video.style.setProperty('width', '100%', 'important');
      video.style.setProperty('height', '100%', 'important');
      video.style.setProperty('transform', transform, 'important');
      video.style.setProperty('transform-origin', 'center center', 'important');
    }
  }

  function applySubtitlesState(enabled, styleSpec) {
    subtitlesEnabled = (enabled !== false && enabled !== 'false');
    var capCss = subtitlesEnabled
      ? '.captions, .jw-captions, .vjs-text-track-display, .plyr__captions { display: block !important; opacity: 1 !important; visibility: visible !important; pointer-events: none !important; }'
      : '.captions, .jw-captions, .vjs-text-track-display, .plyr__captions, video::-webkit-media-text-track-container, video::-webkit-media-text-track-display { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
    if (styleSpec && typeof styleSpec === 'object') {
      var scale = Math.max(0.6, Math.min(2.2, (Number(styleSpec.fontSizeSp) || 16) / 16));
      var color = styleSpec.textColor || '#ffffff';
      var bg = styleSpec.bgColor || 'rgba(0,0,0,0.6)';
      var stroke = styleSpec.outline ? '1.5px' : '0px';
      var weight = styleSpec.bold ? '800' : '600';
      var pos = styleSpec.position === 'Top' ? '-72%' : (styleSpec.position === 'Middle' ? '-38%' : '-4%');
      capCss += ' media-container, .vjs-ejected, .captions {' +
        '--media-user-font-size: ' + scale.toFixed(2) + ' !important;' +
        '--media-user-text-color: ' + color + ' !important;' +
        '--media-user-text-bg: ' + bg + ' !important;' +
        '--media-user-text-stroke-width: ' + stroke + ' !important;' +
        '--media-user-font-weight: ' + weight + ' !important;' +
        '--media-user-caption-position: ' + pos + ' !important;' +
        '}';
    }
    ensureStyle('__zenith_subs', capCss);
    if (video && video.textTracks) {
      try {
        for (var i = 0; i < video.textTracks.length; i++) {
          var t = video.textTracks[i];
          if (t.kind === 'subtitles' || t.kind === 'captions') {
            t.mode = subtitlesEnabled ? 'showing' : 'disabled';
          }
        }
      } catch (e) {}
    }
  }

  function isCaptionNode(node) {
    if (!node || !node.tagName) { return false; }
    var tag = node.tagName.toUpperCase();
    if (tag === 'TRACK') { return true; }
    var cls = String(node.className || '').toLowerCase();
    return cls.indexOf('caption') >= 0 || cls.indexOf('subtitle') >= 0 || cls.indexOf('text-track') >= 0;
  }

  function collectVideos(scope, out) {
    try {
      var found = scope.querySelectorAll('video');
      for (var i = 0; i < found.length; i++) { out.push(found[i]); }
    } catch (e) {}
    try {
      var all = scope.querySelectorAll('*');
      for (var j = 0; j < all.length; j++) {
        if (all[j].shadowRoot) {
          ensureStyle('__zenith_hide', MEDIA_CSS, all[j].shadowRoot);
          collectVideos(all[j].shadowRoot, out);
        }
      }
    } catch (e) {}
    return out;
  }

  function pickVideo() {
    var list = collectVideos(document, []);
    var best = null;
    var bestScore = -1;
    for (var i = 0; i < list.length; i++) {
      var candidate = list[i];
      if (!candidate.isConnected) { continue; }
      var score = 0;
      if (candidate.currentSrc) { score += 2; }
      if (candidate.videoWidth) { score += 2; }
      if (candidate.duration && isFinite(candidate.duration)) { score += 1; }
      if (!candidate.paused) { score += 1; }
      if (score > bestScore) { bestScore = score; best = candidate; }
    }
    return best;
  }

  function hideNode(node) {
    if (!node || !node.style) { return; }
    node.style.setProperty('display', 'none', 'important');
    node.style.setProperty('opacity', '0', 'important');
    node.style.setProperty('visibility', 'hidden', 'important');
    node.style.setProperty('pointer-events', 'none', 'important');
  }

  function hideChrome() {
    ensureStyle('__zenith_hide', MEDIA_CSS);
    try {
      var interceptors = document.querySelectorAll(INTERCEPTOR_SELECTORS);
      for (var k = 0; k < interceptors.length; k++) {
        hideNode(interceptors[k]);
        if (interceptors[k].tagName && (interceptors[k].tagName.toLowerCase() === 'media-gesture' || interceptors[k].tagName.toLowerCase() === 'media-hotkey')) {
          interceptors[k].remove();
        }
      }
    } catch (e) {}

    for (var i = 0; i < CHROME_SELECTORS.length; i++) {
      var nodes;
      try { nodes = document.querySelectorAll(CHROME_SELECTORS[i]); } catch (e) { continue; }
      for (var j = 0; j < nodes.length; j++) {
        var node = nodes[j];
        if (!node || node === document.body || node === document.documentElement) { continue; }
        if (node.tagName === 'VIDEO' || node.tagName === 'SCRIPT' || node.tagName === 'STYLE' || node.tagName === 'LINK') { continue; }
        if (isCaptionNode(node)) { continue; }
        if (video && (video.contains(node) || node.contains(video))) { continue; }
        hideNode(node);
      }
    }

    // Ancestor-sibling sweep: from <video> up to <body>, hide any sibling branch that doesn't hold <video> or captions
    if (video && video.isConnected) {
      var anc = video;
      while (anc && anc !== document.body && anc !== document.documentElement) {
        var parent = anc.parentElement;
        if (!parent) { break; }
        if (parent.style) {
          parent.style.setProperty('border-radius', '0', 'important');
        }
        var children = parent.children;
        for (var c = 0; c < children.length; c++) {
          var sib = children[c];
          if (sib === anc) { continue; }
          var tag = (sib.tagName || '').toUpperCase();
          if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'LINK' || tag === 'CANVAS' || tag === 'TRACK') { continue; }
          if (sib.contains && sib.contains(video)) { continue; }
          if (isCaptionNode(sib) || (sib.querySelector && sib.querySelector('.captions, .jw-captions, .vjs-text-track-display, .plyr__captions'))) {
            continue;
          }
          hideNode(sib);
        }
        anc = parent;
      }
    }
  }

  function extractChaptersAndBuffer() {
    var info = { b: 0, introStart: -1, introEnd: -1, outroStart: -1, outroEnd: -1 };
    if (!video || !video.isConnected) { return info; }
    try {
      if (video.buffered && video.buffered.length > 0) {
        var cur = video.currentTime || 0;
        for (var bi = 0; bi < video.buffered.length; bi++) {
          if (video.buffered.start(bi) <= cur + 1.5 && video.buffered.end(bi) >= cur) {
            info.b = video.buffered.end(bi);
            break;
          }
        }
        if (!info.b) { info.b = video.buffered.end(video.buffered.length - 1); }
      }
    } catch (e) {}
    try {
      var tracks = video.textTracks;
      if (tracks) {
        for (var ti = 0; ti < tracks.length; ti++) {
          var tr = tracks[ti];
          if (tr.kind === 'chapters' && tr.cues) {
            for (var ci = 0; ci < tr.cues.length; ci++) {
              var cue = tr.cues[ci];
              var label = String(cue.text || '').toLowerCase();
              if (label.indexOf('open') >= 0 || label.indexOf('intro') >= 0) {
                info.introStart = Math.round(cue.startTime);
                info.introEnd = Math.round(cue.endTime);
              } else if (label.indexOf('end') >= 0 || label.indexOf('outro') >= 0) {
                info.outroStart = Math.round(cue.startTime);
                info.outroEnd = Math.round(cue.endTime);
              }
            }
          }
        }
      }
    } catch (e) {}
    return info;
  }

  function report() {
    if (!video || !video.isConnected) {
      if (hadVideo) { post({ ready: false }); }
      return;
    }
    var total = video.duration;
    var src = video.currentSrc || video.src || '';
    if (!src) {
      try {
        var sources = video.querySelectorAll('source');
        if (sources.length > 0) { src = sources[sources.length - 1].src || ''; }
      } catch (e) {}
    }
    var extra = extractChaptersAndBuffer();
    post({
      ready: true,
      t: video.currentTime || 0,
      d: (total && isFinite(total)) ? total : 0,
      b: (extra.b && isFinite(extra.b)) ? extra.b : 0,
      introStart: extra.introStart,
      introEnd: extra.introEnd,
      outroStart: extra.outroStart,
      outroEnd: extra.outroEnd,
      playing: !video.paused && !video.ended,
      waiting: video.readyState < 3 && !video.paused,
      volume: (typeof video.volume === 'number') ? video.volume : 1,
      muted: !!video.muted,
      rate: video.playbackRate || 1,
      ended: !!video.ended,
      src: src,
      drm: !!(video.mediaKeys || (window.MediaSource && video.currentSrc && video.currentSrc.indexOf('blob:') === 0))
    });
  }

  function post(payload) {
    var raw = JSON.stringify(payload);
    try {
      if (window.AndroidPlayer && window.AndroidPlayer.postState) {
        window.AndroidPlayer.postState(raw);
        return;
      }
    } catch (e) {}
    try {
      if (window.parent && window.parent !== window) {
        window.parent.postMessage(JSON.stringify({ __zenith: payload }), '*');
      }
    } catch (e) {}
  }

  function broadcastCommand(name, arg) {
    var frames = document.querySelectorAll('iframe');
    for (var i = 0; i < frames.length; i++) {
      try {
        frames[i].contentWindow.postMessage(JSON.stringify({ __zenithCmd: name, arg: arg }), '*');
      } catch (e) {}
    }
  }

  function applyCommand(name, arg) {
    if (name === 'setFit') {
      currentFit = String(arg || 'fit').toLowerCase();
      applyFitCss(currentFit);
    } else if (name === 'setSubtitles') {
      applySubtitlesState(arg);
    } else if (name === 'setSubtitleStyle') {
      try {
        var parsed = (typeof arg === 'string') ? JSON.parse(arg) : arg;
        applySubtitlesState(parsed ? parsed.enabled : subtitlesEnabled, parsed);
      } catch (e) {}
    }
    if (!video || !video.isConnected) { return; }
    try {
      if (name === 'toggle') {
        if (video.paused) {
          var started = video.play();
          if (started && started.catch) { started.catch(function () {}); }
        } else {
          video.pause();
        }
      } else if (name === 'play') {
        var resumed = video.play();
        if (resumed && resumed.catch) { resumed.catch(function () {}); }
      } else if (name === 'pause') {
        video.pause();
      } else if (name === 'seek') {
        video.currentTime = Math.max(0, Number(arg) || 0);
      } else if (name === 'seekBy') {
        var limit = (video.duration && isFinite(video.duration)) ? video.duration : 1e9;
        video.currentTime = Math.max(0, Math.min(limit, (video.currentTime || 0) + (Number(arg) || 0)));
      } else if (name === 'setVolume') {
        video.volume = Math.max(0, Math.min(1, Number(arg)));
        if (video.volume > 0 && video.muted) { video.muted = false; }
      } else if (name === 'setMuted') {
        video.muted = (arg === true || arg === 'true');
      } else if (name === 'setRate') {
        video.playbackRate = Math.max(0.25, Math.min(4, Number(arg) || 1));
      }
    } catch (e) {}
    var hasVideo = !!(video && video.isConnected);
    post({
      diag: true,
      cmd: name,
      applied: hasVideo,
      playing: hasVideo ? !video.paused : false,
      t: hasVideo ? (video.currentTime || 0) : 0
    });
    report();
  }

  function tryAutoplay(target) {
    if (String(location.search || '').indexOf('autoplay=0') >= 0) { return; }
    if (!target || !target.paused) { return; }
    try {
      var p = target.play();
      if (p && p.catch) {
        p.catch(function () {
          try {
            target.muted = true;
            var mp = target.play();
            if (mp && mp.then) {
              mp.then(function () {
                window.setTimeout(function () { try { target.muted = false; } catch (e) {} }, 150);
              }).catch(function () {});
            }
          } catch (e2) {}
        });
      }
    } catch (e) {}
  }

  function bind(target) {
    var changed = (video !== target);
    video = target;
    if (!target.__zenithBound) {
      target.__zenithBound = true;
      var events = [
        'loadedmetadata', 'loadeddata', 'durationchange', 'timeupdate', 'progress',
        'play', 'pause', 'playing', 'waiting', 'canplay', 'canplaythrough',
        'seeking', 'seeked', 'volumechange', 'ratechange', 'ended', 'error', 'emptied'
      ];
      for (var i = 0; i < events.length; i++) {
        target.addEventListener(events[i], report, true);
      }
      target.addEventListener('canplay', function () {
        if (!target.__zenithAutoplayed) {
          target.__zenithAutoplayed = true;
          tryAutoplay(target);
        }
        hideChrome();
        report();
      }, true);
      if (target.readyState >= 2 && !target.__zenithAutoplayed) {
        target.__zenithAutoplayed = true;
        tryAutoplay(target);
      }
    }
    hadVideo = true;
    hideChrome();
    if (changed) {
      applyFitCss(currentFit);
      applySubtitlesState(subtitlesEnabled);
    }
    report();
  }

  function scan() {
    var candidate = pickVideo();
    if (candidate) { bind(candidate); }
  }

  function scheduleScan() {
    if (scanPending) { return; }
    scanPending = true;
    window.setTimeout(function () {
      scanPending = false;
      scan();
      hideChrome();
    }, 120);
  }

  function onMessage(event) {
    var data = null;
    try {
      data = (typeof event.data === 'string') ? JSON.parse(event.data) : event.data;
    } catch (e) { return; }
    if (!data) { return; }
    if (data.__zenith) {
      try {
        if (window.AndroidPlayer && window.AndroidPlayer.postState) {
          window.AndroidPlayer.postState(JSON.stringify(data.__zenith));
        }
      } catch (e) {}
      return;
    }
    if (data.__zenithCmd) {
      applyCommand(data.__zenithCmd, data.arg);
      broadcastCommand(data.__zenithCmd, data.arg);
    }
  }

  function diagnostics() {
    var videos = collectVideos(document, []);
    post({
      diag: true,
      url: String(location.href).substring(0, 140),
      isTop: (window.parent === window),
      frames: document.querySelectorAll('iframe').length,
      videos: videos.length,
      hasBridge: !!(window.AndroidPlayer && window.AndroidPlayer.postState),
      title: String(document.title).substring(0, 90)
    });
  }

  function start() {
    ensureStyle('__zenith_ads', AD_CSS);
    ensureStyle('__zenith_hide', MEDIA_CSS);
    applyFitCss(currentFit);
    hideChrome();
    scan();
    window.setTimeout(diagnostics, 2500);
    window.setTimeout(diagnostics, 9000);
    var target = document.documentElement || document.body;
    if (target && window.MutationObserver) {
      var observer = new MutationObserver(function () { scheduleScan(); });
      try {
        observer.observe(target, {
          childList: true,
          subtree: true,
          attributes: true,
          attributeFilter: ['class', 'style', 'data-visible', 'data-user-active', 'hidden']
        });
      } catch (e) {}
    }
    window.setInterval(function () {
      scan();
      hideChrome();
      if (video && video.isConnected) {
        report();
      }
    }, 600);
    window.addEventListener('message', onMessage);
  }

  window.__zenith = {
    cmd: function (name, arg) {
      applyCommand(name, arg);
      broadcastCommand(name, arg);
    },
    refresh: function () {
      scan();
      hideChrome();
      report();
    }
  };

  if (document.readyState === 'loading') {
    ensureStyle('__zenith_ads', AD_CSS);
    ensureStyle('__zenith_hide', MEDIA_CSS);
    document.addEventListener('DOMContentLoaded', start);
  } else {
    start();
  }
})();
""".trimIndent()
}
