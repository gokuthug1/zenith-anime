/**
 * Zenith Anime Web — Network & API Service
 * Integrates AniList GraphQL, Jikan (MAL) Reviews, AnimeThemes.moe,
 * and Multi-Server Streaming Embed Resolvers.
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
    const english = titleObj.english && titleObj.english !== 'null' ? titleObj.english : '';
    const romaji = titleObj.romaji && titleObj.romaji !== 'null' ? titleObj.romaji : '';
    const native = titleObj.native && titleObj.native !== 'null' ? titleObj.native : '';
    const mainTitle = english || romaji || native || 'Untitled Anime';

    const cover = (item.coverImage && (item.coverImage.extraLarge || item.coverImage.large)) || '';
    const banner = item.bannerImage || cover;

    // Clean up HTML tags from description
    const rawDesc = item.description || 'No description available.';
    const cleanDesc = rawDesc.replace(/<[^>]*>/g, '').trim();

    // Studio name
    let studio = 'Unknown';
    if (item.studios && item.studios.nodes && item.studios.nodes.length > 0) {
      studio = item.studios.nodes[0].name || 'Unknown';
    }

    // Rating normalized to 10-point scale
    const rawScore = item.averageScore || 0;
    const rating = rawScore > 0 ? (rawScore / 10).toFixed(1) : '—';

    // Format episode count
    const totalEpisodes = item.episodes || 0;

    return {
      id: String(item.id),
      malId: String(item.idMal || ''),
      title: mainTitle,
      englishTitle: english || mainTitle,
      romajiTitle: romaji || mainTitle,
      nativeTitle: native,
      coverImage: cover,
      bannerImage: banner,
      synopsis: cleanDesc,
      rating: parseFloat(rating) || 0,
      ratingDisplay: rating,
      year: item.seasonYear || '',
      season: item.season || '',
      status: item.status || 'FINISHED',
      type: item.format || 'TV',
      totalEpisodes,
      genres: Array.isArray(item.genres) ? item.genres.join(', ') : (item.genres || ''),
      genreList: item.genres || [],
      studio,
      nextAiringEpisode: item.nextAiringEpisode || null
    };
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
        if (list.length > 0) {
          return list[Math.floor(Math.random() * list.length)];
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

    // ---- ANIME DETAILS (WITH CHARACTERS & RELATIONS) ----
    async getAnimeDetails(id) {
      const isMal = String(id).startsWith('mal_');
      const numericId = isMal ? parseInt(id.replace('mal_', ''), 10) : parseInt(id, 10);
      const filterArg = isMal ? 'idMal: $id' : 'id: $id';

      const query = `
        query ($id: Int) {
          Media(${filterArg}, type: ANIME) {
            ${MEDIA_FIELDS}
            characters(page: 1, perPage: 14, sort: [ROLE, RELEVANCE]) {
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
                  title {
                    english
                    romaji
                  }
                  format
                  coverImage {
                    large
                  }
                  averageScore
                  status
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

      // Parse Relations
      const relations = [];
      const relEdges = media.relations?.edges || [];
      relEdges.forEach((edge) => {
        const node = edge.node;
        if (!node) return;
        const relTitle = node.title?.english || node.title?.romaji || 'Related Anime';
        relations.push({
          id: String(node.id),
          relationType: edge.relationType || 'RELATED',
          title: relTitle,
          format: node.format || 'TV',
          coverImage: node.coverImage?.large || '',
          rating: node.averageScore ? (node.averageScore / 10).toFixed(1) : '',
          status: node.status || ''
        });
      });

      base.characters = characters;
      base.relations = relations;

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
                reviews(page: 1, perPage: 8, sort: [RATING_DESC, ID_DESC]) {
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
                reviews(page: 1, perPage: 8, sort: [RATING_DESC, ID_DESC]) {
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
            const avatar = user.avatar?.medium || '';
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
      if (targetMalId && collectedReviews.length < 4) {
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
