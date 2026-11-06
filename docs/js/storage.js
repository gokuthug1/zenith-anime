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
    NOTIFICATIONS: 'zenith_notifications'
  };

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
          availableEpisodes: availableEps
        }, 'WATCHING');
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

    // ---- BACKUP EXPORT & IMPORT (100% Android APK Compatible) ----
    exportBackupJson() {
      const lib = this.getLibrary();
      const progress = this.getAllProgress();
      const settings = this.getSettings();

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
        }))
      };

      return JSON.stringify(backup, null, 2);
    },

    importBackupJson(jsonString, options = {}) {
      const replaceAll = Boolean(options.replaceAll);
      try {
        const parsed = JSON.parse(jsonString);
        if (!parsed || (parsed.app !== 'ZenithAnime' && !Array.isArray(parsed.animeState) && !Array.isArray(parsed.progressState))) {
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
        if (Array.isArray(parsed.progressState)) {
          parsed.progressState.forEach(p => {
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

        if (parsed.settings && typeof parsed.settings === 'object') {
          this.updateSettings(parsed.settings);
        }

        return {
          success: true,
          animeCount: (parsed.animeState || []).length,
          progressCount: (parsed.progressState || []).length
        };
      } catch (err) {
        return { success: false, error: err.message };
      }
    },

    clearAllData() {
      localStorage.removeItem(KEYS.LIBRARY);
      localStorage.removeItem(KEYS.PROGRESS);
      localStorage.removeItem(KEYS.RECENT_SEARCHES);
    }
  };

  window.ZenithStorage = Storage;
})(window);
