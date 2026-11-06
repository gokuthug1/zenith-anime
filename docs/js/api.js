/**
 * Zenith Anime Web — Network & API Service
 * Integrates AniList GraphQL, Jikan (MAL) Reviews, AnimeThemes.moe,
 * SeasonLabeler, Aired Episode Limiting, and Multi-Server Streaming Embed Resolvers.
 */

(function (window) {
  'use strict';

  const ANILIST_URL = 'https://graphql.anilist.co';
  const JIKAN_URL = 'https://api.jikan.moe/v4';
  const ANIMETHEMES_URL = 'https://api.animethemes.moe';

  // In-memory cache for fast tab navigation
  const cache = new Map();
  const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

  function getCached(key) {
    const entry = cache.get(key);
    if (!entry) return null;
    if (Date.now() - entry.time > CACHE_TTL) {
      cache.delete(key);
      return null;
    }
    return entry.data;
  }

  function setCached(key, data) {
    cache.set(key, { time: Date.now(), data });
  }

  function isSimilarToRomaji(a, b) {
    if (!a || !b) return false;
    const norm = (s) => String(s).toLowerCase().replace(/[^a-z0-9]/g, '').replace(/ou/g, 'o').replace(/oo/g, 'o').replace(/uu/g, 'u');
    return norm(a) === norm(b);
  }

  // ---- SEASON LABELER (Ported from Android SeasonLabeler.kt) ----
  const SeasonLabeler = {
    FINAL_SEASON: 99,
    RELATION_LABELS: {
      ADAPTATION: 'Same series',
      SEQUEL: 'Next season',
      PREQUEL: 'Previous season',
      PARENT: 'Main story',
      SIDE_STORY: 'Side story',
      'SIDE STORY': 'Side story',
      SPIN_OFF: 'Spin-off',
      'SPIN OFF': 'Spin-off',
      ALTERNATIVE: 'Alternate version',
      SUMMARY: 'Recap',
      CHARACTER: 'Character',
      SOURCE: 'Source material',
      COMPILATION: 'Compilation',
      CONTAINS: 'Contains',
      OTHER: 'Related'
    },

    romanToInt(raw) {
      const token = String(raw || '').trim().toLowerCase();
      if (!token || !/^[ivxlcdm]+$/.test(token)) return 0;
      const values = { i: 1, v: 5, x: 10, l: 50, c: 100, d: 500, m: 1000 };
      let total = 0;
      let previous = 0;
      for (let i = token.length - 1; i >= 0; i--) {
        const val = values[token[i]];
        if (!val) return 0;
        total += val < previous ? -val : val;
        previous = Math.max(previous, val);
      }
      return total >= 1 && total <= 20 ? total : 0;
    },

    friendlyRelationLabel(relationType) {
      const norm = String(relationType || 'RELATED').trim().toUpperCase().replace(/-/g, '_');
      const mapped =
        this.RELATION_LABELS[norm] ||
        this.RELATION_LABELS[norm.replace(/_/g, ' ')] ||
        norm.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
      return /adaptation/i.test(mapped) ? 'Same series' : mapped;
    },

    seasonNumberFromTitle(title) {
      const lower = String(title || '').trim().toLowerCase();
      if (!lower) return 0;

      if (/\bfinal\s+(?:season|part|cour)\b/.test(lower)) return this.FINAL_SEASON;

      let m = lower.match(/\b(\d{1,2})(?:st|nd|rd|th)\s+season\b/);
      if (m && parseInt(m[1], 10) > 0) return parseInt(m[1], 10);

      m = lower.match(/\bseason\s+(\d{1,2})\b/);
      if (m && parseInt(m[1], 10) > 0) return parseInt(m[1], 10);

      m = lower.match(/\bseason\s+([ivxlcdm]{1,5})\b/);
      if (m) {
        const n = this.romanToInt(m[1]);
        if (n > 0) return n;
      }

      m = lower.match(/\bpart\s+([0-9]{1,2}|[ivxlcdm]{1,5})\b/);
      if (m) {
        const n = parseInt(m[1], 10) || this.romanToInt(m[1]);
        if (n > 0) return n;
      }

      m = lower.match(/\b(\d{1,2})(?:st|nd|rd|th)\s+cour\b/) || lower.match(/\bcour\s+(\d{1,2})\b/);
      if (m && parseInt(m[1], 10) > 0) return parseInt(m[1], 10);

      const primarySegment = lower.split(':')[0].split(' - ')[0].split(' – ')[0];
      const tokens = lower.split(/[^a-z0-9]+/).filter(Boolean);
      const primaryTokens = primarySegment.split(/[^a-z0-9]+/).filter(Boolean);
      const candidates = [tokens[tokens.length - 1], primaryTokens[primaryTokens.length - 1]].filter(Boolean);
      for (const tok of candidates) {
        if (tok.length < 2) continue;
        const num = this.romanToInt(tok);
        if (num >= 2 && num <= 12) return num;
      }
      return 0;
    },

    seasonLabel(title, relationType, format, style = 'Season') {
      const upperFormat = String(format || '').trim().toUpperCase();
      if (['SPECIAL', 'OVA', 'ONA', 'MUSIC'].includes(upperFormat)) {
        return upperFormat === 'OVA' ? 'OVA' : upperFormat === 'ONA' ? 'ONA' : 'Specials';
      }
      if (upperFormat === 'MOVIE') {
        return 'Movie';
      }
      const usePart = String(style || '').trim().toLowerCase() === 'part';
      const num = this.seasonNumberFromTitle(title);
      let label;
      if (num === this.FINAL_SEASON) {
        label = usePart ? 'Final Part' : 'Final Season';
      } else if (num > 0) {
        label = usePart ? `Part ${num}` : `Season ${num}`;
      } else {
        label = this.friendlyRelationLabel(relationType);
      }
      return /adaptation/i.test(label) ? 'Same series' : label;
    }
  };

  /**
   * Computes episode availability with exact Android Anime.kt parity:
   * - Upcoming / Not Yet Released anime ALWAYS have availableEpisodes = 0 (never 12/24 phantom episodes).
   * - Currently Airing anime cap availableEpisodes to (nextAiringEpisode.episode - 1) so unaired episodes are never listed.
   */
  function computeEpisodeAvailability(item) {
    const rawStatus = String(item.status || 'FINISHED').trim().toUpperCase();
    const format = String(item.format || item.type || 'TV').trim().toUpperCase();
    const nextAiring = item.nextAiringEpisode && Number(item.nextAiringEpisode.episode) > 0
      ? {
          episode: Number(item.nextAiringEpisode.episode),
          timeUntilAiring: Number(item.nextAiringEpisode.timeUntilAiring || 0),
          airingAt: Number(item.nextAiringEpisode.airingAt || 0)
        }
      : null;

    const isUpcoming =
      rawStatus === 'NOT_YET_RELEASED' ||
      rawStatus === 'UPCOMING' ||
      rawStatus === 'NOT YET AIRED' ||
      Boolean(nextAiring && nextAiring.episode === 1 && nextAiring.timeUntilAiring > 0);

    const isAiring =
      !isUpcoming &&
      (rawStatus === 'RELEASING' || rawStatus === 'CURRENTLY AIRING' || rawStatus === 'AIRING');

    const rawEpisodes = item.episodes !== null && item.episodes !== undefined && Number(item.episodes) > 0
      ? Number(item.episodes)
      : (Number(item.totalEpisodes) > 0 ? Number(item.totalEpisodes) : null);

    const streamingList = Array.isArray(item.streamingEpisodes) ? item.streamingEpisodes : [];
    const streamingCount = streamingList.length;

    let totalEpisodes = 0;
    if (rawEpisodes !== null && rawEpisodes > 0) {
      totalEpisodes = rawEpisodes;
    } else if (nextAiring && nextAiring.episode > 0) {
      totalEpisodes = Math.max(0, nextAiring.episode - 1);
    } else if (streamingCount > 0) {
      totalEpisodes = streamingCount;
    } else if (rawStatus === 'FINISHED') {
      totalEpisodes = format === 'MOVIE' ? 1 : 12;
    } else {
      totalEpisodes = 0;
    }

    let availableEpisodes = 0;
    if (isUpcoming) {
      availableEpisodes = 0;
    } else if (isAiring) {
      if (nextAiring && nextAiring.episode > 0) {
        availableEpisodes = Math.max(0, nextAiring.episode - 1);
      } else if (streamingCount > 0) {
        availableEpisodes = streamingCount;
      } else {
        availableEpisodes = Math.max(0, totalEpisodes);
      }
    } else if (format === 'MOVIE') {
      availableEpisodes = 1;
    } else {
      availableEpisodes = Math.max(0, totalEpisodes);
    }

    return {
      status: rawStatus,
      isUpcoming,
      isAiring,
      totalEpisodes,
      availableEpisodes,
      nextAiringEpisode: nextAiring
    };
  }

  const MEDIA_FIELDS = `
    id
    idMal
    title {
      romaji
      english
      native
    }
    synonyms
    coverImage {
      large
      extraLarge
    }
    bannerImage
    description
    averageScore
    status
    episodes
    seasonYear
    season
    format
    genres
    studios(isMain: true) {
      nodes {
        name
      }
    }
    nextAiringEpisode {
      airingAt
      timeUntilAiring
      episode
    }
  `;

  function parseMediaItem(item) {
    if (!item) return null;
    const titleObj = item.title || {};
    const englishRaw = titleObj.english && titleObj.english !== 'null' ? titleObj.english.trim() : '';
    const romaji = titleObj.romaji && titleObj.romaji !== 'null' ? titleObj.romaji.trim() : '';
    const native = titleObj.native && titleObj.native !== 'null' ? titleObj.native.trim() : '';
    const synonyms = Array.isArray(item.synonyms) ? item.synonyms.map(s => String(s || '').trim()).filter(Boolean) : [];

    // Prefer distinct ASCII English synonym if english title is missing or identical to romaji (Android parity)
    let english = englishRaw;
    if (!englishRaw || (romaji && isSimilarToRomaji(englishRaw, romaji))) {
      const candidateSynonym = synonyms.find(syn => /^[\x00-\x7F]+$/.test(syn) && !isSimilarToRomaji(syn, romaji));
      if (candidateSynonym) english = candidateSynonym;
    }

    const mainTitle = english || romaji || native || 'Untitled Anime';

    const cover = (item.coverImage && (item.coverImage.extraLarge || item.coverImage.large)) || '';
    const banner = item.bannerImage || cover;

    // Clean up HTML tags from description
    const rawDesc = item.description || 'No description available.';
    const cleanDesc = rawDesc.replace(/<[^>]*>/g, '').trim() || 'No synopsis available.';

    // Studio name
    let studio = 'Studio';
    if (item.studios && item.studios.nodes && item.studios.nodes.length > 0) {
      studio = item.studios.nodes[0].name || 'Studio';
    }

    // Rating normalized to 10-point scale
    const rawScore = item.averageScore || 0;
    const rating = rawScore > 0 ? (rawScore / 10).toFixed(1) : '—';

    const epState = computeEpisodeAvailability(item);
    const genreArr = Array.isArray(item.genres) ? item.genres : [];
    const genresStr = genreArr.join(', ');
    const ageRating = /ecchi|horror/i.test(genresStr) ? 'R' : 'PG-13';

    // Map optional streamingEpisodes metadata (titles + thumbnails) by episode number
    const episodeMetadata = {};
    if (Array.isArray(item.streamingEpisodes)) {
      item.streamingEpisodes.forEach((se, idx) => {
        if (!se) return;
        const rawTitle = String(se.title || '').trim();
        const numMatch = rawTitle.match(/(?:episode|ep\.?)\s*(\d+)/i);
        const epNum = numMatch ? parseInt(numMatch[1], 10) : (idx + 1);
        const cleanEpTitle = rawTitle.replace(/^(?:episode|ep\.?)\s*\d+\s*[-:–—]?\s*/i, '').trim() || `Episode ${epNum}`;
        if (epNum > 0 && !episodeMetadata[epNum]) {
          episodeMetadata[epNum] = {
            number: epNum,
            title: cleanEpTitle,
            thumbnail: se.thumbnail || ''
          };
        }
      });
    }

    return {
      id: String(item.id),
      malId: String(item.idMal || ''),
      title: mainTitle,
      englishTitle: english || mainTitle,
      romajiTitle: romaji || mainTitle,
      nativeTitle: native || mainTitle,
      coverImage: cover,
      bannerImage: banner,
      synopsis: cleanDesc,
      rating: parseFloat(rating) || 0,
      ratingDisplay: rating,
      ageRating,
      quality: 'HD',
      year: item.seasonYear || '',
      season: item.season || '',
      status: epState.status,
      isUpcoming: epState.isUpcoming,
      isAiring: epState.isAiring,
      type: item.format || 'TV',
      totalEpisodes: epState.totalEpisodes,
      availableEpisodes: epState.availableEpisodes,
      genres: genresStr,
      genreList: genreArr,
      studio,
      nextAiringEpisode: epState.nextAiringEpisode,
      episodeMetadata
    };
  }

  /**
   * Ensure any anime object (including legacy saved library items) has accurate
   * isUpcoming, isAiring, and availableEpisodes fields.
   */
  function normalizeAnime(anime) {
    if (!anime) return null;
    if (typeof anime.availableEpisodes === 'number' && typeof anime.isUpcoming === 'boolean') {
      return anime;
    }
    const epState = computeEpisodeAvailability(anime);
    return Object.assign({}, anime, {
      status: epState.status,
      isUpcoming: epState.isUpcoming,
      isAiring: epState.isAiring,
      totalEpisodes: epState.totalEpisodes,
      availableEpisodes: epState.availableEpisodes,
      nextAiringEpisode: epState.nextAiringEpisode || anime.nextAiringEpisode || null
    });
  }

  async function executeGraphql(query, variables = {}) {
    const cacheKey = JSON.stringify({ query, variables });
    const cached = getCached(cacheKey);
    if (cached) return cached;

    try {
      const response = await fetch(ANILIST_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({ query, variables })
      });

      if (!response.ok) {
        throw new Error(`AniList returned HTTP ${response.status}`);
      }

      const json = await response.json();
      if (json.errors && json.errors.length > 0) {
        throw new Error(json.errors[0].message);
      }

      const data = json.data;
      setCached(cacheKey, data);
      return data;
    } catch (err) {
      console.error('GraphQL Query Error:', err);
      throw err;
    }
  }

  const API = {
    queryAniList: executeGraphql,
    SeasonLabeler,
    computeEpisodeAvailability,
    normalizeAnime,

    // ---- HOME CATALOG FEEDS ----
    async getTrending(page = 1, perPage = 20) {
      const query = `
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: TRENDING_DESC) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await executeGraphql(query, { page, perPage });
      const list = data?.Page?.media || [];
      return list.map(parseMediaItem).filter(Boolean);
    },

    async getPopular(page = 1, perPage = 20) {
      const query = `
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: POPULARITY_DESC) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await executeGraphql(query, { page, perPage });
      const list = data?.Page?.media || [];
      return list.map(parseMediaItem).filter(Boolean);
    },

    async getTopRated(page = 1, perPage = 20) {
      const query = `
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: SCORE_DESC) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await executeGraphql(query, { page, perPage });
      const list = data?.Page?.media || [];
      return list.map(parseMediaItem).filter(Boolean);
    },

    async getByGenre(genre, page = 1, perPage = 20) {
      const query = `
        query ($genre: String, $page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, genre: $genre, sort: POPULARITY_DESC) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await executeGraphql(query, { genre, page, perPage });
      const list = data?.Page?.media || [];
      return list.map(parseMediaItem).filter(Boolean);
    },

    async getRandomAnime() {
      const randomPage = Math.floor(Math.random() * 25) + 1;
      const query = `
        query ($page: Int) {
          Page(page: $page, perPage: 10) {
            media(type: ANIME, sort: POPULARITY_DESC) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      try {
        const data = await executeGraphql(query, { page: randomPage });
        const list = (data?.Page?.media || []).map(parseMediaItem).filter(Boolean);
        const airedList = list.filter(a => a.availableEpisodes > 0);
        const pool = airedList.length > 0 ? airedList : list;
        if (pool.length > 0) {
          return pool[Math.floor(Math.random() * pool.length)];
        }
      } catch (e) {
        console.warn('Random anime failed, fallback to trending', e);
      }
      const fallback = await this.getTrending(1, 5);
      return fallback[0] || null;
    },

    // ---- SEARCH & FILTER ----
    async searchAnime(queryStr, options = {}) {
      const { format, status, season, seasonYear, genre, sort = 'POPULARITY_DESC' } = options;

      const query = `
        query (
          $search: String,
          $format: MediaFormat,
          $status: MediaStatus,
          $season: MediaSeason,
          $seasonYear: Int,
          $sort: [MediaSort],
          $genre: String
        ) {
          Page(page: 1, perPage: 30) {
            media(
              search: $search,
              type: ANIME,
              format: $format,
              status: $status,
              season: $season,
              seasonYear: $seasonYear,
              sort: $sort,
              genre: $genre
            ) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;

      const variables = {};
      if (queryStr && queryStr.trim()) variables.search = queryStr.trim();
      if (format && format !== 'All') variables.format = format.toUpperCase();
      if (status && status !== 'All') {
        const s = status.toLowerCase();
        if (s.includes('airing') || s.includes('releasing')) variables.status = 'RELEASING';
        else if (s.includes('finished')) variables.status = 'FINISHED';
        else if (s.includes('not yet')) variables.status = 'NOT_YET_RELEASED';
        else variables.status = status.toUpperCase();
      }
      if (season && season !== 'All') variables.season = season.toUpperCase();
      if (seasonYear && Number(seasonYear) > 1950) variables.seasonYear = Number(seasonYear);
      if (genre && genre !== 'All') variables.genre = genre;

      const sortMap = {
        'popularity': 'POPULARITY_DESC',
        'score': 'SCORE_DESC',
        'trending': 'TRENDING_DESC',
        'newest': 'START_DATE_DESC',
        'recently added': 'UPDATED_AT_DESC',
        'title a-z': 'TITLE_ROMAJI'
      };
      variables.sort = [sortMap[String(sort).toLowerCase()] || 'POPULARITY_DESC'];

      const data = await executeGraphql(query, variables);
      const list = data?.Page?.media || [];
      return list.map(parseMediaItem).filter(Boolean);
    },

    // ---- ANIME DETAILS (WITH STREAMING EPISODES, CHARACTERS, RELATIONS & RECOMMENDATIONS) ----
    async getAnimeDetails(id) {
      const isMal = String(id).startsWith('mal_');
      const numericId = isMal ? parseInt(String(id).replace('mal_', ''), 10) : parseInt(id, 10);
      const filterArg = isMal ? 'idMal: $id' : 'id: $id';

      const query = `
        query ($id: Int) {
          Media(${filterArg}, type: ANIME) {
            ${MEDIA_FIELDS}
            streamingEpisodes {
              title
              thumbnail
              url
            }
            characters(page: 1, perPage: 16, sort: [ROLE, RELEVANCE]) {
              edges {
                role
                node {
                  id
                  name {
                    full
                  }
                  image {
                    large
                  }
                }
                voiceActors(language: JAPANESE) {
                  name {
                    full
                  }
                  image {
                    large
                  }
                  languageV2
                }
              }
            }
            relations {
              edges {
                relationType
                node {
                  id
                  type
                  title {
                    english
                    romaji
                    native
                  }
                  format
                  coverImage {
                    large
                    extraLarge
                  }
                  averageScore
                  status
                  episodes
                  seasonYear
                  nextAiringEpisode {
                    airingAt
                    timeUntilAiring
                    episode
                  }
                }
              }
            }
            recommendations(page: 1, perPage: 10, sort: [RATING_DESC]) {
              nodes {
                mediaRecommendation {
                  ${MEDIA_FIELDS}
                }
              }
            }
          }
        }
      `;

      const data = await executeGraphql(query, { id: numericId });
      const media = data?.Media;
      if (!media) return null;

      const base = parseMediaItem(media);

      // Parse Characters & VAs
      const characters = [];
      const charEdges = media.characters?.edges || [];
      charEdges.forEach((edge, i) => {
        const node = edge.node;
        if (!node) return;
        const vas = edge.voiceActors || [];
        const primaryVa = vas[0] || null;
        characters.push({
          id: String(node.id || i),
          role: edge.role || 'MAIN',
          characterName: node.name?.full || 'Character',
          characterImage: node.image?.large || '',
          vaName: primaryVa?.name?.full || '',
          vaImage: primaryVa?.image?.large || '',
          vaLanguage: primaryVa?.languageV2 || 'Japanese'
        });
      });

      // Parse Anime-only Relations with SeasonLabeler friendly labels (never raw ADAPTATION)
      const relations = [];
      const relEdges = media.relations?.edges || [];
      relEdges.forEach((edge) => {
        const node = edge.node;
        if (!node) return;
        const nodeType = String(node.type || 'ANIME').toUpperCase();
        const nodeFormat = String(node.format || 'TV').toUpperCase();
        if (nodeType !== 'ANIME' || ['MANGA', 'NOVEL', 'ONE_SHOT'].includes(nodeFormat)) {
          return;
        }
        const relTitle = node.title?.english || node.title?.romaji || node.title?.native || 'Related Anime';
        const relRomaji = node.title?.romaji || relTitle;
        const relNative = node.title?.native || relTitle;
        const rawRelType = edge.relationType || 'RELATED';
        const friendlyLabel = SeasonLabeler.seasonLabel(relTitle, rawRelType, nodeFormat, 'Season');
        const seasonNum = SeasonLabeler.seasonNumberFromTitle(relTitle);
        const epState = computeEpisodeAvailability(node);

        relations.push({
          id: String(node.id),
          relationType: rawRelType,
          friendlyLabel,
          seasonNumber: seasonNum,
          title: relTitle,
          englishTitle: node.title?.english || relTitle,
          romajiTitle: relRomaji,
          nativeTitle: relNative,
          format: nodeFormat,
          type: nodeFormat,
          coverImage: node.coverImage?.extraLarge || node.coverImage?.large || '',
          rating: node.averageScore ? parseFloat((node.averageScore / 10).toFixed(1)) : 0,
          ratingDisplay: node.averageScore ? (node.averageScore / 10).toFixed(1) : '',
          year: node.seasonYear || '',
          status: epState.status,
          isUpcoming: epState.isUpcoming,
          isAiring: epState.isAiring,
          totalEpisodes: epState.totalEpisodes,
          availableEpisodes: epState.availableEpisodes,
          isCurrent: String(node.id) === String(base.id)
        });
      });

      // Parse Recommendations ("More Like This")
      const recommendations = [];
      const recNodes = media.recommendations?.nodes || [];
      recNodes.forEach((rec) => {
        const parsed = parseMediaItem(rec?.mediaRecommendation);
        if (parsed && parsed.id !== base.id) {
          recommendations.push(parsed);
        }
      });

      base.characters = characters;
      base.relations = relations;
      base.recommendations = recommendations;

      return base;
    },

    // ---- WEEKLY SCHEDULE ----
    async getSchedule(startSec, endSec) {
      const query = `
        query ($start: Int, $end: Int) {
          Page(page: 1, perPage: 50) {
            airingSchedules(airingAt_greater: $start, airingAt_lesser: $end, sort: TIME) {
              id
              airingAt
              timeUntilAiring
              episode
              media {
                ${MEDIA_FIELDS}
              }
            }
          }
        }
      `;

      try {
        const data = await executeGraphql(query, { start: startSec, end: endSec });
        const list = data?.Page?.airingSchedules || [];
        return list.map(item => {
          const anime = parseMediaItem(item.media);
          if (!anime) return null;
          return {
            id: item.id,
            airingAt: item.airingAt,
            timeUntilAiring: item.timeUntilAiring,
            episode: item.episode,
            anime
          };
        }).filter(Boolean);
      } catch (e) {
        console.warn('Schedule fetch failed', e);
        return [];
      }
    },

    // ---- ANILIST PUBLIC PROFILE WATCHLIST IMPORT (Ported from Android MalSyncService.kt) ----
    async importFromAniList(username) {
      const trimmed = String(username || '').trim();
      if (!trimmed) {
        throw new Error('Please enter a valid AniList username.');
      }

      const query = `
        query ($userName: String) {
          MediaListCollection(userName: $userName, type: ANIME) {
            lists {
              name
              status
              entries {
                status
                score
                progress
                media {
                  ${MEDIA_FIELDS}
                }
              }
            }
          }
        }
      `;

      const data = await executeGraphql(query, { userName: trimmed });
      const lists = data?.MediaListCollection?.lists || [];
      const imported = [];

      lists.forEach((listObj) => {
        const entries = listObj.entries || [];
        entries.forEach((entry) => {
          const parsed = parseMediaItem(entry.media);
          if (!parsed) return;

          const aniStatus = String(entry.status || 'CURRENT').toUpperCase();
          let watchStatus = 'WATCHING';
          if (aniStatus === 'COMPLETED') watchStatus = 'COMPLETED';
          else if (aniStatus === 'PAUSED') watchStatus = 'ON_HOLD';
          else if (aniStatus === 'DROPPED') watchStatus = 'DROPPED';
          else if (aniStatus === 'PLANNING') watchStatus = 'PLAN_TO_WATCH';

          imported.push(Object.assign({}, parsed, {
            watchStatus,
            progressEpisodes: Number(entry.progress || 0)
          }));
        });
      });

      return imported;
    },

    // ---- ANIME REVIEWS (ANILIST PRIMARY + JIKAN MAL FALLBACK) ----
    async getMalReviews(malId, animeId) {
      if (!malId && !animeId) return [];
      const cacheKey = `anime_reviews_${animeId || malId}`;
      const cached = getCached(cacheKey);
      if (cached) return cached;

      const targetAnimeId = animeId && animeId !== '0' ? parseInt(animeId, 10) : null;
      const targetMalId = malId && malId !== '0' ? parseInt(malId, 10) : null;
      const collectedReviews = [];
      let hadNetworkError = false;

      // 1. Primary: Fetch from AniList GraphQL
      if (targetAnimeId || targetMalId) {
        const query = targetAnimeId
          ? `
            query ($id: Int) {
              Media(id: $id, type: ANIME) {
                id
                idMal
                reviews(page: 1, perPage: 10, sort: [RATING_DESC, ID_DESC]) {
                  nodes {
                    id
                    summary
                    score
                    rating
                    ratingAmount
                    body
                    createdAt
                    user {
                      name
                      avatar {
                        medium
                        large
                      }
                    }
                  }
                }
              }
            }
          `
          : `
            query ($idMal: Int) {
              Media(idMal: $idMal, type: ANIME) {
                id
                idMal
                reviews(page: 1, perPage: 10, sort: [RATING_DESC, ID_DESC]) {
                  nodes {
                    id
                    summary
                    score
                    rating
                    ratingAmount
                    body
                    createdAt
                    user {
                      name
                      avatar {
                        medium
                        large
                      }
                    }
                  }
                }
              }
            }
          `;

        try {
          const variables = targetAnimeId ? { id: Number(animeId) } : { idMal: Number(malId) };
          const res = await executeGraphql(query, variables);

          const nodes = res?.Media?.reviews?.nodes || [];
          nodes.forEach((item, i) => {
            const user = item.user || {};
            const author = user.name || 'AniList Reviewer';
            const avatar = user.avatar?.large || user.avatar?.medium || '';
            const score = item.score ? Math.round(item.score / 10) : 8;
            const body = (item.body || item.summary || '').trim();
            if (!body) return;
            const firstLine = body.split('\n').filter(Boolean)[0] || '';
            const summary = (item.summary || (firstLine.length > 120 ? firstLine.slice(0, 120) + '...' : firstLine)).trim();
            const rawDate = item.createdAt
              ? new Date(item.createdAt * 1000).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
              : '';
            const helpful = item.rating || item.ratingAmount || 0;

            collectedReviews.push({
              id: `al_${item.id || i}`,
              source: 'AniList',
              author,
              avatarUrl: avatar,
              score,
              summary,
              body,
              date: rawDate,
              isSpoiler: /spoilers?/i.test(summary) && !/~!/.test(body),
              episodesWatched: 0,
              helpfulCount: helpful
            });
          });
        } catch (err) {
          hadNetworkError = true;
          console.warn('AniList review fetch error:', err);
        }
      }

      // 2. Secondary / Supplement Fallback: Jikan (MyAnimeList) with 6000ms timeout
      if (targetMalId && collectedReviews.length < 6) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 6000);

        try {
          const response = await fetch(`${JIKAN_URL}/anime/${targetMalId}/reviews?preliminary=true&spoilers=true`, {
            signal: controller.signal
          });
          clearTimeout(timeoutId);
          if (response.ok) {
            const json = await response.json();
            const data = json.data || [];

            data.slice(0, 8).forEach((item, i) => {
              const user = item.user || {};
              const author = user.username || 'MAL Reviewer';
              const avatar = user.images?.jpg?.image_url || '';
              const score = item.score || 8;
              const body = (item.review || '').trim();
              if (!body) return;
              const firstLine = body.split('\n').filter(Boolean)[0] || '';
              const summary = firstLine.length > 120 ? firstLine.slice(0, 120) + '...' : firstLine;
              const rawDate = item.date || '';
              const date = rawDate.length >= 10 ? rawDate.slice(0, 10) : rawDate;
              const helpful = item.reactions?.overall || item.reactions?.nice || 0;

              collectedReviews.push({
                id: `mal_${item.mal_id || i}`,
                source: 'MAL',
                author,
                avatarUrl: avatar,
                score,
                summary,
                body,
                date,
                isSpoiler: Boolean(item.is_spoiler),
                episodesWatched: item.episodes_watched || 0,
                helpfulCount: helpful
              });
            });
          } else {
            hadNetworkError = true;
          }
        } catch {
          hadNetworkError = true;
        } finally {
          clearTimeout(timeoutId);
        }
      }

      // Do not cache empty results if a network error happened
      if (collectedReviews.length > 0 || !hadNetworkError) {
        setCached(cacheKey, collectedReviews);
      }
      return collectedReviews;
    },

    // ---- ANIMETHEMES.MOE OPENINGS & ENDINGS ----
    async getAnimeThemes(anilistId, title) {
      const cacheKey = `themes_${anilistId || title}`;
      const cached = getCached(cacheKey);
      if (cached) return cached;

      try {
        let url = '';
        if (anilistId && anilistId !== '0') {
          url = `${ANIMETHEMES_URL}/anime?filter[has]=resources&filter[site]=AniList&filter[external_id]=${anilistId}&include=animethemes.animethemeentries.videos,animethemes.song.artists`;
        } else {
          const cleanTitle = encodeURIComponent((title || '').replace(/(season|part|cour)\s*\d+/gi, '').trim());
          url = `${ANIMETHEMES_URL}/anime?q=${cleanTitle}&include=animethemes.animethemeentries.videos,animethemes.song.artists`;
        }

        const response = await fetch(url);
        if (!response.ok) return [];
        const json = await response.json();
        const animeList = json.anime || [];
        if (animeList.length === 0) return [];

        const firstAnime = animeList[0];
        const rawThemes = firstAnime.animethemes || [];
        const themes = [];

        rawThemes.forEach((t, i) => {
          const song = t.song || {};
          const songTitle = song.title || `${t.type || 'OP'} ${t.sequence || 1}`;
          const artist = (song.artists && song.artists[0]?.name) || '';
          const entries = t.animethemeentries || [];
          if (entries.length === 0) return;

          const videos = entries[0].videos || [];
          if (videos.length === 0) return;

          const video = videos[0];
          themes.push({
            id: t.id || i,
            type: t.type || 'OP', // 'OP' | 'ED'
            sequence: t.sequence || 1,
            title: songTitle,
            artist,
            videoUrl: video.link || '',
            resolution: `${video.resolution || 1080}p`,
            episodes: entries[0].episodes || ''
          });
        });

        setCached(cacheKey, themes);
        return themes;
      } catch (err) {
        console.warn('AnimeThemes error:', err);
        return [];
      }
    },

    // ---- MULTI-SERVER STREAMING RESOLVER ----
    getStreamUrl(server, anime, episodeNumber = 1, language = 'sub') {
      const anilistId = String(anime.id || '');
      const malId = String(anime.malId || anilistId);
      const ep = episodeNumber || 1;
      const lang = language.toLowerCase() === 'dub' ? 'dub' : 'sub';
      const targetId = malId && malId !== '0' ? malId : anilistId;
      const accent = window.ZenithStorage?.getSettings().accent || 'emerald';
      const accentHex = {
        emerald: '10b981',
        cyan: '06b6d4',
        sakura: 'f43f5e',
        violet: '8b5cf6',
        amber: 'f59e0b',
        crimson: 'ef4444',
        indigo: '536dfe',
        gold: 'ffd54f'
      }[accent] || '10b981';

      switch (server) {
        case 'aniembed':
          return `https://aniembed.se/e/${anilistId}/${ep}?lang=${lang}&autoplay=1`;

        case 'viduki':
          return `https://viduki.net/1/tv/${targetId}/1/${ep}?color=${accentHex}`;

        case 'megaplay':
          return malId && malId !== '0'
            ? `https://megaplay.buzz/stream/mal/${malId}/${ep}/${lang}`
            : `https://megaplay.buzz/stream/ani/${anilistId}/${ep}/${lang}`;

        case 'vidsrc':
          return `https://vidsrc.pm/embed/anime/${targetId}/${ep}`;

        default:
          return `https://aniembed.se/e/${anilistId}/${ep}?lang=${lang}&autoplay=1`;
      }
    },

    getAvailableServers() {
      return [
        { id: 'aniembed', name: 'AniEmbed (Recommended)', note: 'Fast HLS • Sub & Dub • AniList synced' },
        { id: 'viduki', name: 'Viduki Multi-Stream', note: 'HD Multi-source • Minimal ads' },
        { id: 'megaplay', name: 'MegaPlay Buzz', note: 'High speed • Sub & Dub • MAL synced' },
        { id: 'vidsrc', name: 'VidSrc Ultra', note: 'Fast global servers • Fallback' }
      ];
    }
  };

  window.ZenithAPI = API;
})(window);
