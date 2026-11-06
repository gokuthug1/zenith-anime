package com.example.ui.search

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Anime
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeQuickActionDialog
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: AnimeRepository,
    onAnimeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeAccent = LocalThemeAccent.current
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val focusManager = LocalFocusManager.current
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var isRouletteRolling by remember { mutableStateOf(false) }
    var onlineResults by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var selectedQuickActionAnime by remember { mutableStateOf<Anime?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Filter states
    var filterFormat by remember { mutableStateOf("All") }
    var filterStatus by remember { mutableStateOf("All") }
    var filterSeason by remember { mutableStateOf("All") }
    var filterSort by remember { mutableStateOf("Popularity") }
    var filterAdult by remember(settings.adultContentFilter) { mutableStateOf(settings.adultContentFilter) }

    val searchPrefs = remember { context.getSharedPreferences("zenith_search_prefs", android.content.Context.MODE_PRIVATE) }
    var recentSearches by remember {
        val raw = searchPrefs.getString("recent_queries", null)
        val list = if (raw.isNullOrBlank()) emptyList<String>() else raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        mutableStateOf(list)
    }

    fun saveRecentSearches(list: List<String>) {
        if (settings.incognitoMode) return
        recentSearches = list
        searchPrefs.edit().putString("recent_queries", list.joinToString("\n")).apply()
    }

    fun addRecentSearch(query: String) {
        if (settings.incognitoMode) return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val updated = (listOf(trimmed) + recentSearches.filter { !it.equals(trimmed, ignoreCase = true) }).take(8)
        saveRecentSearches(updated)
    }

    val localAnime by repository.allAnime.collectAsState(initial = emptyList())

    val genres = listOf(
        "All",
        "Action",
        "Adventure",
        "Comedy",
        "Drama",
        "Ecchi",
        "Fantasy",
        "Hentai",
        "18+",
        "Horror",
        "Mahou Shoujo",
        "Mecha",
        "Music",
        "Mystery",
        "Psychological",
        "Romance",
        "Sci-Fi",
        "Shounen",
        "Slice of Life",
        "Sports",
        "Supernatural",
        "Thriller"
    )
    val formats = listOf("All", "TV", "Movie", "OVA", "ONA", "Special")
    val statuses = listOf("All", "Currently Airing", "Finished Airing", "Not Yet Aired")
    val seasons = listOf("All", "Winter", "Spring", "Summer", "Fall")
    val sorts = listOf("Popularity", "Score", "Trending", "Newest", "Recently Added", "Title A-Z")
    val adultModes = listOf("SFW Only", "Include 18+", "18+ Only")

    fun resolveIsAdultParam(genreValue: String, adultMode: String): Boolean? {
        if (genreValue.equals("Hentai", ignoreCase = true) || genreValue.equals("18+", ignoreCase = true)) {
            return true
        }
        return when (adultMode) {
            "18+ Only" -> true
            "Include 18+" -> null
            else -> false
        }
    }

    fun triggerSearch(
        q: String = searchQuery,
        genreOverride: String = selectedGenre,
        adultOverride: String = filterAdult,
        forceFilterQuery: Boolean = false
    ) {
        val trimmed = q.trim()
        val hasActiveFilter = genreOverride != "All" ||
            filterFormat != "All" ||
            filterStatus != "All" ||
            filterSeason != "All" ||
            adultOverride != "SFW Only" ||
            forceFilterQuery
        if (trimmed.length >= 2 || hasActiveFilter) {
            isSearchingOnline = true
            if (trimmed.length >= 2) {
                addRecentSearch(trimmed)
            }
            scope.launch {
                val results = repository.searchOnlineFiltered(
                    query = trimmed.ifBlank { null },
                    format = if (filterFormat != "All") filterFormat else null,
                    status = if (filterStatus != "All") filterStatus else null,
                    season = if (filterSeason != "All") filterSeason else null,
                    sort = filterSort,
                    genre = if (genreOverride != "All") genreOverride else null,
                    isAdult = resolveIsAdultParam(genreOverride, adultOverride)
                )
                onlineResults = results
                isSearchingOnline = false
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            delay(500)
            triggerSearch(searchQuery)
        }
    }

    val combined = remember(onlineResults, localAnime) {
        (onlineResults + localAnime).distinctBy { it.id }
    }

    val filteredAnime = remember(
        combined,
        searchQuery,
        selectedGenre,
        filterFormat,
        filterStatus,
        filterSeason,
        filterSort,
        filterAdult,
        settings.titleLanguagePreference
    ) {
        val matched = combined.filter { anime ->
            val isOnlineMatch = onlineResults.any { it.id == anime.id }
            val matchesQuery = searchQuery.isBlank() ||
                    isOnlineMatch ||
                    anime.title.contains(searchQuery, ignoreCase = true) ||
                    anime.englishTitle.contains(searchQuery, ignoreCase = true) ||
                    anime.romajiTitle.contains(searchQuery, ignoreCase = true) ||
                    anime.nativeTitle.contains(searchQuery, ignoreCase = true) ||
                    anime.genres.contains(searchQuery, ignoreCase = true)

            val isAnimeAdult = anime.ageRating.equals("18+", ignoreCase = true) ||
                    anime.genres.contains("Hentai", ignoreCase = true) ||
                    anime.genres.contains("18+", ignoreCase = true)

            val matchesGenre = when {
                selectedGenre == "All" -> true
                selectedGenre.equals("18+", ignoreCase = true) -> isAnimeAdult
                else -> anime.genres.contains(selectedGenre, ignoreCase = true)
            }

            val matchesAdult = when {
                selectedGenre.equals("Hentai", ignoreCase = true) || selectedGenre.equals("18+", ignoreCase = true) -> isAnimeAdult
                filterAdult == "18+ Only" -> isAnimeAdult
                filterAdult == "Include 18+" -> true
                else -> !isAnimeAdult
            }

            val matchesFormat = filterFormat == "All" || anime.type.equals(filterFormat, ignoreCase = true)

            val matchesStatus = when (filterStatus) {
                "All" -> true
                "Currently Airing", "Airing" -> anime.isAiring
                "Finished Airing", "Finished" -> anime.status.equals("FINISHED", ignoreCase = true) ||
                        anime.status.contains("Finished", ignoreCase = true)
                "Not Yet Aired", "Upcoming" -> anime.isUpcoming
                else -> true
            }

            val matchesSeason = filterSeason == "All" || anime.season.contains(filterSeason, ignoreCase = true)

            matchesQuery && matchesGenre && matchesAdult && matchesFormat && matchesStatus && matchesSeason
        }

        when (filterSort) {
            "Score", "Popularity" -> matched.sortedWith(
                compareByDescending<Anime> { it.rating }.thenBy(String.CASE_INSENSITIVE_ORDER) {
                    it.displayTitle(settings.titleLanguagePreference)
                }
            )
            "Newest", "Trending" -> matched.sortedWith(
                compareByDescending<Anime> { it.year }.thenByDescending { it.rating }
            )
            "Recently Added" -> matched.sortedWith(
                compareByDescending<Anime> { it.lastUpdated }.thenByDescending { it.year }
            )
            "Title A-Z" -> matched.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle(settings.titleLanguagePreference) }
            )
            else -> matched
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        // Top Search Bar + Roulette + Filter Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search anime...", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = themeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onlineResults = emptyList()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    triggerSearch(searchQuery)
                }),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Random Anime Roulette button
            IconButton(
                onClick = {
                    if (!isRouletteRolling) {
                        isRouletteRolling = true
                        scope.launch {
                            val pick = repository.getRandomAnime()
                            isRouletteRolling = false
                            if (pick != null) {
                                Toast.makeText(
                                    context,
                                    "Roulette Discovered: ${pick.displayTitle(settings.titleLanguagePreference)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onAnimeClick(pick.id)
                            } else {
                                Toast.makeText(context, "Could not discover anime. Try again!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("search_roulette_button")
            ) {
                if (isRouletteRolling) {
                    CircularProgressIndicator(
                        color = themeAccent,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Anime Roulette",
                        tint = themeAccent
                    )
                }
            }

            // Filter button
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier.testTag("search_filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Sort & Filter",
                    tint = if (filterFormat != "All" || filterStatus != "All" || filterSeason != "All" || filterSort != "Popularity" || filterAdult != "SFW Only") themeAccent else Color.White
                )
            }
        }

        // Genre Selector Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) themeAccent else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            selectedGenre = genre
                            if (genre != "All") {
                                triggerSearch(searchQuery, genreOverride = genre, forceFilterQuery = true)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("genre_chip_${genre.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) bgColor else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Sleek Online Search Loading Indicator Pill
        AnimatedVisibility(
            visible = isSearchingOnline,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(surfaceColor)
                        .border(0.5.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = themeAccent,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Searching AniList catalog...",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Recent Searches row
        if (searchQuery.isBlank() && recentSearches.isNotEmpty() && !settings.incognitoMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Searches",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Clear All",
                    color = themeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { saveRecentSearches(emptyList()) }
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentSearches) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                searchQuery = tag
                                triggerSearch(tag)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (filteredAnime.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Search any anime online" else "No results for \"$searchQuery\"",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Type a title above or roll the dice for a random anime discovery." else "Tap Search to query live databases or adjust filters.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { triggerSearch(searchQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = bgColor),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Query Online AniList", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.gridColumnsCount.coerceIn(2, 3)),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                items(filteredAnime, key = { it.id }) { anime ->
                    AnimeCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime.id) },
                        onLongClick = { selectedQuickActionAnime = anime },
                        titlePreference = settings.titleLanguagePreference
                    )
                }
            }
        }
    }

    // Sort & Filter Bottom Sheet Modal
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = surfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort & Filter",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            filterFormat = "All"
                            filterStatus = "All"
                            filterSeason = "All"
                            filterSort = "Popularity"
                            filterAdult = "SFW Only"
                            repository.settingsManager.update { it.copy(adultContentFilter = "SFW Only") }
                        }
                    ) {
                        Text("Reset", color = themeAccent, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Content Rating / 18+ Filter
                Text("MATURITY / 18+ CONTENT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(adultModes) { mode ->
                        FilterChip(
                            selected = filterAdult == mode,
                            onClick = {
                                filterAdult = mode
                                repository.settingsManager.update { it.copy(adultContentFilter = mode) }
                            },
                            label = { Text(mode, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = bgColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Format
                Text("FORMAT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(formats) { fmt ->
                        FilterChip(
                            selected = filterFormat == fmt,
                            onClick = { filterFormat = fmt },
                            label = { Text(fmt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = bgColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status
                Text("STATUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statuses) { stat ->
                        FilterChip(
                            selected = filterStatus == stat,
                            onClick = { filterStatus = stat },
                            label = { Text(stat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = bgColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Season
                Text("SEASON", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons) { sea ->
                        FilterChip(
                            selected = filterSeason == sea,
                            onClick = { filterSeason = sea },
                            label = { Text(sea, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = bgColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sort
                Text("SORT BY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sorts) { s ->
                        FilterChip(
                            selected = filterSort == s,
                            onClick = { filterSort = s },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = bgColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        showFilterSheet = false
                        triggerSearch(searchQuery, forceFilterQuery = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = bgColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (selectedQuickActionAnime != null) {
        val target = selectedQuickActionAnime!!
        AnimeQuickActionDialog(
            anime = target,
            onDismiss = { selectedQuickActionAnime = null },
            onPlay = { onAnimeClick(target.id) },
            onViewDetails = { onAnimeClick(target.id) },
            onToggleFavorite = {
                scope.launch {
                    repository.toggleFavorite(target.id, !target.isFavorite)
                    Toast.makeText(context, if (!target.isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleWatchlist = {
                scope.launch {
                    repository.toggleWatchlist(target.id, !target.isInWatchlist, target)
                    Toast.makeText(context, if (!target.isInWatchlist) "Added to Watchlist" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                }
            },
            onChangeWatchStatus = { status ->
                scope.launch {
                    repository.updateWatchStatus(target.id, status.name)
                    Toast.makeText(context, "Moved to ${status.displayName}", Toast.LENGTH_SHORT).show()
                }
            },
            onDownload = {
                scope.launch {
                    val ep = com.example.data.model.Episode(
                        id = "${target.id}_1",
                        animeId = target.id,
                        number = 1,
                        title = "Episode 1",
                        thumbnail = target.coverImage
                    )
                    repository.addDownload(target, ep)
                    Toast.makeText(context, "Saved Episode 1 for offline", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
