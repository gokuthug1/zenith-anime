package com.example.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.repository.AnimeRepository
import com.example.data.util.LibrarySort
import kotlin.math.roundToInt

@Composable
fun LibrarySettingsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    SettingsScaffold(
        title = "Library & Series",
        subtitle = "How your anime are grouped and labelled",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSection(title = "CONTINUE WATCHING") {
            SettingSwitchRow(
                title = "One card per anime",
                subtitle = "Show one card per anime instead of one card per episode",
                checked = settings.continueWatchingOneCard,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(continueWatchingOneCard = value) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Shows to show",
                subtitle = "How many series appear in the Continue Watching row",
                value = settings.continueWatchingLimit.toFloat(),
                valueRange = 5f..50f,
                steps = 44,
                valueLabel = { "${it.roundToInt()}" },
                onValueChange = { value ->
                    repository.settingsManager.update { it.copy(continueWatchingLimit = value.roundToInt()) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Preview next episode",
                subtitle = "Preview the next episode on the Continue Watching card",
                checked = settings.continueWatchingShowNextEpisode,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(continueWatchingShowNextEpisode = value) }
                }
            )
        }

        SettingsSection(title = "EPISODES AND SORTING") {
            SettingChoiceRow(
                title = "Episode style",
                subtitle = "Cards show a thumbnail per episode, Numbers are compact chips, " +
                    "Grid lays episodes out three per row",
                currentValue = settings.episodeLayout,
                options = listOf("Cards", "Numbers", "Grid"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(episodeLayout = value) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Sort order",
                subtitle = "Order used for My List, watchlist and completed anime",
                currentValue = settings.librarySortOrder,
                options = LibrarySort.ALL,
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(librarySortOrder = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Show episode layout hint",
                subtitle = "Briefly explain the active episode style above the list",
                checked = settings.showEpisodeLayoutHint,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(showEpisodeLayoutHint = value) }
                }
            )
        }

        SettingsSection(title = "SEASONS AND NAMING") {
            SettingChoiceRow(
                title = "Anime title language",
                subtitle = "Choose whether anime titles display in English or Japanese (Romaji)",
                currentValue = settings.titleLanguagePreference,
                options = listOf("English", "Japanese"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(titleLanguagePreference = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Friendly season names",
                subtitle = "Say Season 1, Season 2 instead of AniList words like Adaptation or Sequel",
                checked = settings.friendlySeasonLabels,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(friendlySeasonLabels = value) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Season label style",
                subtitle = "How sequels are numbered for a series",
                currentValue = settings.seasonLabelStyle,
                options = listOf("Season", "Part"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(seasonLabelStyle = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Never say Adaptation",
                subtitle = "Never show the raw word Adaptation anywhere",
                checked = settings.hideAdaptationLabel,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(hideAdaptationLabel = value) }
                }
            )
        }

        SettingsSection(title = "WATCH PROGRESS") {
            SettingSliderRow(
                title = "Mark as watched at",
                subtitle = "Percentage of an episode watched before it counts as finished",
                value = settings.markWatchedThresholdPercent.toFloat(),
                valueRange = 50f..100f,
                steps = 49,
                valueLabel = { "${it.roundToInt()}%" },
                onValueChange = { value ->
                    repository.settingsManager.update {
                        it.copy(markWatchedThresholdPercent = value.roundToInt())
                    }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Add to My List when I start watching",
                subtitle = "Add an anime to My List as soon as you start watching it",
                checked = settings.autoAddToMyListOnWatch,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(autoAddToMyListOnWatch = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Blur unseen episode covers",
                subtitle = "Blur covers of episodes you have not reached yet",
                checked = settings.spoilerBlurCovers,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(spoilerBlurCovers = value) }
                }
            )
        }

        SettingsSection(title = "CONTENT & MATURITY") {
            SettingChoiceRow(
                title = "Adult & 18+ search filter",
                subtitle = "Control whether 18+ / Hentai / Adult anime are shown in Search alongside Ecchi & standard anime",
                currentValue = settings.adultContentFilter,
                options = listOf("SFW Only", "Include 18+", "18+ Only"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(adultContentFilter = value) }
                }
            )
        }
    }
}
