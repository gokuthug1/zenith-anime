/**
 * Zenith Anime Web — Local-First Storage & State Management
 * Persistent storage using LocalStorage with continuous progress tracking,
 * full library organization (with Android LibrarySort parity), aired episode awareness,
 * and backup export/import compatible with the Zenith Anime Android APK.
 */

(function (window) {
  'use strict';

  const KEYS = {
    SETTINGS: 'zenith_settings',
    LIBRARY: 'zenith_library',
    PROGRESS: 'zenith_watch_progress',
    RECENT_SEARCHES: 'zenith_recent_searches',
    NOTIFICATIONS: 'zenith_notifications',
    DAILY_USAGE: 'zenith_daily_usage',
    ANIME_STATS: 'zenith_anime_watch_stats'
  };

  const DATE_KEY_RE = /^\d{4}-\d{2}-\d{2}$/;

  function pad2(n) {
    return String(n).padStart(2, '0');
  }

  function formatDateKeyFromDate(d) {
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  }

  function parseDateKeyLocal(dateKey) {
    if (!DATE_KEY_RE.test(String(dateKey || ''))) return null;
    const [y, m, d] = dateKey.split('-').map(Number);
    const dt = new Date(y, m - 1, d, 12, 0, 0, 0);
    if (dt.getFullYear() !== y || dt.getMonth() !== m - 1 || dt.getDate() !== d) return null;
    return dt;
  }

  function shiftDateKey(dateKey, deltaDays) {
    const dt = parseDateKeyLocal(dateKey) || new Date();
    dt.setDate(dt.getDate() + deltaDays);
    return formatDateKeyFromDate(dt);
  }

  function shortDayLabel(dateKey) {
    const dt = parseDateKeyLocal(dateKey);
    if (!dt) return String(dateKey).slice(-5);
    return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][dt.getDay()];
  }

  const DEFAULT_SETTINGS = {
    accent: 'emerald',
    titleLanguage: 'english', // 'english' | 'romaji' | 'native'
    defaultServer: 'aniembed', // 'aniembed' | 'viduki' | 'megaplay' | 'vidsrc'
    defaultAudio: 'sub', // 'sub' | 'dub'
    episodeLayout: 'grid', // 'grid' | 'numbers' | 'list'
    spoilerBlurCovers: false,
    continueWatchingShowNextEpisode: true,
    librarySortOrder: 'updated', // 'updated' | 'title' | 'title_desc' | 'score' | 'year' | 'episodes'
    reduceAnimations: false
  };

  function safeGet(key, fallback) {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : fallback;
    } catch (e) {
      console.warn('Storage read failed for', key, e);
      return fallback;
    }
  }

  function safeSet(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
      console.warn('Storage write failed for', key, e);
    }
  }

  function buildLibraryRecord(anime, existing = {}) {
    const norm = window.ZenithAPI?.normalizeAnime ? window.ZenithAPI.normalizeAnime(anime) : anime;
    return {
      id: String(norm.id),
      malId: norm.malId || existing.malId || '',
      title: norm.title || existing.title || 'Anime',
      englishTitle: norm.englishTitle || norm.title || existing.englishTitle || '',
      romajiTitle: norm.romajiTitle || norm.title || existing.romajiTitle || '',
      nativeTitle: norm.nativeTitle || existing.nativeTitle || '',
      coverImage: norm.coverImage || existing.coverImage || '',
      bannerImage: norm.bannerImage || norm.coverImage || existing.bannerImage || '',
      rating: Number(norm.rating ?? existing.rating ?? 0),
      ratingDisplay: norm.ratingDisplay || (norm.rating > 0 ? Number(norm.rating).toFixed(1) : (existing.ratingDisplay || '—')),
      year: norm.year || existing.year || '',
      type: norm.type || existing.type || 'TV',
      mediaStatus: norm.status !== 'WATCHING' && norm.status !== 'COMPLETED' && norm.status !== 'PLAN_TO_WATCH' && norm.status !== 'ON_HOLD' && norm.status !== 'DROPPED'
        ? (norm.status || existing.mediaStatus || 'FINISHED')
        : (existing.mediaStatus || 'FINISHED'),
      isUpcoming: typeof norm.isUpcoming === 'boolean' ? norm.isUpcoming : Boolean(existing.isUpcoming),
      isAiring: typeof norm.isAiring === 'boolean' ? norm.isAiring : Boolean(existing.isAiring),
      totalEpisodes: Number(norm.totalEpisodes ?? existing.totalEpisodes ?? 0),
      availableEpisodes: typeof norm.availableEpisodes === 'number'
        ? norm.availableEpisodes
        : Number(existing.availableEpisodes ?? norm.totalEpisodes ?? 0),
      nextAiringEpisode: norm.nextAiringEpisode || existing.nextAiringEpisode || null,
      genres: norm.genres || existing.genres || '',
      isFavorite: Boolean(existing.isFavorite),
      status: existing.status || null,
      addedAt: existing.addedAt || Date.now(),
      updatedAt: Date.now()
    };
  }

  const Storage = {
    // ---- SETTINGS ----
    getSettings() {
      const saved = safeGet(KEYS.SETTINGS, {});
      const standaloneAccent = localStorage.getItem('zenith_accent');
      if (standaloneAccent && !saved.accent) {
        saved.accent = standaloneAccent;
      }
      return Object.assign({}, DEFAULT_SETTINGS, saved);
    },

    updateSettings(partial) {
      const current = this.getSettings();
      const updated = Object.assign({}, current, partial);
      safeSet(KEYS.SETTINGS, updated);
      if (partial.accent) {
        try {
          localStorage.setItem('zenith_accent', partial.accent);
        } catch {
          // ignore
        }
      }
      return updated;
    },

    // ---- DISPLAY TITLE HELPER (Ported from Android Anime.displayTitle) ----
    getDisplayTitle(anime) {
      if (!anime) return 'Anime';
      const pref = (this.getSettings().titleLanguage || 'english').toLowerCase();
      if (pref === 'romaji') {
        return anime.romajiTitle || anime.title || anime.englishTitle || 'Anime';
      }
      if (pref === 'native' || pref === 'japanese') {
        return anime.nativeTitle || anime.romajiTitle || anime.title || 'Anime';
      }
      return anime.englishTitle || anime.title || anime.romajiTitle || 'Anime';
    },

    // ---- RECENT SEARCHES ----
    getRecentSearches() {
      return safeGet(KEYS.RECENT_SEARCHES, []);
    },

    addRecentSearch(query) {
      if (!query || !query.trim()) return;
      const q = query.trim();
      const current = this.getRecentSearches().filter(s => s.toLowerCase() !== q.toLowerCase());
      const updated = [q, ...current].slice(0, 10);
      safeSet(KEYS.RECENT_SEARCHES, updated);
    },

    clearRecentSearches() {
      safeSet(KEYS.RECENT_SEARCHES, []);
    },

    // ---- WATCHLIST & LIBRARY ----
    getLibrary() {
      return safeGet(KEYS.LIBRARY, {});
    },

    getAnimeEntry(animeId) {
      const lib = this.getLibrary();
      return lib[String(animeId)] || null;
    },

    isFavorite(animeId) {
      const entry = this.getAnimeEntry(animeId);
      return Boolean(entry && entry.isFavorite);
    },

    getWatchStatus(animeId) {
      const entry = this.getAnimeEntry(animeId);
      return entry ? entry.status : null;
    },

    setWatchStatus(anime, status) {
      if (!anime || !anime.id) return null;
      const lib = this.getLibrary();
      const id = String(anime.id);
      const existing = buildLibraryRecord(anime, lib[id] || {});

      if (!status || status === 'REMOVE') {
        if (!existing.isFavorite) {
          delete lib[id];
        } else {
          existing.status = null;
          existing.updatedAt = Date.now();
          lib[id] = existing;
        }
      } else {
        existing.status = status;
        existing.updatedAt = Date.now();
        lib[id] = existing;
      }
      safeSet(KEYS.LIBRARY, lib);
      return lib[id] || null;
    },

    toggleFavorite(anime) {
      if (!anime || !anime.id) return false;
      const lib = this.getLibrary();
      const id = String(anime.id);
      const existing = buildLibraryRecord(anime, lib[id] || {});

      existing.isFavorite = !existing.isFavorite;
      existing.updatedAt = Date.now();

      if (!existing.isFavorite && !existing.status) {
        delete lib[id];
        safeSet(KEYS.LIBRARY, lib);
        return false;
      } else {
        lib[id] = existing;
        safeSet(KEYS.LIBRARY, lib);
        return existing.isFavorite;
      }
    },

    importWatchlistItems(animeList, replaceAll = false) {
      if (!Array.isArray(animeList)) return 0;
      const lib = replaceAll ? {} : this.getLibrary();
      let count = 0;

      animeList.forEach((item) => {
        if (!item || !item.id) return;
        const id = String(item.id);
        const existing = lib[id] || {};
        const record = buildLibraryRecord(item, existing);
        record.status = item.watchStatus || existing.status || 'WATCHING';
        record.updatedAt = Date.now();
        lib[id] = record;
        count++;
      });

      safeSet(KEYS.LIBRARY, lib);
      return count;
    },

    getFilteredLibrary(tab = 'ALL', sortBy = 'updated', searchQuery = '') {
      const lib = this.getLibrary();
      let items = Object.values(lib).map(entry => {
        // Hydrate mediaStatus back to status on clone for card rendering while keeping watchStatus
        const hydrated = Object.assign({}, entry, {
          watchStatus: entry.status,
          status: entry.mediaStatus || (entry.isUpcoming ? 'NOT_YET_RELEASED' : (entry.isAiring ? 'RELEASING' : 'FINISHED'))
        });
        return window.ZenithAPI?.normalizeAnime ? window.ZenithAPI.normalizeAnime(hydrated) : hydrated;
      });

      switch (tab) {
        case 'WATCHING':
          items = items.filter(i => i.watchStatus === 'WATCHING');
          break;
        case 'COMPLETED':
          items = items.filter(i => i.watchStatus === 'COMPLETED');
          break;
        case 'PLAN_TO_WATCH':
          items = items.filter(i => i.watchStatus === 'PLAN_TO_WATCH');
          break;
        case 'ON_HOLD':
          items = items.filter(i => i.watchStatus === 'ON_HOLD');
          break;
        case 'DROPPED':
          items = items.filter(i => i.watchStatus === 'DROPPED');
          break;
        case 'FAVORITES':
          items = items.filter(i => i.isFavorite);
          break;
        case 'ALL':
        default:
          items = items.filter(i => i.watchStatus || i.isFavorite);
          break;
      }

      if (searchQuery && searchQuery.trim()) {
        const q = searchQuery.trim().toLowerCase();
        items = items.filter(i =>
          (i.title || '').toLowerCase().includes(q) ||
          (i.englishTitle || '').toLowerCase().includes(q) ||
          (i.romajiTitle || '').toLowerCase().includes(q) ||
          (i.genres || '').toLowerCase().includes(q)
        );
      }

      // Ported from Android LibrarySort.kt
      items.sort((a, b) => {
        const titleA = this.getDisplayTitle(a);
        const titleB = this.getDisplayTitle(b);
        switch (sortBy) {
          case 'title':
            return titleA.localeCompare(titleB);
          case 'title_desc':
            return titleB.localeCompare(titleA);
          case 'score':
            return (b.rating || 0) - (a.rating || 0) || titleA.localeCompare(titleB);
          case 'year':
            return (Number(b.year) || 0) - (Number(a.year) || 0) || titleA.localeCompare(titleB);
          case 'episodes':
            return (b.availableEpisodes || b.totalEpisodes || 0) - (a.availableEpisodes || a.totalEpisodes || 0) || titleA.localeCompare(titleB);
          case 'updated':
          default:
            return (b.updatedAt || b.addedAt || 0) - (a.updatedAt || a.addedAt || 0);
        }
      });

      return items;
    },

    // ---- WATCH PROGRESS & CONTINUE WATCHING ----
    getAllProgress() {
      return safeGet(KEYS.PROGRESS, {});
    },

    getProgress(animeId, episodeNumber) {
      const all = this.getAllProgress();
      const key = `${animeId}_ep_${episodeNumber}`;
      return all[key] || all[`${animeId}_${episodeNumber}`] || null;
    },

    getLatestProgressForAnime(animeId) {
      const all = this.getAllProgress();
      const matches = Object.values(all).filter(p => String(p.animeId) === String(animeId));
      if (matches.length === 0) return null;
      matches.sort((a, b) => (b.lastWatchedAt || 0) - (a.lastWatchedAt || 0));
      return matches[0];
    },

    saveProgress(data) {
      if (!data || !data.animeId || !data.episodeNumber) return null;
      const all = this.getAllProgress();
      const key = `${data.animeId}_ep_${data.episodeNumber}`;
      const now = Date.now();
      const existing = all[key] || {};

      const duration = data.durationSec || existing.durationSec || 1440; // ~24 min fallback
      const currentTime = Math.min(duration, Math.max(0, data.currentTimeSec || 0));
      const isCompleted = data.completed !== undefined
        ? data.completed
        : (duration > 0 && (currentTime / duration) >= 0.88);

      const availableEps = typeof data.availableEpisodes === 'number'
        ? data.availableEpisodes
        : (existing.availableEpisodes || 0);

      all[key] = {
        id: key,
        animeId: String(data.animeId),
        animeTitle: data.animeTitle || existing.animeTitle || 'Anime',
        episodeNumber: Number(data.episodeNumber),
        episodeTitle: data.episodeTitle || existing.episodeTitle || `Episode ${data.episodeNumber}`,
        coverImage: data.coverImage || existing.coverImage || '',
        bannerImage: data.bannerImage || existing.bannerImage || '',
        currentTimeSec: Math.floor(currentTime),
        durationSec: Math.floor(duration),
        language: data.language || existing.language || 'sub',
        completed: isCompleted,
        availableEpisodes: availableEps,
        lastWatchedAt: now
      };

      // Auto-queue next aired episode in Continue Watching if episode completed (Android selfHealWatchProgress parity)
      const settings = this.getSettings();
      if (isCompleted && settings.continueWatchingShowNextEpisode && availableEps > Number(data.episodeNumber)) {
        const nextEp = Number(data.episodeNumber) + 1;
        const nextKey = `${data.animeId}_ep_${nextEp}`;
        if (!all[nextKey]) {
          all[nextKey] = {
            id: nextKey,
            animeId: String(data.animeId),
            animeTitle: data.animeTitle || existing.animeTitle || 'Anime',
            episodeNumber: nextEp,
            episodeTitle: `Episode ${nextEp}`,
            coverImage: data.coverImage || existing.coverImage || '',
            bannerImage: data.bannerImage || existing.bannerImage || '',
            currentTimeSec: 0,
            durationSec: 1440,
            language: data.language || existing.language || 'sub',
            completed: false,
            availableEpisodes: availableEps,
            lastWatchedAt: now + 5
          };
        }
      }

      safeSet(KEYS.PROGRESS, all);

      // Auto-update library status to WATCHING if not already set
      const libEntry = this.getAnimeEntry(data.animeId);
      if (!libEntry || !libEntry.status) {
        this.setWatchStatus({
          id: data.animeId,
          title: data.animeTitle,
          coverImage: data.coverImage,
          bannerImage: data.bannerImage,
          genres: data.genres || libEntry?.genres || '',
          availableEpisodes: availableEps
        }, 'WATCHING');
      }

      if (isCompleted && !existing.completed) {
        this.recordEpisodeCompleted({
          id: data.animeId,
          title: data.animeTitle || existing.animeTitle || 'Anime',
          coverImage: data.coverImage || existing.coverImage || '',
          genres: data.genres || libEntry?.genres || ''
        }, now);
      }

      return all[key];
    },

    toggleEpisodeWatched(anime, episodeNumber) {
      if (!anime || !anime.id) return false;
      const existing = this.getProgress(anime.id, episodeNumber);
      const nextCompleted = !(existing && existing.completed);
      const duration = existing?.durationSec || 1440;
      const epMeta = anime.episodeMetadata && anime.episodeMetadata[episodeNumber];
      this.saveProgress({
        animeId: anime.id,
        animeTitle: this.getDisplayTitle(anime),
        episodeNumber: Number(episodeNumber),
        episodeTitle: epMeta?.title || existing?.episodeTitle || `Episode ${episodeNumber}`,
        coverImage: anime.coverImage,
        bannerImage: anime.bannerImage,
        currentTimeSec: nextCompleted ? duration : 0,
        durationSec: duration,
        availableEpisodes: anime.availableEpisodes,
        completed: nextCompleted
      });
      return nextCompleted;
    },

    deleteProgress(animeId, episodeNumber) {
      const all = this.getAllProgress();
      const key = `${animeId}_ep_${episodeNumber}`;
      const legacyKey = `${animeId}_${episodeNumber}`;
      let changed = false;
      if (all[key]) {
        delete all[key];
        changed = true;
      }
      if (all[legacyKey]) {
        delete all[legacyKey];
        changed = true;
      }
      if (changed) safeSet(KEYS.PROGRESS, all);
    },

    deleteProgressForAnime(animeId) {
      const all = this.getAllProgress();
      let changed = false;
      Object.keys(all).forEach(k => {
        if (String(all[k].animeId) === String(animeId)) {
          delete all[k];
          changed = true;
        }
      });
      if (changed) {
        safeSet(KEYS.PROGRESS, all);
      }
    },

    clearAllProgress() {
      safeSet(KEYS.PROGRESS, {});
    },

    getContinueWatchingList(limit = 12) {
      const all = this.getAllProgress();
      const progressList = Object.values(all);

      // Group by anime ID to pick the latest episode per series
      const byAnime = {};
      progressList.forEach(p => {
        if (!byAnime[p.animeId] || (p.lastWatchedAt || 0) > (byAnime[p.animeId].lastWatchedAt || 0)) {
          byAnime[p.animeId] = p;
        }
      });

      const uniqueList = Object.values(byAnime);
      uniqueList.sort((a, b) => (b.lastWatchedAt || 0) - (a.lastWatchedAt || 0));
      return uniqueList.slice(0, limit);
    },

    getHistoryList(limit = 50) {
      const all = this.getAllProgress();
      const progressList = Object.values(all);
      progressList.sort((a, b) => (b.lastWatchedAt || 0) - (a.lastWatchedAt || 0));
      return progressList.slice(0, limit);
    },

    // ---- WATCH TIME & APP USAGE STATISTICS ----
    getLocalDateKey(ts = Date.now()) {
      return formatDateKeyFromDate(new Date(ts));
    },

    formatDuration(totalSec) {
      const safe = Math.max(0, Math.floor(Number(totalSec) || 0));
      if (safe < 60) return `${safe}s`;
      const totalMinutes = Math.floor(safe / 60);
      const hours = Math.floor(totalMinutes / 60);
      const minutes = totalMinutes % 60;
      if (hours >= 24) {
        const days = Math.floor(hours / 24);
        const remHours = hours % 24;
        return remHours > 0 ? `${days}d ${remHours}h` : `${days}d`;
      }
      if (hours > 0) {
        return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
      }
      return `${minutes}m`;
    },

    getDailyUsage() {
      return safeGet(KEYS.DAILY_USAGE, {});
    },

    getAnimeWatchStats() {
      return safeGet(KEYS.ANIME_STATS, {});
    },

    recordAppUsage(deltaSec, options = {}) {
      const ts = Number(options.timestampMs) || Date.now();
      const incrementSession = Boolean(options.incrementSession);
      const raw = Number(deltaSec) || 0;
      const clamped = raw > 0 ? Math.min(60, Math.max(1, Math.floor(raw))) : 0;
      if (clamped === 0 && !incrementSession) return null;

      const daily = this.getDailyUsage();
      const dateKey = this.getLocalDateKey(ts);
      const existing = daily[dateKey] || {
        dateKey,
        appTimeSec: 0,
        watchTimeSec: 0,
        episodesCompleted: 0,
        sessionsCount: 0,
        updatedAt: ts
      };

      daily[dateKey] = {
        dateKey,
        appTimeSec: Math.max(0, Number(existing.appTimeSec) || 0) + clamped,
        watchTimeSec: Math.max(0, Number(existing.watchTimeSec) || 0),
        episodesCompleted: Math.max(0, Number(existing.episodesCompleted) || 0),
        sessionsCount: Math.max(0, Number(existing.sessionsCount) || 0) + (incrementSession ? 1 : 0),
        updatedAt: ts
      };
      safeSet(KEYS.DAILY_USAGE, daily);
      return daily[dateKey];
    },

    recordWatchTime(anime, deltaSec, timestampMs = Date.now()) {
      if (!anime || !anime.id) return null;
      const raw = Number(deltaSec) || 0;
      if (raw <= 0) return null;
      const clamped = Math.min(60, Math.max(1, Math.floor(raw)));
      const ts = Number(timestampMs) || Date.now();

      const daily = this.getDailyUsage();
      const dateKey = this.getLocalDateKey(ts);
      const existingDaily = daily[dateKey] || {
        dateKey,
        appTimeSec: 0,
        watchTimeSec: 0,
        episodesCompleted: 0,
        sessionsCount: 0,
        updatedAt: ts
      };
      daily[dateKey] = {
        dateKey,
        appTimeSec: Math.max(0, Number(existingDaily.appTimeSec) || 0),
        watchTimeSec: Math.max(0, Number(existingDaily.watchTimeSec) || 0) + clamped,
        episodesCompleted: Math.max(0, Number(existingDaily.episodesCompleted) || 0),
        sessionsCount: Math.max(0, Number(existingDaily.sessionsCount) || 0),
        updatedAt: ts
      };
      safeSet(KEYS.DAILY_USAGE, daily);

      const animeId = String(anime.id);
      const stats = this.getAnimeWatchStats();
      const existingAnime = stats[animeId] || {};
      const libEntry = this.getAnimeEntry(animeId);
      const resolvedTitle = this.getDisplayTitle(anime) !== 'Anime'
        ? this.getDisplayTitle(anime)
        : (anime.title || existingAnime.animeTitle || libEntry?.title || `Anime #${animeId}`);

      stats[animeId] = {
        animeId,
        animeTitle: resolvedTitle || existingAnime.animeTitle || '',
        coverImage: anime.coverImage || existingAnime.coverImage || libEntry?.coverImage || '',
        genres: anime.genres || existingAnime.genres || libEntry?.genres || '',
        watchTimeSec: Math.max(0, Number(existingAnime.watchTimeSec) || 0) + clamped,
        episodesWatchedCount: Math.max(0, Number(existingAnime.episodesWatchedCount) || 0),
        lastWatchedAt: ts
      };
      safeSet(KEYS.ANIME_STATS, stats);
      return stats[animeId];
    },

    recordEpisodeCompleted(anime, timestampMs = Date.now()) {
      if (!anime || !anime.id) return null;
      const ts = Number(timestampMs) || Date.now();

      const daily = this.getDailyUsage();
      const dateKey = this.getLocalDateKey(ts);
      const existingDaily = daily[dateKey] || {
        dateKey,
        appTimeSec: 0,
        watchTimeSec: 0,
        episodesCompleted: 0,
        sessionsCount: 0,
        updatedAt: ts
      };
      daily[dateKey] = {
        dateKey,
        appTimeSec: Math.max(0, Number(existingDaily.appTimeSec) || 0),
        watchTimeSec: Math.max(0, Number(existingDaily.watchTimeSec) || 0),
        episodesCompleted: Math.max(0, Number(existingDaily.episodesCompleted) || 0) + 1,
        sessionsCount: Math.max(0, Number(existingDaily.sessionsCount) || 0),
        updatedAt: ts
      };
      safeSet(KEYS.DAILY_USAGE, daily);

      const animeId = String(anime.id);
      const stats = this.getAnimeWatchStats();
      const existingAnime = stats[animeId] || {};
      const libEntry = this.getAnimeEntry(animeId);

      stats[animeId] = {
        animeId,
        animeTitle: anime.title || existingAnime.animeTitle || libEntry?.title || `Anime #${animeId}`,
        coverImage: anime.coverImage || existingAnime.coverImage || libEntry?.coverImage || '',
        genres: anime.genres || existingAnime.genres || libEntry?.genres || '',
        watchTimeSec: Math.max(0, Number(existingAnime.watchTimeSec) || 0),
        episodesWatchedCount: Math.max(0, Number(existingAnime.episodesWatchedCount) || 0) + 1,
        lastWatchedAt: ts
      };
      safeSet(KEYS.ANIME_STATS, stats);
      return stats[animeId];
    },

    getUsageStatsSummary(range = '7d', todayKey = this.getLocalDateKey()) {
      const dailyMap = this.getDailyUsage();
      const animeMap = this.getAnimeWatchStats();

      const daysCount = range === 'today' ? 1 : (range === '7d' ? 7 : (range === '30d' ? 30 : null));
      const cutoffKey = daysCount ? shiftDateKey(todayKey, -(daysCount - 1)) : null;

      const validDaily = Object.values(dailyMap).filter(d => d && DATE_KEY_RE.test(d.dateKey));
      const filteredDaily = validDaily.filter(d => !cutoffKey || (d.dateKey >= cutoffKey && d.dateKey <= todayKey));

      const totalWatchTimeSec = filteredDaily.reduce((acc, d) => acc + Math.max(0, Number(d.watchTimeSec) || 0), 0);
      const totalAppTimeSec = filteredDaily.reduce((acc, d) => acc + Math.max(0, Number(d.appTimeSec) || 0), 0);
      const episodesCompleted = filteredDaily.reduce((acc, d) => acc + Math.max(0, Number(d.episodesCompleted) || 0), 0);
      const sessionsCount = filteredDaily.reduce((acc, d) => acc + Math.max(0, Number(d.sessionsCount) || 0), 0);
      const activeDays = filteredDaily.filter(d => (d.watchTimeSec || 0) > 0 || (d.appTimeSec || 0) > 0 || (d.episodesCompleted || 0) > 0).length;

      const divisor = daysCount || Math.max(1, activeDays);
      const avgDailyWatchTimeSec = Math.floor(totalWatchTimeSec / divisor);

      const activeKeySet = new Set(
        validDaily
          .filter(d => (d.watchTimeSec || 0) > 0 || (d.appTimeSec || 0) > 0 || (d.episodesCompleted || 0) > 0)
          .map(d => d.dateKey)
      );
      let currentStreakDays = 0;
      let cursor = activeKeySet.has(todayKey) ? todayKey : shiftDateKey(todayKey, -1);
      while (activeKeySet.has(cursor)) {
        currentStreakDays++;
        cursor = shiftDateKey(cursor, -1);
      }

      const sortedActiveKeys = Array.from(activeKeySet).sort();
      let longestStreakDays = 0;
      let run = 0;
      let prevKey = null;
      sortedActiveKeys.forEach(k => {
        if (prevKey && shiftDateKey(prevKey, 1) === k) {
          run++;
        } else {
          run = 1;
        }
        if (run > longestStreakDays) longestStreakDays = run;
        prevKey = k;
      });

      const chartWindowDays = (range === '30d' || range === 'all') ? 14 : 7;
      const chartDays = [];
      for (let offset = -(chartWindowDays - 1); offset <= 0; offset++) {
        const key = shiftDateKey(todayKey, offset);
        const row = dailyMap[key];
        chartDays.push({
          dateKey: key,
          shortLabel: offset === 0 ? 'Today' : shortDayLabel(key),
          watchTimeSec: Math.max(0, Number(row?.watchTimeSec) || 0),
          appTimeSec: Math.max(0, Number(row?.appTimeSec) || 0),
          episodesCompleted: Math.max(0, Number(row?.episodesCompleted) || 0)
        });
      }

      const topAnime = Object.values(animeMap)
        .filter(a => a && ((a.watchTimeSec || 0) > 0 || (a.episodesWatchedCount || 0) > 0))
        .sort((a, b) =>
          (b.watchTimeSec || 0) - (a.watchTimeSec || 0) ||
          (b.episodesWatchedCount || 0) - (a.episodesWatchedCount || 0) ||
          (b.lastWatchedAt || 0) - (a.lastWatchedAt || 0)
        )
        .slice(0, 8);

      const genreTotals = {};
      Object.values(animeMap).forEach(stat => {
        if (!stat || ((stat.watchTimeSec || 0) <= 0 && (stat.episodesWatchedCount || 0) <= 0)) return;
        const parsed = String(stat.genres || '')
          .split(',')
          .map(g => g.trim())
          .filter(g => g && g.toLowerCase() !== 'anime');
        const unique = Array.from(new Set(parsed));
        unique.forEach(g => {
          if (!genreTotals[g]) {
            genreTotals[g] = { genre: g, watchTimeSec: 0, animeIds: new Set() };
          }
          genreTotals[g].watchTimeSec += Math.max(0, Number(stat.watchTimeSec) || 0);
          genreTotals[g].animeIds.add(String(stat.animeId));
        });
      });

      const topGenres = Object.values(genreTotals)
        .map(g => ({ genre: g.genre, watchTimeSec: g.watchTimeSec, animeCount: g.animeIds.size }))
        .sort((a, b) => b.watchTimeSec - a.watchTimeSec || b.animeCount - a.animeCount)
        .slice(0, 6);

      return {
        range,
        totalWatchTimeSec,
        totalAppTimeSec,
        episodesCompleted,
        sessionsCount,
        activeDays,
        avgDailyWatchTimeSec,
        currentStreakDays,
        longestStreakDays,
        chartDays,
        topAnime,
        topGenres
      };
    },

    clearUsageStats() {
      localStorage.removeItem(KEYS.DAILY_USAGE);
      localStorage.removeItem(KEYS.ANIME_STATS);
    },

    // ---- BACKUP EXPORT & IMPORT (100% Android APK Compatible) ----
    exportBackupJson() {
      const lib = this.getLibrary();
      const progress = this.getAllProgress();
      const settings = this.getSettings();
      const dailyUsage = this.getDailyUsage();
      const animeStats = this.getAnimeWatchStats();

      const backup = {
        version: 2,
        exportedAt: Date.now(),
        app: 'ZenithAnime',
        settings,
        animeState: Object.values(lib).map(a => ({
          id: a.id,
          malId: a.malId || '',
          title: a.title,
          englishTitle: a.englishTitle || a.title,
          romajiTitle: a.romajiTitle || a.title,
          nativeTitle: a.nativeTitle || '',
          coverImage: a.coverImage,
          bannerImage: a.bannerImage,
          rating: a.rating,
          year: a.year,
          type: a.type,
          status: a.mediaStatus || 'FINISHED',
          totalEpisodes: a.totalEpisodes,
          availableEpisodes: a.availableEpisodes,
          genres: a.genres,
          isFavorite: Boolean(a.isFavorite),
          isInWatchlist: Boolean(a.status),
          isCompleted: a.status === 'COMPLETED',
          watchStatus: a.status || '',
          lastUpdated: a.updatedAt || a.addedAt || Date.now()
        })),
        progressState: Object.values(progress).map(p => ({
          id: p.id,
          animeId: p.animeId,
          episodeNumber: p.episodeNumber,
          episodeTitle: p.episodeTitle,
          animeTitle: p.animeTitle,
          coverImage: p.coverImage,
          currentTimeSec: p.currentTimeSec,
          durationSec: p.durationSec,
          language: p.language,
          completed: p.completed,
          lastWatchedAt: p.lastWatchedAt
        })),
        usageStats: {
          schemaVersion: 1,
          dailyUsage: Object.values(dailyUsage).map(d => ({
            dateKey: d.dateKey,
            appTimeSec: Math.max(0, Number(d.appTimeSec) || 0),
            watchTimeSec: Math.max(0, Number(d.watchTimeSec) || 0),
            episodesCompleted: Math.max(0, Number(d.episodesCompleted) || 0),
            sessionsCount: Math.max(0, Number(d.sessionsCount) || 0),
            updatedAt: Number(d.updatedAt) || Date.now()
          })),
          animeStats: Object.values(animeStats).map(a => ({
            animeId: String(a.animeId),
            animeTitle: a.animeTitle || '',
            coverImage: a.coverImage || '',
            genres: a.genres || '',
            watchTimeSec: Math.max(0, Number(a.watchTimeSec) || 0),
            episodesWatchedCount: Math.max(0, Number(a.episodesWatchedCount) || 0),
            lastWatchedAt: Number(a.lastWatchedAt) || Date.now()
          }))
        }
      };

      return JSON.stringify(backup, null, 2);
    },

    importBackupJson(jsonString, options = {}) {
      const replaceAll = Boolean(options.replaceAll);
      try {
        const parsed = JSON.parse(jsonString);
        if (!parsed || (parsed.app !== 'ZenithAnime' && !Array.isArray(parsed.animeState) && !Array.isArray(parsed.progressState) && !Array.isArray(parsed.watchProgress))) {
          throw new Error('Invalid Zenith Anime backup file format');
        }

        const lib = replaceAll ? {} : this.getLibrary();
        if (Array.isArray(parsed.animeState)) {
          parsed.animeState.forEach(a => {
            if (a && a.id) {
              const id = String(a.id);
              const existing = lib[id] || {};
              const record = buildLibraryRecord(a, existing);
              record.isFavorite = replaceAll ? Boolean(a.isFavorite) : Boolean(a.isFavorite || existing.isFavorite);
              record.status = a.watchStatus || (a.isCompleted ? 'COMPLETED' : (a.isInWatchlist ? 'WATCHING' : (replaceAll ? null : existing.status)));
              record.addedAt = a.lastUpdated || existing.addedAt || Date.now();
              record.updatedAt = a.lastUpdated || Date.now();
              if (record.isFavorite || record.status) {
                lib[id] = record;
              }
            }
          });
          safeSet(KEYS.LIBRARY, lib);
        }

        const progress = replaceAll ? {} : this.getAllProgress();
        const incomingProgress = Array.isArray(parsed.progressState)
          ? parsed.progressState
          : (Array.isArray(parsed.watchProgress) ? parsed.watchProgress : []);
        if (incomingProgress.length > 0 || replaceAll) {
          incomingProgress.forEach(p => {
            if (p && p.animeId && p.episodeNumber) {
              const key = `${p.animeId}_ep_${p.episodeNumber}`;
              const existing = progress[key];
              if (!replaceAll && existing && (existing.lastWatchedAt || 0) > (Number(p.lastWatchedAt) || 0)) {
                return;
              }
              progress[key] = {
                id: key,
                animeId: String(p.animeId),
                animeTitle: p.animeTitle || existing?.animeTitle || 'Anime',
                episodeNumber: Number(p.episodeNumber),
                episodeTitle: p.episodeTitle || existing?.episodeTitle || `Episode ${p.episodeNumber}`,
                coverImage: p.coverImage || existing?.coverImage || '',
                bannerImage: p.bannerImage || p.coverImage || existing?.bannerImage || '',
                currentTimeSec: Number(p.currentTimeSec) || 0,
                durationSec: Number(p.durationSec) || 1440,
                language: p.language || 'sub',
                completed: Boolean(p.completed || existing?.completed),
                lastWatchedAt: Number(p.lastWatchedAt) || Date.now()
              };
            }
          });
          safeSet(KEYS.PROGRESS, progress);
        }

        const incomingUsage = parsed.usageStats;
        if (replaceAll) {
          this.clearUsageStats();
        }
        if (incomingUsage && typeof incomingUsage === 'object') {
          const daily = replaceAll ? {} : this.getDailyUsage();
          if (Array.isArray(incomingUsage.dailyUsage)) {
            incomingUsage.dailyUsage.forEach(d => {
              if (!d || !DATE_KEY_RE.test(String(d.dateKey || ''))) return;
              const dk = String(d.dateKey);
              const inApp = Math.max(0, Number(d.appTimeSec) || 0);
              const inWatch = Math.max(0, Number(d.watchTimeSec) || 0);
              const inEps = Math.max(0, Number(d.episodesCompleted) || 0);
              const inSessions = Math.max(0, Number(d.sessionsCount) || 0);
              const inUpdated = Number(d.updatedAt) || Date.now();
              const existing = daily[dk];
              daily[dk] = existing ? {
                dateKey: dk,
                appTimeSec: Math.max(existing.appTimeSec || 0, inApp),
                watchTimeSec: Math.max(existing.watchTimeSec || 0, inWatch),
                episodesCompleted: Math.max(existing.episodesCompleted || 0, inEps),
                sessionsCount: Math.max(existing.sessionsCount || 0, inSessions),
                updatedAt: Math.max(existing.updatedAt || 0, inUpdated)
              } : {
                dateKey: dk,
                appTimeSec: inApp,
                watchTimeSec: inWatch,
                episodesCompleted: inEps,
                sessionsCount: inSessions,
                updatedAt: inUpdated
              };
            });
            safeSet(KEYS.DAILY_USAGE, daily);
          }

          const animeStats = replaceAll ? {} : this.getAnimeWatchStats();
          if (Array.isArray(incomingUsage.animeStats)) {
            incomingUsage.animeStats.forEach(a => {
              if (!a || !a.animeId) return;
              const aid = String(a.animeId);
              const inWatch = Math.max(0, Number(a.watchTimeSec) || 0);
              const inEps = Math.max(0, Number(a.episodesWatchedCount) || 0);
              const inLast = Number(a.lastWatchedAt) || Date.now();
              const existing = animeStats[aid];
              animeStats[aid] = existing ? {
                animeId: aid,
                animeTitle: existing.animeTitle || a.animeTitle || '',
                coverImage: existing.coverImage || a.coverImage || '',
                genres: existing.genres || a.genres || '',
                watchTimeSec: Math.max(existing.watchTimeSec || 0, inWatch),
                episodesWatchedCount: Math.max(existing.episodesWatchedCount || 0, inEps),
                lastWatchedAt: Math.max(existing.lastWatchedAt || 0, inLast)
              } : {
                animeId: aid,
                animeTitle: a.animeTitle || '',
                coverImage: a.coverImage || '',
                genres: a.genres || '',
                watchTimeSec: inWatch,
                episodesWatchedCount: inEps,
                lastWatchedAt: inLast
              };
            });
            safeSet(KEYS.ANIME_STATS, animeStats);
          }
        }

        if (parsed.settings && typeof parsed.settings === 'object') {
          this.updateSettings(parsed.settings);
        }

        return {
          success: true,
          animeCount: (parsed.animeState || []).length,
          progressCount: incomingProgress.length
        };
      } catch (err) {
        return { success: false, error: err.message };
      }
    },

    clearAllData() {
      localStorage.removeItem(KEYS.LIBRARY);
      localStorage.removeItem(KEYS.PROGRESS);
      localStorage.removeItem(KEYS.RECENT_SEARCHES);
      localStorage.removeItem(KEYS.DAILY_USAGE);
      localStorage.removeItem(KEYS.ANIME_STATS);
    }
  };

  window.ZenithStorage = Storage;
})(window);
