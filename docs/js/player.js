/**
 * Zenith Anime Web — Clean Native Video Player & Watch Tracking Engine
 * Direct un-hijacked iframe video stream embedding with instant multi-server switching,
 * Sub/Dub audio toggle, aired-episode-bounded navigation, Theater Mode, Fullscreen, Share,
 * and continuous watch history & progress tracking.
 */

(function (window) {
  'use strict';

  function formatTime(sec) {
    const s = Math.max(0, Math.floor(sec || 0));
    const m = Math.floor(s / 60);
    const r = s % 60;
    return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`;
  }

  const Player = {
    currentAnime: null,
    currentEpisode: 1,
    currentServer: 'aniembed',
    currentLanguage: 'sub',
    isTheaterMode: false,
    timer: null,
    playbackSeconds: 0,
    estimatedDuration: 1440, // Standard anime broadcast: ~24 minutes in seconds

    init() {
      // 100% native interaction — zero global keyboard or click intercepting.
    },

    /**
     * Load and mount an anime episode, enforcing aired-episode limits
     */
    load(anime, episodeNumber = 1, options = {}) {
      const normalized = window.ZenithAPI?.normalizeAnime ? window.ZenithAPI.normalizeAnime(anime) : anime;
      this.currentAnime = normalized;

      const availableEps = typeof normalized.availableEpisodes === 'number'
        ? normalized.availableEpisodes
        : (normalized.isUpcoming ? 0 : (normalized.totalEpisodes || 0));

      const requestedEp = Math.max(1, parseInt(episodeNumber, 10) || 1);

      const settings = window.ZenithStorage?.getSettings() || {};
      this.currentServer = options.server || settings.defaultServer || 'aniembed';
      this.currentLanguage = options.language || settings.defaultAudio || 'sub';

      // Guard 1: Anime has not aired any episodes yet
      if (normalized.isUpcoming || availableEps <= 0) {
        this.stopProgressTracking();
        this.currentEpisode = 1;
        this.renderUnreleasedState();
        return;
      }

      // Guard 2: Requested episode exceeds aired available episodes
      if (requestedEp > availableEps) {
        this.currentEpisode = availableEps;
        window.ZenithUI?.showToast(`Episode ${requestedEp} has not aired yet — playing latest Episode ${availableEps}`);
      } else {
        this.currentEpisode = requestedEp;
      }

      // Restore saved progress for this episode if previously watched
      const saved = window.ZenithStorage?.getProgress(normalized.id, this.currentEpisode);
      this.estimatedDuration = saved && saved.durationSec ? saved.durationSec : 1440;
      this.playbackSeconds = saved
        ? Math.min(this.estimatedDuration, Math.max(0, saved.currentTimeSec || 0))
        : 0;

      this.renderPlayer();
      this.startProgressTracking();
    },

    /**
     * Render clean notice when user navigates to #watch for an upcoming/unreleased anime
     */
    renderUnreleasedState() {
      const container = document.getElementById('player-viewport-container');
      if (!container || !this.currentAnime) return;

      const anime = this.currentAnime;
      const displayTitle = window.ZenithStorage?.getDisplayTitle
        ? window.ZenithStorage.getDisplayTitle(anime)
        : anime.title;

      let countdownNote = 'Episodes will become playable as soon as the official broadcast begins.';
      if (anime.nextAiringEpisode && anime.nextAiringEpisode.timeUntilAiring > 0) {
        const sec = anime.nextAiringEpisode.timeUntilAiring;
        const d = Math.floor(sec / 86400);
        const h = Math.floor((sec % 86400) / 3600);
        const m = Math.floor((sec % 3600) / 60);
        countdownNote = `Episode ${anime.nextAiringEpisode.episode} airs in ${d > 0 ? `${d}d ` : ''}${h}h ${m}m.`;
      }

      container.innerHTML = `
        <div class="player-wrapper player-unreleased-box">
          <div class="unreleased-player-inner">
            <div class="unreleased-icon-circle">
              <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
            </div>
            <h2>${displayTitle} — Not Yet Aired</h2>
            <p>${countdownNote}</p>
            <div class="unreleased-actions">
              <a href="#details/${anime.id}" class="btn btn-primary">Back to Anime Details</a>
              <a href="#schedule" class="btn btn-secondary">View Airing Schedule</a>
            </div>
          </div>
        </div>
      `;
    },

    /**
     * Render the clean native video viewport, episode progress tracker, and stream control bar
     */
    renderPlayer() {
      const container = document.getElementById('player-viewport-container');
      if (!container) return;

      const anime = this.currentAnime;
      const ep = this.currentEpisode;
      const server = this.currentServer;
      const lang = this.currentLanguage;

      const embedUrl = window.ZenithAPI.getStreamUrl(server, anime, ep, lang);
      const displayTitle = window.ZenithStorage?.getDisplayTitle
        ? window.ZenithStorage.getDisplayTitle(anime)
        : anime.title;
      const availableEpisodes = typeof anime.availableEpisodes === 'number'
        ? anime.availableEpisodes
        : (anime.totalEpisodes || 1);

      const saved = window.ZenithStorage?.getProgress(anime.id, ep);
      const isWatched = Boolean(saved?.completed || (this.playbackSeconds / this.estimatedDuration) >= 0.88);
      const progressPct = Math.min(100, Math.round((this.playbackSeconds / this.estimatedDuration) * 100));

      container.className = `player-viewport-container${this.isTheaterMode ? ' theater-mode' : ''}`;
      document.body.classList.toggle('theater-active', this.isTheaterMode);

      container.innerHTML = `
        <div class="player-wrapper" id="player-wrapper">
          <!-- 100% Native Clean Iframe Embed (Zero Overlays, Direct Native Player Interaction) -->
          <div class="player-embed-box">
            <iframe 
              id="zenith-video-iframe"
              class="player-iframe"
              src="${embedUrl}"
              allow="autoplay; fullscreen; encrypted-media; picture-in-picture; screen-wake-lock"
              allowfullscreen
              referrerpolicy="no-referrer"
              title="${displayTitle} - Episode ${ep}"
            ></iframe>
          </div>

          <!-- Interactive Episode Watch Progress Strip -->
          <div class="player-progress-strip" id="player-progress-strip">
            <div class="player-progress-track" id="player-progress-track" role="slider" aria-label="Episode Watch Progress Tracker" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${progressPct}" title="Click or drag to adjust saved episode watch progress">
              <div class="player-scrub-tooltip" id="player-scrub-tooltip">${formatTime(this.playbackSeconds)}</div>
              <div class="player-progress-fill" id="player-progress-fill" style="width: ${progressPct}%">
                <span class="player-progress-thumb"></span>
              </div>
            </div>
            <div class="player-progress-meta">
              <span class="player-time-badge" id="player-time-label">
                <span class="pulse-dot"></span>
                <span>Tracked: <strong id="player-elapsed-text">${formatTime(this.playbackSeconds)}</strong> / ${formatTime(this.estimatedDuration)}</span>
              </span>
              <button type="button" class="player-watched-toggle ${isWatched ? 'watched' : ''}" id="player-btn-watched" title="Toggle Episode Watched Status">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span id="player-watched-text">${isWatched ? 'Watched ✓' : 'Mark Watched'}</span>
              </button>
            </div>
          </div>

          <!-- Non-Intrusive Server & Stream Control Bar (Directly Below Video) -->
          <div class="player-stream-bar" id="player-stream-bar">
            <!-- Server Switcher Pills -->
            <div class="stream-bar-group stream-servers-group">
              <span class="stream-bar-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                  <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                  <line x1="6" y1="6" x2="6.01" y2="6"></line>
                  <line x1="6" y1="18" x2="6.01" y2="18"></line>
                </svg>
                <span>Server</span>
              </span>
              <div class="stream-servers-list">
                ${window.ZenithAPI.getAvailableServers().map(s => `
                  <button type="button" class="server-pill-btn ${s.id === server ? 'active' : ''}" data-server="${s.id}" title="${s.note}">
                    <span class="server-dot"></span>
                    <span class="server-name">${s.name.replace(/ \(.*\)/, '')}</span>
                  </button>
                `).join('')}
              </div>
            </div>

            <!-- Audio & Transport Actions -->
            <div class="stream-bar-group stream-controls-group">
              <!-- Audio (Sub / Dub) Toggle -->
              <button type="button" class="stream-pill-btn audio-pill ${lang === 'dub' ? 'dub' : 'sub'}" id="player-btn-audio" title="Switch Audio Track (Sub / Dub)">
                <span class="audio-badge">${lang.toUpperCase()}</span>
                <span>Audio</span>
              </button>

              <!-- Previous & Next Episode Navigation (Bounded by availableEpisodes) -->
              <div class="ep-nav-group">
                <button type="button" class="stream-ctrl-btn" id="ctrl-prev" ${ep <= 1 ? 'disabled' : ''} title="Previous Episode">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
                  </svg>
                  <span>Prev</span>
                </button>

                <button type="button" class="stream-ctrl-btn" id="ctrl-next" ${ep >= availableEpisodes ? 'disabled' : ''} title="${ep >= availableEpisodes ? 'Latest aired episode reached' : 'Next Episode'}">
                  <span>Next</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
                  </svg>
                </button>
              </div>

              <!-- Reload Stream Button -->
              <button type="button" class="stream-ctrl-btn" id="player-btn-reload" title="Reload Video Stream">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10"></polyline>
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                </svg>
                <span class="ctrl-btn-text">Reload</span>
              </button>

              <!-- Theater Mode Toggle (Desktop/Laptop) -->
              <button type="button" class="stream-ctrl-btn btn-theater-toggle ${this.isTheaterMode ? 'active' : ''}" id="player-btn-theater" title="Toggle Widescreen Theater Mode">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                  <line x1="6" y1="4" x2="6" y2="20"></line>
                  <line x1="18" y1="4" x2="18" y2="20"></line>
                </svg>
                <span class="ctrl-btn-text">${this.isTheaterMode ? 'Default View' : 'Theater'}</span>
              </button>

              <!-- Fullscreen Toggle -->
              <button type="button" class="stream-ctrl-btn" id="player-btn-fullscreen" title="Toggle Fullscreen">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
                </svg>
                <span class="ctrl-btn-text">Fullscreen</span>
              </button>

              <!-- Share Episode Button -->
              <button type="button" class="stream-ctrl-btn" id="player-btn-share" title="Share Episode Link">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="18" cy="5" r="3"></circle>
                  <circle cx="6" cy="12" r="3"></circle>
                  <circle cx="18" cy="19" r="3"></circle>
                  <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                  <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
                </svg>
                <span class="ctrl-btn-text">Share</span>
              </button>
            </div>
          </div>
        </div>
      `;

      this.bindPlayerEvents();
    },

    getServerDisplayName(serverId) {
      const match = window.ZenithAPI.getAvailableServers().find(s => s.id === serverId);
      return match ? match.name.split(' ')[0] : 'Server';
    },

    /**
     * Wire up server switching, audio toggle, progress scrubbing, theater mode, fullscreen, share, and episode controls
     */
    bindPlayerEvents() {
      // 1. Server switcher pills
      document.querySelectorAll('.server-pill-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const s = btn.getAttribute('data-server');
          if (s && s !== this.currentServer) {
            this.currentServer = s;
            window.ZenithStorage?.updateSettings({ defaultServer: s });
            const iframe = document.getElementById('zenith-video-iframe');
            if (iframe) {
              iframe.src = window.ZenithAPI.getStreamUrl(s, this.currentAnime, this.currentEpisode, this.currentLanguage);
            }
            document.querySelectorAll('.server-pill-btn').forEach(b => {
              b.classList.toggle('active', b.getAttribute('data-server') === s);
            });
            const displayName = this.getServerDisplayName(s);
            window.ZenithUI?.showToast(`Switched server to ${displayName}`);
          }
        });
      });

      // 2. Sub / Dub audio toggle
      document.getElementById('player-btn-audio')?.addEventListener('click', () => {
        this.currentLanguage = this.currentLanguage === 'sub' ? 'dub' : 'sub';
        window.ZenithStorage?.updateSettings({ defaultAudio: this.currentLanguage });
        const iframe = document.getElementById('zenith-video-iframe');
        if (iframe) {
          iframe.src = window.ZenithAPI.getStreamUrl(this.currentServer, this.currentAnime, this.currentEpisode, this.currentLanguage);
        }
        const btn = document.getElementById('player-btn-audio');
        if (btn) {
          btn.className = `stream-pill-btn audio-pill ${this.currentLanguage === 'dub' ? 'dub' : 'sub'}`;
          const badge = btn.querySelector('.audio-badge');
          if (badge) badge.textContent = this.currentLanguage.toUpperCase();
        }
        window.ZenithUI?.showToast(`Switched audio to ${this.currentLanguage.toUpperCase()}`);
      });

      // 3. Reload stream button
      document.getElementById('player-btn-reload')?.addEventListener('click', () => {
        const iframe = document.getElementById('zenith-video-iframe');
        if (iframe) {
          const currentSrc = iframe.src;
          iframe.src = '';
          setTimeout(() => {
            iframe.src = currentSrc;
          }, 80);
          window.ZenithUI?.showToast('Reloading video stream...');
        }
      });

      // 4. Previous / Next episode navigation via hash change (strictly bounded by availableEpisodes)
      document.getElementById('ctrl-prev')?.addEventListener('click', () => {
        if (this.currentEpisode > 1) {
          window.location.hash = `#watch/${this.currentAnime.id}/${this.currentEpisode - 1}`;
        }
      });

      document.getElementById('ctrl-next')?.addEventListener('click', () => {
        const available = typeof this.currentAnime?.availableEpisodes === 'number'
          ? this.currentAnime.availableEpisodes
          : (this.currentAnime?.totalEpisodes || 1);
        if (this.currentEpisode < available) {
          window.location.hash = `#watch/${this.currentAnime.id}/${this.currentEpisode + 1}`;
        }
      });

      // 5. Theater Mode toggle
      document.getElementById('player-btn-theater')?.addEventListener('click', (e) => {
        this.isTheaterMode = !this.isTheaterMode;
        const container = document.getElementById('player-viewport-container');
        container?.classList.toggle('theater-mode', this.isTheaterMode);
        document.body.classList.toggle('theater-active', this.isTheaterMode);
        const btn = e.currentTarget;
        btn.classList.toggle('active', this.isTheaterMode);
        const label = btn.querySelector('.ctrl-btn-text');
        if (label) label.textContent = this.isTheaterMode ? 'Default View' : 'Theater';
        window.ZenithUI?.showToast(this.isTheaterMode ? 'Theater Mode enabled' : 'Standard view restored');
      });

      // 6. Fullscreen toggle
      document.getElementById('player-btn-fullscreen')?.addEventListener('click', () => {
        const wrapper = document.getElementById('player-wrapper');
        if (!wrapper) return;
        if (!document.fullscreenElement) {
          (wrapper.requestFullscreen || wrapper.webkitRequestFullscreen)?.call(wrapper).catch(() => {});
        } else {
          (document.exitFullscreen || document.webkitExitFullscreen)?.call(document).catch(() => {});
        }
      });

      // 7. Share Episode button
      document.getElementById('player-btn-share')?.addEventListener('click', () => {
        if (this.currentAnime) {
          window.ZenithUI?.shareAnime(this.currentAnime, this.currentEpisode);
        }
      });

      // 8. Mark Watched toggle button
      document.getElementById('player-btn-watched')?.addEventListener('click', (e) => {
        if (!this.currentAnime) return;
        const btn = e.currentTarget;
        const nowWatched = window.ZenithStorage?.toggleEpisodeWatched(this.currentAnime, this.currentEpisode);
        if (nowWatched) {
          this.playbackSeconds = this.estimatedDuration;
        } else if (this.playbackSeconds >= this.estimatedDuration * 0.88) {
          this.playbackSeconds = 0;
        }
        btn.classList.toggle('watched', Boolean(nowWatched));
        const txt = document.getElementById('player-watched-text');
        if (txt) txt.textContent = nowWatched ? 'Watched ✓' : 'Mark Watched';
        this.updateProgressUI();

        // Update active episode pill in horizontal guide if present
        const activePill = document.querySelector(`.watch-ep-pill[href="#watch/${this.currentAnime.id}/${this.currentEpisode}"]`);
        if (activePill) {
          activePill.classList.toggle('watched', Boolean(nowWatched));
        }

        window.ZenithUI?.showToast(nowWatched ? `Episode ${this.currentEpisode} marked as watched!` : `Episode ${this.currentEpisode} marked as unwatched`);
      });

      // 9. Interactive Episode Progress Scrubber with floating time bubble
      const track = document.getElementById('player-progress-track');
      const tooltip = document.getElementById('player-scrub-tooltip');
      if (track) {
        let isDragging = false;
        const scrubToClientX = (clientX, updatePlayback = true) => {
          const rect = track.getBoundingClientRect();
          if (!rect.width) return;
          const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
          const targetSec = Math.round(ratio * this.estimatedDuration);
          if (tooltip) {
            tooltip.textContent = formatTime(targetSec);
            tooltip.style.left = `${ratio * 100}%`;
          }
          if (updatePlayback) {
            this.playbackSeconds = targetSec;
            this.updateProgressUI();
            this.saveCurrentProgress();
          }
        };

        track.addEventListener('pointerenter', () => {
          track.classList.add('scrub-hover');
        });
        track.addEventListener('pointerleave', () => {
          if (!isDragging) track.classList.remove('scrub-hover');
        });
        track.addEventListener('pointerdown', (e) => {
          isDragging = true;
          track.classList.add('scrub-hover');
          track.setPointerCapture?.(e.pointerId);
          scrubToClientX(e.clientX, true);
        });
        track.addEventListener('pointermove', (e) => {
          scrubToClientX(e.clientX, isDragging);
        });
        track.addEventListener('pointerup', (e) => {
          if (!isDragging) return;
          isDragging = false;
          track.classList.remove('scrub-hover');
          track.releasePointerCapture?.(e.pointerId);
        });
      }
    },

    updateProgressUI() {
      const pct = Math.min(100, Math.round((this.playbackSeconds / this.estimatedDuration) * 100));
      const fill = document.getElementById('player-progress-fill');
      const elapsed = document.getElementById('player-elapsed-text');
      const track = document.getElementById('player-progress-track');
      if (fill) fill.style.width = `${pct}%`;
      if (elapsed) elapsed.textContent = formatTime(this.playbackSeconds);
      if (track) track.setAttribute('aria-valuenow', String(pct));

      const isWatched = (this.playbackSeconds / this.estimatedDuration) >= 0.88;
      const watchedBtn = document.getElementById('player-btn-watched');
      const watchedTxt = document.getElementById('player-watched-text');
      if (watchedBtn && isWatched && !watchedBtn.classList.contains('watched')) {
        watchedBtn.classList.add('watched');
        if (watchedTxt) watchedTxt.textContent = 'Watched ✓';
      }
    },

    // =========================================================================
    // CONTINUOUS WATCH TIME, HISTORY & PROGRESS TRACKING
    // =========================================================================

    startProgressTracking() {
      this.stopProgressTracking();
      this.saveCurrentProgress();

      this.timer = setInterval(() => {
        if (document.hidden) return;
        if (this.playbackSeconds < this.estimatedDuration) {
          this.playbackSeconds = Math.min(this.estimatedDuration, this.playbackSeconds + 1);
          this.updateProgressUI();
        }

        if (this.playbackSeconds % 5 === 0) {
          this.saveCurrentProgress();
        }
      }, 1000);
    },

    stopProgressTracking() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
      this.saveCurrentProgress();
    },

    saveCurrentProgress() {
      if (!this.currentAnime || !this.currentEpisode) return;
      if (this.currentAnime.isUpcoming || (typeof this.currentAnime.availableEpisodes === 'number' && this.currentAnime.availableEpisodes <= 0)) {
        return;
      }

      const duration = this.estimatedDuration || 1440;
      const clampedSec = Math.min(duration, Math.max(0, this.playbackSeconds));
      const existing = window.ZenithStorage?.getProgress(this.currentAnime.id, this.currentEpisode);
      const isCompleted = Boolean(existing?.completed || (duration > 0 && (clampedSec / duration) >= 0.88));
      const epMeta = this.currentAnime.episodeMetadata && this.currentAnime.episodeMetadata[this.currentEpisode];

      window.ZenithStorage?.saveProgress({
        animeId: this.currentAnime.id,
        animeTitle: window.ZenithStorage?.getDisplayTitle ? window.ZenithStorage.getDisplayTitle(this.currentAnime) : this.currentAnime.title,
        episodeNumber: this.currentEpisode,
        episodeTitle: epMeta?.title || existing?.episodeTitle || `Episode ${this.currentEpisode}`,
        coverImage: this.currentAnime.coverImage,
        bannerImage: this.currentAnime.bannerImage,
        currentTimeSec: clampedSec,
        durationSec: duration,
        completed: isCompleted,
        availableEpisodes: this.currentAnime.availableEpisodes,
        language: this.currentLanguage
      });
    }
  };

  window.ZenithPlayer = Player;
})(window);
