package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

/**
 * Provider for AniEmbed (https://aniembed.se/#docs)
 * Direct embed player supporting AniList ID and episode numbers,
 * with sub/dub, autoplay, and timestamp controls.
 */
class AniEmbedProvider : StreamingProvider {
    override val id: String = "aniembed"
    override val displayName: String = "Server AniEmbed (aniembed.se)"
    override val domain: String = "aniembed.se"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = true,
        supportsAutoNext = true,
        supportsAutoSkipIntro = true,
        supportsProgressEvents = true,
        supportsStartAt = true,
        requiresEmbed = false
    )

    override fun supportsAnime(anime: Anime): Boolean = anime.id.isNotEmpty()

    override fun getEmbedUrl(
        anime: Anime,
        episode: Episode,
        language: String,
        autoplay: Boolean,
        autoSkip: Boolean,
        startAtSec: Long
    ): String {
        val anilistId = anime.id
        val lang = if (language.equals("dub", ignoreCase = true)) "dub" else "sub"
        val auto = if (autoplay) "1" else "0"
        val tParam = if (startAtSec > 0) "&t=$startAtSec" else ""
        return "https://aniembed.se/e/$anilistId/${episode.number}?lang=$lang&autoplay=$auto$tParam"
    }
}
