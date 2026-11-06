/**
 * Zenith Anime Web — Main Application Controller & Router
 * SPA router, deep-link & Android intent handoff manager, responsive bottom dock,
 * theme manager, and global navigation listeners.
 */

(function (window) {
  'use strict';

  const ANDROID_PACKAGE = 'com.aistudio.zenithanime.vxkt';
  const APK_DOWNLOAD_URL = 'https://github.com/gokuthug1/zenith-anime/raw/master/APK/zenith-anime-latest.apk';

  const App = {
    currentRoute: 'home',
    arrivedViaDeepLink: false,
    initialSearchHandled: false,

    init() {
      // 1. Apply user's saved accent theme
      const settings = window.ZenithStorage.getSettings();
      document.documentElement.setAttribute('data-accent', settings.accent || 'emerald');

      // 2. Initialize Player & UI
      window.ZenithPlayer.init();
      window.ZenithUI?.init?.();

      // 3. Bind Global Navigation Events
      this.bindGlobalNavigation();

      // 4. Listen for URL Hash Changes and window unload
      window.addEventListener('hashchange', () => this.handleRoute());
      window.addEventListener('beforeunload', () => {
        window.ZenithPlayer?.stopProgressTracking();
      });

      // 5. Check URL search params (?anime=ID&ep=EP) and initial route dispatch
      this.consumeInitialSearchParams();
      this.handleRoute();
    },

    isAndroid() {
      return /Android/i.test(navigator.userAgent || '');
    },

    /**
     * Build canonical universal share URL (works on Web + triggers Android App Links / Deep Link Banner)
     */
    buildShareUrl(animeId, ep) {
      const base = `${window.location.origin}${window.location.pathname}`;
      if (ep) {
        return `${base}?anime=${encodeURIComponent(animeId)}&ep=${encodeURIComponent(ep)}#watch/${animeId}/${ep}`;
      }
      return `${base}?anime=${encodeURIComponent(animeId)}#details/${animeId}`;
    },

    /**
     * Build Android Intent URL for com.aistudio.zenithanime.vxkt with seamless browser fallback
     */
    buildAndroidIntentUrl(animeId, ep) {
      const epQuery = ep ? `?ep=${encodeURIComponent(ep)}` : '';
      const fallbackUrl = encodeURIComponent(this.buildShareUrl(animeId, ep));
      return `intent://anime/${animeId}${epQuery}#Intent;scheme=zenithanime;package=${ANDROID_PACKAGE};S.browser_fallback_url=${fallbackUrl};end`;
    },

    /**
     * Build direct custom scheme URL (zenithanime://anime/ID?ep=EP)
     */
    buildCustomSchemeUrl(animeId, ep) {
      const epQuery = ep ? `?ep=${encodeURIComponent(ep)}` : '';
      return `zenithanime://anime/${animeId}${epQuery}`;
    },

    /**
     * Launch the Zenith Anime Android App directly to the specified anime / episode
     */
    openInAndroidApp(animeId, ep) {
      if (!animeId) return;
      if (this.isAndroid()) {
        window.location.href = this.buildAndroidIntentUrl(animeId, ep);
        return;
      }

      // On desktop or non-Android devices, attempt custom scheme and offer APK / QR info modal
      const customScheme = this.buildCustomSchemeUrl(animeId, ep);
      const intentUrl = this.buildAndroidIntentUrl(animeId, ep);
      const shareUrl = this.buildShareUrl(animeId, ep);

      window.ZenithUI?.openModal('Open in Zenith Anime Android App', `
        <div class="open-app-modal-content">
          <p class="open-app-modal-desc">
            Have the <strong>Zenith Anime Android App</strong> installed? Open this link on your Android phone or launch directly via deep link:
          </p>
          <div class="open-app-modal-actions">
            <a href="${intentUrl}" class="btn btn-primary">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                <line x1="12" y1="18" x2="12.01" y2="18"></line>
              </svg>
              <span>Launch Zenith App (${ep ? `Ep ${ep}` : 'Details'})</span>
            </a>
            <a href="${APK_DOWNLOAD_URL}" class="btn btn-secondary" download>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              <span>Download Android APK</span>
            </a>
          </div>
          <div class="open-app-link-box">
            <input type="text" class="open-app-link-input" value="${shareUrl}" readonly onclick="this.select()">
            <button type="button" class="btn btn-secondary btn-sm" id="modal-btn-copy-deeplink">Copy Link</button>
          </div>
          <small class="open-app-scheme-hint">App URI: <code>${customScheme}</code></small>
        </div>
      `);

      document.getElementById('modal-btn-copy-deeplink')?.addEventListener('click', () => {
        navigator.clipboard?.writeText(shareUrl);
        window.ZenithUI?.showToast('Copied universal share link!');
      });
    },

    /**
     * Parse ?anime=ID&ep=EP from window.location.search on initial load
     */
    consumeInitialSearchParams() {
      if (this.initialSearchHandled) return;
      this.initialSearchHandled = true;

      const search = window.location.search;
      if (!search) return;

      const params = new URLSearchParams(search);
      const animeId = params.get('anime') || params.get('id');
      const ep = params.get('ep') || params.get('episode');

      if (animeId) {
        this.arrivedViaDeepLink = true;
        const currentHash = (window.location.hash || '').replace(/^#\/?/, '');
        // Route to target anime/episode if hash isn't already pointing to a specific deep route
        if (!currentHash || currentHash === 'home' || (!currentHash.startsWith('details/') && !currentHash.startsWith('watch/') && !currentHash.startsWith('anime/'))) {
          if (ep && parseInt(ep, 10) > 0) {
            window.location.hash = `#watch/${animeId}/${parseInt(ep, 10)}`;
          } else {
            window.location.hash = `#details/${animeId}`;
          }
        }
      }
    },

    /**
     * Render or update the Smart Android App Banner when viewing an anime or arriving via shared link
     */
    updateSmartAppBanner(animeId, ep) {
      const existing = document.getElementById('zenith-smart-app-banner');
      if (!animeId) {
        existing?.remove();
        return;
      }

      const dismissed = sessionStorage.getItem('zenith_app_banner_dismissed') === '1';
      if (dismissed) {
        existing?.remove();
        return;
      }

      // Show automatically on Android or when user arrived via a shared ?anime= link
      if (!this.isAndroid() && !this.arrivedViaDeepLink) {
        existing?.remove();
        return;
      }

      let banner = existing;
      if (!banner) {
        banner = document.createElement('div');
        banner.id = 'zenith-smart-app-banner';
        banner.className = 'smart-app-banner';
        document.body.prepend(banner);
      }

      banner.innerHTML = `
        <div class="container smart-app-banner-inner">
          <div class="smart-app-banner-info">
            <img src="assets/ic_zenith_logo.svg" alt="Zenith Anime" class="smart-app-banner-icon" width="28" height="28">
            <div class="smart-app-banner-text">
              <strong>Zenith Anime Android App</strong>
              <span>Open ${ep ? `Episode ${ep}` : 'this anime'} in the native app for ExoPlayer & offline tracking</span>
            </div>
          </div>
          <div class="smart-app-banner-actions">
            <button type="button" class="btn btn-primary btn-sm" id="smart-banner-open-btn">Open App</button>
            <button type="button" class="smart-banner-dismiss" id="smart-banner-dismiss-btn" aria-label="Dismiss banner">&times;</button>
          </div>
        </div>
      `;

      document.getElementById('smart-banner-open-btn')?.addEventListener('click', () => {
        this.openInAndroidApp(animeId, ep);
      });

      document.getElementById('smart-banner-dismiss-btn')?.addEventListener('click', () => {
        sessionStorage.setItem('zenith_app_banner_dismissed', '1');
        banner.remove();
      });
    },

    bindGlobalNavigation() {
      // Header quick search
      const headerSearchInput = document.getElementById('header-search-input');
      const headerSearchForm = document.getElementById('header-search-form');

      headerSearchForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        const q = headerSearchInput?.value.trim();
        if (q) {
          window.location.hash = `#search?q=${encodeURIComponent(q)}`;
          if (headerSearchInput) {
            headerSearchInput.value = '';
            headerSearchInput.blur();
          }
        }
      });

      // Header random anime roulette button
      document.getElementById('header-btn-random')?.addEventListener('click', async () => {
        window.ZenithUI?.showToast('Rolling random anime roulette...');
        const random = await window.ZenithAPI.getRandomAnime();
        if (random) {
          window.location.hash = `#details/${random.id}`;
        }
      });

      // Mobile Menu Drawer Toggle
      const mobileToggle = document.getElementById('mobile-menu-toggle');
      const mobileNav = document.getElementById('nav-links');
      mobileToggle?.addEventListener('click', () => {
        mobileNav?.classList.toggle('open');
      });

      mobileNav?.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', () => {
          mobileNav.classList.remove('open');
        });
      });

      // Bottom Floating Dock Nav Items
      document.querySelectorAll('.dock-nav-item').forEach(item => {
        item.addEventListener('click', () => {
          document.querySelectorAll('.dock-nav-item').forEach(i => i.classList.remove('active'));
          item.classList.add('active');
        });
      });
    },

    handleRoute() {
      const hash = window.location.hash || '#home';
      const cleanHash = hash.replace(/^#\/?/, '');
      const mainContainer = document.getElementById('app-main-content');
      if (!mainContainer) return;

      // Smooth view enter animation
      mainContainer.classList.remove('route-view-enter');
      void mainContainer.offsetWidth; // trigger reflow for smooth re-trigger
      mainContainer.classList.add('route-view-enter');

      // Scroll to top
      window.scrollTo({ top: 0, behavior: 'instant' });

      // Parse route & parameters
      const [routePart, queryPart] = cleanHash.split('?');
      const segments = routePart.split('/').filter(Boolean);
      let rootRoute = segments[0] || 'home';
      if (rootRoute === 'anime') rootRoute = 'details';

      // Clear hero carousel interval when leaving home
      if (rootRoute !== 'home' && window.ZenithUI?.heroInterval) {
        clearInterval(window.ZenithUI.heroInterval);
        window.ZenithUI.heroInterval = null;
      }
      if (rootRoute !== 'details' && window.ZenithUI?.airingCountdownInterval) {
        clearInterval(window.ZenithUI.airingCountdownInterval);
        window.ZenithUI.airingCountdownInterval = null;
      }

      // Persist & stop tracking if transitioning away from watch
      if (this.currentRoute === 'watch' && rootRoute !== 'watch') {
        window.ZenithPlayer?.stopProgressTracking();
        document.body.classList.remove('theater-active');
      }
      this.currentRoute = rootRoute;

      // Parse query params from hash
      const queryParams = {};
      if (queryPart) {
        queryPart.split('&').forEach(param => {
          const [k, v] = param.split('=');
          if (k) queryParams[decodeURIComponent(k)] = decodeURIComponent(v || '');
        });
      }

      // Update Navigation Active States (Header & Bottom Dock)
      this.updateActiveNavIndicators(rootRoute);

      // Route Dispatcher
      switch (rootRoute) {
        case 'home':
          this.updateSmartAppBanner(null);
          window.ZenithUI.renderHomeScreen(mainContainer);
          break;

        case 'search':
          this.updateSmartAppBanner(null);
          window.ZenithUI.renderSearchScreen(mainContainer, queryParams);
          break;

        case 'details': {
          const animeId = segments[1] || queryParams.id || queryParams.anime;
          const epParam = queryParams.ep ? parseInt(queryParams.ep, 10) : null;
          if (animeId && epParam) {
            window.location.hash = `#watch/${animeId}/${epParam}`;
            return;
          }
          if (animeId) {
            this.updateSmartAppBanner(animeId, null);
            window.ZenithUI.renderDetailsScreen(mainContainer, animeId);
          } else {
            window.location.hash = '#home';
          }
          break;
        }

        case 'watch': {
          const watchAnimeId = segments[1] || queryParams.anime || queryParams.id;
          const watchEp = parseInt(segments[2] || queryParams.ep, 10) || 1;
          if (watchAnimeId) {
            this.updateSmartAppBanner(watchAnimeId, watchEp);
            window.ZenithUI.renderWatchScreen(mainContainer, watchAnimeId, watchEp);
          } else {
            window.location.hash = '#home';
          }
          break;
        }

        case 'schedule':
          this.updateSmartAppBanner(null);
          window.ZenithUI.renderScheduleScreen(mainContainer);
          break;

        case 'mylist':
          this.updateSmartAppBanner(null);
          window.ZenithUI.renderMyListScreen(mainContainer);
          break;

        case 'settings':
          this.updateSmartAppBanner(null);
          window.ZenithUI.renderSettingsScreen(mainContainer);
          break;

        default:
          this.updateSmartAppBanner(null);
          window.ZenithUI.renderHomeScreen(mainContainer);
          break;
      }
    },

    updateActiveNavIndicators(route) {
      document.body.setAttribute('data-route', route);

      // Top header nav links
      document.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href') || '';
        const isMatch = href === `#${route}`;
        link.classList.toggle('active', isMatch);
      });

      // Bottom floating island dock
      document.querySelectorAll('.dock-nav-item').forEach(item => {
        const target = item.getAttribute('data-route');
        item.classList.toggle('active', target === route);
      });
    }
  };

  window.addEventListener('DOMContentLoaded', () => {
    App.init();
  });

  window.ZenithApp = App;
})(window);
