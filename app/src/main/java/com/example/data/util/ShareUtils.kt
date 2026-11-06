package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.Anime

data class DeepLinkTarget(
    val animeId: String,
    val episodeNumber: Int? = null
)

object ShareUtils {
    private const val WEB_BASE_URL = "https://gokuthug1.github.io/zenith-anime/"

    fun buildShareWebUrl(animeId: String, episodeNumber: Int? = null): String {
        val cleanId = animeId.trim()
        return if (episodeNumber != null && episodeNumber > 0) {
            "${WEB_BASE_URL}?anime=$cleanId&ep=$episodeNumber#watch/$cleanId/$episodeNumber"
        } else {
            "${WEB_BASE_URL}?anime=$cleanId#details/$cleanId"
        }
    }

    fun buildAppDeepLink(animeId: String, episodeNumber: Int? = null): String {
        val cleanId = animeId.trim()
        return if (episodeNumber != null && episodeNumber > 0) {
            "zenithanime://watch/$cleanId/$episodeNumber"
        } else {
            "zenithanime://anime/$cleanId"
        }
    }

    fun shareAnime(
        context: Context,
        anime: Anime,
        titleLanguagePreference: String = "English",
        episodeNumber: Int? = null
    ) {
        val title = anime.displayTitle(titleLanguagePreference).ifBlank { anime.title }
        val webUrl = buildShareWebUrl(anime.id, episodeNumber)
        val appUrl = buildAppDeepLink(anime.id, episodeNumber)

        val text = buildString {
            if (episodeNumber != null && episodeNumber > 0) {
                append("Watch $title — Episode $episodeNumber on Zenith Anime!\n")
            } else {
                append("Watch $title on Zenith Anime!\n")
            }
            if (anime.rating > 0) {
                append("⭐ Rating: ${anime.rating}/10\n")
            }
            append("\nStream on Web or Open in Zenith App:\n")
            append(webUrl)
            append("\n\nDirect App Link:\n")
            append(appUrl)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, if (episodeNumber != null) "$title - Episode $episodeNumber" else title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share $title").apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }

    fun parseDeepLink(intent: Intent?): DeepLinkTarget? {
        if (intent == null) return null
        val uri: Uri = intent.data ?: return null
        return parseDeepLinkUri(uri)
    }

    fun parseDeepLinkUri(uri: Uri): DeepLinkTarget? {
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        val pathSegments = uri.pathSegments.orEmpty()

        // 1. Check query parameters first (?anime=12345&ep=2 or ?id=12345)
        val queryAnime = uri.getQueryParameter("anime")?.takeIf { it.isNotBlank() }
            ?: uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }
        val queryEp = (uri.getQueryParameter("ep") ?: uri.getQueryParameter("episode"))?.toIntOrNull()

        if (queryAnime != null) {
            return DeepLinkTarget(animeId = queryAnime.trim(), episodeNumber = queryEp)
        }

        // 2. Check URI fragment (#details/12345, #anime/12345, #watch/12345/3)
        val fragment = uri.fragment?.trim()?.removePrefix("/").orEmpty()
        if (fragment.isNotEmpty()) {
            val parts = fragment.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val routeType = parts[0].lowercase()
                val id = parts[1]
                if (routeType == "details" || routeType == "anime") {
                    return DeepLinkTarget(animeId = id, episodeNumber = queryEp)
                } else if (routeType == "watch") {
                    val ep = parts.getOrNull(2)?.toIntOrNull() ?: queryEp ?: 1
                    return DeepLinkTarget(animeId = id, episodeNumber = ep)
                }
            }
        }

        // 3. Check custom schemes: zenithanime://anime/12345 or zenithanime://watch/12345/2
        if (scheme == "zenithanime" || scheme == "zenith") {
            when (host) {
                "anime", "details" -> {
                    val id = pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
                    val ep = pathSegments.getOrNull(1)?.toIntOrNull() ?: queryEp
                    return DeepLinkTarget(animeId = id, episodeNumber = ep)
                }
                "watch" -> {
                    val id = pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
                    val ep = pathSegments.getOrNull(1)?.toIntOrNull() ?: queryEp ?: 1
                    return DeepLinkTarget(animeId = id, episodeNumber = ep)
                }
                else -> {
                    if (host.isNotBlank() && host.all { it.isDigit() }) {
                        return DeepLinkTarget(animeId = host, episodeNumber = queryEp)
                    }
                }
            }
        }

        // 4. Check HTTP/HTTPS web links (gokuthug1.github.io, anilist.co, myanimelist.net)
        if (scheme == "http" || scheme == "https") {
            if (host.contains("anilist.co") && pathSegments.size >= 2 && pathSegments[0].equals("anime", true)) {
                val id = pathSegments[1].takeIf { it.all { ch -> ch.isDigit() } } ?: return null
                return DeepLinkTarget(animeId = id, episodeNumber = queryEp)
            }
            if (host.contains("myanimelist.net") && pathSegments.size >= 2 && pathSegments[0].equals("anime", true)) {
                val malId = pathSegments[1].takeIf { it.all { ch -> ch.isDigit() } } ?: return null
                return DeepLinkTarget(animeId = "mal_$malId", episodeNumber = queryEp)
            }
            // Path like /zenith-anime/anime/12345 or /zenith-anime/watch/12345/2
            val animeIdx = pathSegments.indexOfFirst { it.equals("anime", true) || it.equals("details", true) }
            if (animeIdx >= 0 && pathSegments.size > animeIdx + 1) {
                val id = pathSegments[animeIdx + 1]
                return DeepLinkTarget(animeId = id, episodeNumber = queryEp)
            }
            val watchIdx = pathSegments.indexOfFirst { it.equals("watch", true) }
            if (watchIdx >= 0 && pathSegments.size > watchIdx + 1) {
                val id = pathSegments[watchIdx + 1]
                val ep = pathSegments.getOrNull(watchIdx + 2)?.toIntOrNull() ?: queryEp ?: 1
                return DeepLinkTarget(animeId = id, episodeNumber = ep)
            }
        }

        return null
    }
}
