/**
 * Zenith Anime Web — Local-First Storage & State Management
 * Persistent storage using LocalStorage with continuous progress tracking,
 * full library organization, and backup export/import compatible with Android.
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
    titleLanguage: 'english', // 'english' | 'romaji'
    defaultServer: 'aniembed', // 'aniembed' | 'viduki' | 'megaplay' | 'vidsrc'
    defaultAudio: 'sub', // 'sub' | 'dub'
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
      // Also update standalone accent key for cross-page compatibility with landing.html
      if (partial.accent) {
        try {
          localStorage.setItem('zenith_accent', partial.accent);
        } catch {
          // ignore
        }
      }
      return updated;
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
      if (!anime || !anime.id) return;
      const lib = this.getLibrary();
      const id = String(anime.id);
      const existing = lib[id] || {
        id,
        malId: anime.malId || '',
        title: anime.title || '',
        englishTitle: anime.englishTitle || anime.title || '',
        romajiTitle: anime.romajiTitle || anime.title || '',
        coverImage: anime.coverImage || '',
        bannerImage: anime.bannerImage || '',
        rating: anime.rating || 0,
        year: anime.year || '',
        type: anime.type || 'TV',
        totalEpisodes: anime.totalEpisodes || 0,
        genres: anime.genres || '',
        isFavorite: false,
        addedAt: Date.now()
      };

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
      const existing = lib[id] || {
        id,
        malId: anime.malId || '',
        title: anime.title || '',
        englishTitle: anime.englishTitle || anime.title || '',
        romajiTitle: anime.romajiTitle || anime.title || '',
        coverImage: anime.coverImage || '',
        bannerImage: anime.bannerImage || '',
        rating: anime.rating || 0,
        year: anime.year || '',
        type: anime.type || 'TV',
        totalEpisodes: anime.totalEpisodes || 0,
        genres: anime.genres || '',
        status: null,
        addedAt: Date.now()
      };

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

    getFilteredLibrary(tab = 'ALL', sortBy = 'updated') {
      const lib = this.getLibrary();
      let items = Object.values(lib);

      switch (tab) {
        case 'WATCHING':
          items = items.filter(i => i.status === 'WATCHING');
          break;
        case 'COMPLETED':
          items = items.filter(i => i.status === 'COMPLETED');
          break;
        case 'PLAN_TO_WATCH':
          items = items.filter(i => i.status === 'PLAN_TO_WATCH');
          break;
        case 'ON_HOLD':
          items = items.filter(i => i.status === 'ON_HOLD');
          break;
        case 'DROPPED':
          items = items.filter(i => i.status === 'DROPPED');
          break;
        case 'FAVORITES':
          items = items.filter(i => i.isFavorite);
          break;
        case 'ALL':
        default:
          items = items.filter(i => i.status || i.isFavorite);
          break;
      }

      items.sort((a, b) => {
        if (sortBy === 'title') {
          return (a.title || '').localeCompare(b.title || '');
        } else if (sortBy === 'score') {
          return (b.rating || 0) - (a.rating || 0);
        } else {
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
      return all[key] || null;
    },

    getLatestProgressForAnime(animeId) {
      const all = this.getAllProgress();
      const matches = Object.values(all).filter(p => p.animeId === String(animeId));
      if (matches.length === 0) return null;
      matches.sort((a, b) => (b.lastWatchedAt || 0) - (a.lastWatchedAt || 0));
      return matches[0];
    },

    saveProgress(data) {
      if (!data || !data.animeId || !data.episodeNumber) return;
      const all = this.getAllProgress();
      const key = `${data.animeId}_ep_${data.episodeNumber}`;
      const now = Date.now();
      const existing = all[key] || {};

      const duration = data.durationSec || existing.durationSec || 1440; // ~24 min fallback
      const currentTime = Math.min(duration, Math.max(0, data.currentTimeSec || 0));
      const isCompleted = data.completed !== undefined
        ? data.completed
        : (duration > 0 && (currentTime / duration) >= 0.88);

      all[key] = {
        id: key,
        animeId: String(data.animeId),
        animeTitle: data.animeTitle || existing.animeTitle || 'Anime',
        episodeNumber: Number(data.episodeNumber),
        episodeTitle: data.episodeTitle || `Episode ${data.episodeNumber}`,
        coverImage: data.coverImage || existing.coverImage || '',
        bannerImage: data.bannerImage || existing.bannerImage || '',
        currentTimeSec: Math.floor(currentTime),
        durationSec: Math.floor(duration),
        language: data.language || existing.language || 'sub',
        completed: isCompleted,
        lastWatchedAt: now
      };

      safeSet(KEYS.PROGRESS, all);

      // Auto-update library status to WATCHING if not already set
      const libEntry = this.getAnimeEntry(data.animeId);
      if (!libEntry || !libEntry.status) {
        this.setWatchStatus({
          id: data.animeId,
          title: data.animeTitle,
          coverImage: data.coverImage,
          bannerImage: data.bannerImage
        }, 'WATCHING');
      }

      return all[key];
    },

    toggleEpisodeWatched(anime, episodeNumber) {
      if (!anime || !anime.id) return false;
      const existing = this.getProgress(anime.id, episodeNumber);
      const nextCompleted = !(existing && existing.completed);
      const duration = existing?.durationSec || 1440;
      this.saveProgress({
        animeId: anime.id,
        animeTitle: anime.title,
        episodeNumber: Number(episodeNumber),
        coverImage: anime.coverImage,
        bannerImage: anime.bannerImage,
        currentTimeSec: nextCompleted ? duration : (existing?.currentTimeSec || 0),
        durationSec: duration,
        completed: nextCompleted
      });
      return nextCompleted;
    },

    deleteProgress(animeId, episodeNumber) {
      const all = this.getAllProgress();
      const key = `${animeId}_ep_${episodeNumber}`;
      if (all[key]) {
        delete all[key];
        safeSet(KEYS.PROGRESS, all);
      }
    },

    deleteProgressForAnime(animeId) {
      const all = this.getAllProgress();
      let changed = false;
      Object.keys(all).forEach(k => {
        if (all[k].animeId === String(animeId)) {
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

    // ---- BACKUP EXPORT & IMPORT ----
    exportBackupJson() {
      const lib = this.getLibrary();
      const progress = this.getAllProgress();
      const settings = this.getSettings();

      const backup = {
        version: 1,
        exportedAt: Date.now(),
        app: 'ZenithAnime',
        settings,
        animeState: Object.values(lib).map(a => ({
          id: a.id,
          malId: a.malId || '',
          title: a.title,
          englishTitle: a.englishTitle || a.title,
          romajiTitle: a.romajiTitle || a.title,
          coverImage: a.coverImage,
          bannerImage: a.bannerImage,
          rating: a.rating,
          year: a.year,
          type: a.type,
          totalEpisodes: a.totalEpisodes,
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

    importBackupJson(jsonString) {
      try {
        const parsed = JSON.parse(jsonString);
        if (!parsed || (parsed.app !== 'ZenithAnime' && !parsed.animeState)) {
          throw new Error('Invalid Zenith Anime backup file format');
        }

        const lib = this.getLibrary();
        if (Array.isArray(parsed.animeState)) {
          parsed.animeState.forEach(a => {
            if (a.id) {
              lib[String(a.id)] = {
                id: String(a.id),
                malId: a.malId || '',
                title: a.title || 'Anime',
                englishTitle: a.englishTitle || a.title,
                romajiTitle: a.romajiTitle || a.title,
                coverImage: a.coverImage || '',
                bannerImage: a.bannerImage || a.coverImage || '',
                rating: a.rating || 0,
                year: a.year || '',
                type: a.type || 'TV',
                totalEpisodes: a.totalEpisodes || 0,
                genres: a.genres || '',
                isFavorite: Boolean(a.isFavorite),
                status: a.watchStatus || (a.isCompleted ? 'COMPLETED' : (a.isInWatchlist ? 'WATCHING' : null)),
                addedAt: a.lastUpdated || Date.now()
              };
            }
          });
          safeSet(KEYS.LIBRARY, lib);
        }

        const progress = this.getAllProgress();
        if (Array.isArray(parsed.progressState)) {
          parsed.progressState.forEach(p => {
            if (p.animeId && p.episodeNumber) {
              const key = `${p.animeId}_ep_${p.episodeNumber}`;
              progress[key] = {
                id: key,
                animeId: String(p.animeId),
                animeTitle: p.animeTitle || 'Anime',
                episodeNumber: Number(p.episodeNumber),
                episodeTitle: p.episodeTitle || `Episode ${p.episodeNumber}`,
                coverImage: p.coverImage || '',
                currentTimeSec: Number(p.currentTimeSec) || 0,
                durationSec: Number(p.durationSec) || 1440,
                language: p.language || 'sub',
                completed: Boolean(p.completed),
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
      // Keep settings intact unless explicitly reset
    }
  };

  window.ZenithStorage = Storage;
})(window);
