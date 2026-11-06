/**
 * Zenith Anime Web — UI & Screen Rendering Engine
 * Translates the Android Jetpack Compose UI/UX into clean, responsive web views:
 * Home (Spotlight Carousel, Continue Watching, Trending, Top Rated), Search,
 * Anime Details (Segmented tabs, Rich AniList/MAL reviews, AnimeThemes),
 * Watch (Auto-scrolling Episode Guide, Share, Open in App), Schedule, My List, Settings, and Modals.
 */

(function (window) {
  'use strict';

  const UI = {
    heroInterval: null,
    heroIndex: 0,
    heroAnimeList: [],
    escapeBound: false,

    init() {
      if (!this.escapeBound) {
        this.escapeBound = true;
        document.addEventListener('keydown', (e) => {
          if (e.key === 'Escape') {
            this.closeModal();
          }
        });
      }
    },

    // ---- TOAST SYSTEM ----
    showToast(message, duration = 2800) {
      let container = document.getElementById('toast-container');
      if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
      }

      const toast = document.createElement('div');
      toast.className = 'toast-item';
      toast.innerHTML = `
        <span class="toast-dot"></span>
        <span class="toast-text">${message}</span>
      `;
      container.appendChild(toast);

      setTimeout(() => toast.classList.add('show'), 10);
      setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 250);
      }, duration);
    },

    // ---- MODAL SYSTEM ----
    openModal(title, bodyHtml) {
      this.init();
      let overlay = document.getElementById('zenith-global-modal');
      if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'zenith-global-modal';
        overlay.className = 'modal-backdrop';
        overlay.innerHTML = `
          <div class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="modal-title">
            <div class="modal-header">
              <h3 id="modal-title"></h3>
              <button type="button" class="modal-close" id="modal-close-btn" aria-label="Close modal">&times;</button>
            </div>
            <div class="modal-body" id="modal-body"></div>
          </div>
        `;
        document.body.appendChild(overlay);

        overlay.querySelector('#modal-close-btn').addEventListener('click', () => this.closeModal());
        overlay.addEventListener('click', (e) => {
          if (e.target === overlay) this.closeModal();
        });
      }

      document.getElementById('modal-title').textContent = title;
      document.getElementById('modal-body').innerHTML = bodyHtml;
      overlay.classList.add('open');
    },

    closeModal() {
      const overlay = document.getElementById('zenith-global-modal');
      if (!overlay) return;

      // Pause and unload any active media (prevents audio leak when closing AnimeThemes modal)
      overlay.querySelectorAll('video, audio').forEach(media => {
        try {
          media.pause();
          media.removeAttribute('src');
          media.load();
        } catch {
          // ignore
        }
      });

      const body = document.getElementById('modal-body');
      if (body) body.innerHTML = '';
      overlay.classList.remove('open');
    },

    // ---- UNIVERSAL SHARE HELPER ----
    shareAnime(anime, episodeNumber = null) {
      if (!anime) return;
      const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
      const displayTitle = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
      const shareUrl = window.ZenithApp?.buildShareUrl
        ? window.ZenithApp.buildShareUrl(anime.id, episodeNumber)
        : `${window.location.origin}${window.location.pathname}?anime=${anime.id}${episodeNumber ? `&ep=${episodeNumber}` : ''}#${episodeNumber ? `watch/${anime.id}/${episodeNumber}` : `details/${anime.id}`}`;

      const shareTitle = episodeNumber
        ? `${displayTitle} — Episode ${episodeNumber} | Zenith Anime`
        : `${displayTitle} | Zenith Anime`;
      const shareText = episodeNumber
        ? `Watch ${displayTitle} Episode ${episodeNumber} on Zenith Anime (Web & Android App)`
        : `Stream ${displayTitle} on Zenith Anime (Web & Android App)`;

      if (navigator.share) {
        navigator.share({
          title: shareTitle,
          text: shareText,
          url: shareUrl
        }).catch(() => {});
      } else if (navigator.clipboard) {
        navigator.clipboard.writeText(shareUrl).then(() => {
          this.showToast('Copied universal share link to clipboard!');
        }).catch(() => {
          this.showToast('Share link ready: ' + shareUrl);
        });
      } else {
        this.showToast('Share link: ' + shareUrl);
      }
    },

    // ---- RICH REVIEW BODY PARSER (ANILIST MARKDOWN/HTML + MAL BBCODE) ----
    formatReviewBody(rawText) {
      if (!rawText) return '<p class="review-p">No review text provided.</p>';

      let text = String(rawText).replace(/\r\n?/g, '\n').trim();

      // 1. Decode HTML entities so encoded AniList/MAL tags & punctuation normalize cleanly
      const entityMap = {
        '&quot;': '"',
        '&#039;': "'",
        '&#39;': "'",
        '&apos;': "'",
        '&lt;': '<',
        '&gt;': '>',
        '&nbsp;': ' ',
        '&mdash;': '—',
        '&ndash;': '–',
        '&hellip;': '…',
        '&amp;': '&'
      };
      text = text
        .replace(/&(quot|#0?39|apos|lt|gt|nbsp|mdash|ndash|hellip|amp);/gi, (m) => entityMap[m.toLowerCase()] || m)
        .replace(/&#(\d+);/g, (_, dec) => String.fromCharCode(parseInt(dec, 10)))
        .replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)));

      // 2. Strip any dangerous script/style/iframe/object tags
      text = text
        .replace(/<(script|style|iframe|object|embed|form)[\s\S]*?<\/\1>/gi, '')
        .replace(/<(script|style|iframe|object|embed|form)[^>]*\/?>/gi, '');

      // Token store for safe HTML replacements (images, links, spoilers)
      const tokens = [];
      const addToken = (html) => {
        const key = `@@ZENITH_REV_TOK_${tokens.length}@@`;
        tokens.push(html);
        return key;
      };

      const buildImageFigure = (url, widthSpec) => {
        const cleanUrl = String(url || '').trim().replace(/["'<>]/g, '');
        if (!/^https?:\/\//i.test(cleanUrl)) return '';
        let styleAttr = 'max-width: 100%;';
        if (widthSpec) {
          const spec = String(widthSpec).trim();
          if (spec.endsWith('%')) {
            const pct = Math.min(100, Math.max(15, parseInt(spec, 10) || 100));
            styleAttr = `width: ${pct}%; max-width: 100%;`;
          } else {
            const px = Math.min(760, Math.max(80, parseInt(spec, 10) || 0));
            if (px > 0) styleAttr = `width: ${px}px; max-width: 100%;`;
          }
        }
        return addToken(
          `<figure class="review-inline-figure"><img class="review-inline-img" src="${cleanUrl}" style="${styleAttr}" loading="lazy" alt="Review illustration" referrerpolicy="no-referrer" onerror="this.closest('figure').style.display='none'" /></figure>`
        );
      };

      // 3. Extract inline images:
      // 3a. AniList img(url), img220(url), img400(url), img50%(url)
      text = text.replace(/\bimg(\d+%?)?\(\s*(https?:\/\/[^\s)]+)\s*\)/gi, (_, w, url) => buildImageFigure(url, w));

      // 3b. BBCode [img]url[/img] or [img=WxH]url[/img] or [img width=...]url[/img]
      text = text.replace(/\[img(?:=([0-9]+)(?:x[0-9]+)?|\s+width=["']?([0-9]+)["']?)?\s*\](https?:\/\/[^\s[\]]+)\[\/img\]/gi, (_, w1, w2, url) => {
        return buildImageFigure(url, w1 || w2);
      });

      // 3c. Markdown ![alt](url)
      text = text.replace(/!\[([^\]]*)\]\(\s*(https?:\/\/[^\s)]+)\s*\)/gi, (_, _alt, url) => buildImageFigure(url, null));

      // 3d. HTML <img src="url" width="...">
      text = text.replace(/<img\b[^>]*?\bsrc=["'](https?:\/\/[^"']+)["'][^>]*>/gi, (fullMatch, url) => {
        const wMatch = fullMatch.match(/\bwidth=["']?(\d+%?)["']?/i);
        return buildImageFigure(url, wMatch ? wMatch[1] : null);
      });

      // 4. Normalize safe HTML tags in AniList reviews into BBCode/Markdown equivalents before escaping
      text = text
        .replace(/<center>([\s\S]*?)<\/center>/gi, '[center]$1[/center]')
        .replace(/<(?:b|strong)>([\s\S]*?)<\/(?:b|strong)>/gi, '[b]$1[/b]')
        .replace(/<(?:i|em)>([\s\S]*?)<\/(?:i|em)>/gi, '[i]$1[/i]')
        .replace(/<u>([\s\S]*?)<\/u>/gi, '[u]$1[/u]')
        .replace(/<(?:s|strike|del)>([\s\S]*?)<\/(?:s|strike|del)>/gi, '[s]$1[/s]')
        .replace(/<blockquote>([\s\S]*?)<\/blockquote>/gi, '[quote]$1[/quote]')
        .replace(/<h1[^>]*>([\s\S]*?)<\/h1>/gi, '\n# $1\n')
        .replace(/<h2[^>]*>([\s\S]*?)<\/h2>/gi, '\n## $1\n')
        .replace(/<h3[^>]*>([\s\S]*?)<\/h3>/gi, '\n### $1\n')
        .replace(/<h4[^>]*>([\s\S]*?)<\/h4>/gi, '\n#### $1\n')
        .replace(/<li[^>]*>([\s\S]*?)<\/li>/gi, '\n- $1')
        .replace(/<\/?(?:ul|ol)[^>]*>/gi, '\n')
        .replace(/<hr\s*\/?>/gi, '\n---\n')
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<\/p>\s*<p[^>]*>/gi, '\n\n')
        .replace(/<\/?p[^>]*>/gi, '\n');

      // 5. Extract Links (HTML <a>, Markdown [text](url), BBCode [url=...]...[/url])
      const buildSafeLink = (url, label) => {
        const cleanUrl = String(url || '').trim().replace(/["'<>]/g, '');
        if (!/^https?:\/\//i.test(cleanUrl)) return label || cleanUrl;
        const safeLabel = String(label || cleanUrl)
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;');
        return addToken(`<a href="${cleanUrl}" target="_blank" rel="noopener noreferrer" class="review-link">${safeLabel}</a>`);
      };

      text = text.replace(/<a\b[^>]*?\bhref=["'](https?:\/\/[^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi, (_, url, label) => buildSafeLink(url, label));
      text = text.replace(/\[([^\][\n]+)\]\(\s*(https?:\/\/[^\s)]+)\s*\)/g, (_, label, url) => buildSafeLink(url, label));
      text = text.replace(/\[url=(https?:\/\/[^\]]+)\]([\s\S]*?)\[\/url\]/gi, (_, url, label) => buildSafeLink(url, label));
      text = text.replace(/\[url\](https?:\/\/[^\s[\]]+)\[\/url\]/gi, (_, url) => buildSafeLink(url, url));

      // 6. Escape any remaining raw HTML characters
      text = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

      // 7. Convert AniList & BBCode Spoiler blocks (~! ... !~ and [spoiler]...[/spoiler])
      const renderSpoiler = (_, inner) => {
        const cleaned = inner.trim();
        return addToken(
          `<span class="review-spoiler" onclick="this.classList.toggle('revealed')" onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();this.classList.toggle('revealed');}" role="button" tabindex="0" title="Click to reveal spoiler"><span class="review-spoiler-label">Spoiler</span><span class="review-spoiler-content">${cleaned}</span></span>`
        );
      };
      text = text.replace(/~!\s*([\s\S]*?)\s*!~/g, renderSpoiler);
      text = text.replace(/\[spoiler(?:=[^\]]*)?\]([\s\S]*?)\[\/spoiler\]/gi, renderSpoiler);

      // 8. Convert Centering blocks (~~~ ... ~~~ and [center]...[/center])
      text = text.replace(/~~~\s*([\s\S]*?)\s*~~~/g, '<div class="review-center">$1</div>');
      text = text.replace(/\[center\]([\s\S]*?)\[\/center\]/gi, '<div class="review-center">$1</div>');

      // 9. Convert Blockquotes ([quote]...[/quote] and > lines)
      text = text.replace(/\[quote(?:=[^\]]*)?\]([\s\S]*?)\[\/quote\]/gi, '<blockquote class="review-quote">$1</blockquote>');
      text = text.replace(/(?:^|\n)&gt;\s?([^\n]+)(?=\n|$)/g, '\n<blockquote class="review-quote">$1</blockquote>');

      // 10. Convert Headers (#, ##, ###, ####) and [size=...] headings
      text = text.replace(/\[size=(?:1[5-9]|[2-9]\d)\]([\s\S]*?)\[\/size\]/gi, '<h4 class="review-heading">$1</h4>');
      text = text.replace(/\[\/?size(?:=[^\]]*)?\]/gi, '');
      text = text.replace(/\[\/?color(?:=[^\]]*)?\]/gi, '');
      text = text.replace(/(?:^|\n)####\s+([^\n]+)/g, '\n<h4 class="review-heading">$1</h4>');
      text = text.replace(/(?:^|\n)###\s+([^\n]+)/g, '\n<h4 class="review-heading">$1</h4>');
      text = text.replace(/(?:^|\n)##\s+([^\n]+)/g, '\n<h3 class="review-heading">$1</h3>');
      text = text.replace(/(?:^|\n)#\s+([^\n]+)/g, '\n<h3 class="review-heading">$1</h3>');

      // 11. Horizontal rules (---, ***, ___, [hr])
      text = text.replace(/\[hr\]/gi, '<hr class="review-hr">');
      text = text.replace(/(?:^|\n)[ \t]*(?:-{3,}|\*{3,}|_{3,})[ \t]*(?=\n|$)/g, '\n<hr class="review-hr">\n');

      // 12. Bold, Italic, Underline, Strikethrough (BBCode & Markdown)
      text = text.replace(/\[b\]([\s\S]*?)\[\/b\]/gi, '<strong>$1</strong>');
      text = text.replace(/\*\*([^\n*]+?)\*\*/g, '<strong>$1</strong>');
      text = text.replace(/__([^\n_]+?)__/g, '<strong>$1</strong>');

      text = text.replace(/\[i\]([\s\S]*?)\[\/i\]/gi, '<em>$1</em>');
      text = text.replace(/\*([^\n*]+?)\*/g, '<em>$1</em>');
      text = text.replace(/\b_([^\n_]+?)_\b/g, '<em>$1</em>');

      text = text.replace(/\[u\]([\s\S]*?)\[\/u\]/gi, '<u>$1</u>');
      text = text.replace(/\[s\]([\s\S]*?)\[\/s\]/gi, '<del>$1</del>');
      text = text.replace(/~~([^\n~]+?)~~/g, '<del>$1</del>');

      // 13. Lists (- item, * item, [*] item)
      text = text.replace(/\[\/?list(?:=[^\]]*)?\]/gi, '\n');
      text = text.replace(/(?:^|\n)[ \t]*(?:[-*]|\[\*\])[ \t]+([^\n]+)/g, '\n<li class="review-list-item">$1</li>');
      text = text.replace(/(?:<li class="review-list-item">[\s\S]*?<\/li>\n?)+/g, (match) => `<ul class="review-list">${match}</ul>`);

      // 14. Clean paragraph & line-break assembly
      const paragraphs = text
        .split(/\n{2,}/)
        .map(p => p.trim())
        .filter(Boolean)
        .map(p => {
          if (/^<(?:div|figure|blockquote|h3|h4|hr|ul)/i.test(p)) {
            return p.replace(/\n/g, '<br>');
          }
          return `<p class="review-p">${p.replace(/\n/g, '<br>')}</p>`;
        });

      let finalHtml = paragraphs.join('');

      // 15. Restore tokenized elements (spoilers may contain formatted text or links, so run twice)
      for (let pass = 0; pass < 2; pass++) {
        finalHtml = finalHtml.replace(/@@ZENITH_REV_TOK_(\d+)@@/g, (_, idx) => tokens[Number(idx)] || '');
      }

      return finalHtml;
    },

    // ---- ANIME CARD COMPONENT ----
    renderAnimeCard(anime, options = {}) {
      if (!anime) return '';
      const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
      const displayTitle = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
      const rank = options.rank ? `<span class="card-rank-badge">#${options.rank}</span>` : '';
      const score = anime.rating > 0 ? `★ ${anime.ratingDisplay || anime.rating}` : '';
      const totalEp = anime.totalEpisodes > 0 ? `${anime.totalEpisodes} Ep` : (anime.type || 'TV');

      return `
        <article class="anime-card" data-id="${anime.id}" tabindex="0" role="button" aria-label="${displayTitle}">
          <div class="card-poster-wrapper">
            <img class="card-poster" src="${anime.coverImage}" alt="${displayTitle}" loading="lazy" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'200\\' height=\\'300\\' fill=\\'%23141b27\\'><rect width=\\'200\\' height=\\'300\\'/><text x=\\'50%\\' y=\\'50%\\' fill=\\'%2394a3b8\\' text-anchor=\\'middle\\'>No Image</text></svg>'">
            ${rank}
            <div class="card-badges-top">
              ${score ? `<span class="badge-score">${score}</span>` : ''}
              <span class="badge-ep">${totalEp}</span>
            </div>
            <div class="card-hover-overlay">
              <button type="button" class="btn-card-play" title="Watch Now" data-play-id="${anime.id}">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="card-info">
            <h4 class="card-title" title="${displayTitle}">${displayTitle}</h4>
            <div class="card-meta">
              <span>${anime.type || 'TV'}</span>
              ${anime.year ? `<span>•</span><span>${anime.year}</span>` : ''}
              ${anime.status === 'RELEASING' ? '<span class="status-dot"></span><span>Airing</span>' : ''}
            </div>
          </div>
        </article>
      `;
    },

    // ---- 1. HOME SCREEN ----
    async renderHomeScreen(container) {
      clearInterval(this.heroInterval);
      this.heroInterval = null;
      container.innerHTML = `
        <div class="home-screen-skeleton">
          <div class="skeleton-hero"></div>
          <div class="skeleton-row"></div>
          <div class="skeleton-row"></div>
        </div>
      `;

      try {
        const [trending, popular, topRated] = await Promise.all([
          window.ZenithAPI.getTrending(1, 15),
          window.ZenithAPI.getPopular(1, 15),
          window.ZenithAPI.getTopRated(1, 15)
        ]);

        const continueWatching = window.ZenithStorage.getContinueWatchingList();
        this.heroAnimeList = trending.slice(0, 5);

        container.innerHTML = `
          <!-- Spotlight Hero Carousel -->
          <section class="hero-carousel-section" id="hero-carousel-section">
            <div class="hero-carousel-track" id="hero-carousel-track">
              ${this.renderHeroCards(this.heroAnimeList)}
            </div>
            <div class="hero-indicators" id="hero-indicators">
              ${this.heroAnimeList.map((_, i) => `<button type="button" class="hero-pill ${i === 0 ? 'active' : ''}" data-index="${i}" aria-label="Go to slide ${i + 1}"></button>`).join('')}
            </div>
          </section>

          <div class="container home-content">
            <!-- Continue Watching Row (If Exists) -->
            ${continueWatching.length > 0 ? `
              <section class="catalog-section continue-watching-section">
                <div class="section-title-row">
                  <div class="section-title-left">
                    <span class="section-dot"></span>
                    <h2 class="section-heading">Continue Watching</h2>
                  </div>
                  <button type="button" class="btn-clear-history" id="btn-clear-continue">Clear All</button>
                </div>
                <div class="continue-watching-grid">
                  ${continueWatching.map(p => this.renderContinueCard(p)).join('')}
                </div>
              </section>
            ` : ''}

            <!-- Genre Quick Filter Chips -->
            <section class="genre-chips-section">
              <div class="genre-chips-scroll">
                ${['Action', 'Adventure', 'Fantasy', 'Shounen', 'Romance', 'Supernatural', 'Comedy', 'Drama', 'Sci-Fi'].map(g => `
                  <button type="button" class="genre-chip" data-genre="${g}">
                    <span>${g}</span>
                  </button>
                `).join('')}
              </div>
            </section>

            <!-- Top Trending This Week (12 items for clean 2/3/4/6-col grid balance) -->
            <section class="catalog-section">
              <div class="section-title-row">
                <div class="section-title-left">
                  <span class="section-dot"></span>
                  <h2 class="section-heading">Trending This Week</h2>
                </div>
                <a href="#search?sort=trending" class="section-more-link">See All &rarr;</a>
              </div>
              <div class="anime-cards-grid">
                ${trending.slice(0, 12).map((a, idx) => this.renderAnimeCard(a, { rank: idx + 1 })).join('')}
              </div>
            </section>

            <!-- Top Rated Masterpieces -->
            <section class="catalog-section">
              <div class="section-title-row">
                <div class="section-title-left">
                  <span class="section-dot"></span>
                  <h2 class="section-heading">Top Rated Masterpieces (8.0+)</h2>
                </div>
                <a href="#search?sort=score" class="section-more-link">See All &rarr;</a>
              </div>
              <div class="anime-cards-grid">
                ${topRated.slice(0, 12).map(a => this.renderAnimeCard(a)).join('')}
              </div>
            </section>

            <!-- Popular Anime Catalog -->
            <section class="catalog-section">
              <div class="section-title-row">
                <div class="section-title-left">
                  <span class="section-dot"></span>
                  <h2 class="section-heading">Popular Fan Favorites</h2>
                </div>
                <a href="#search?sort=popularity" class="section-more-link">See All &rarr;</a>
              </div>
              <div class="anime-cards-grid">
                ${popular.slice(0, 12).map(a => this.renderAnimeCard(a)).join('')}
              </div>
            </section>
          </div>
        `;

        this.bindHomeEvents(container);
        this.startHeroAutoRotation();
      } catch (err) {
        container.innerHTML = `
          <div class="container error-state">
            <h3>Unable to load anime catalog</h3>
            <p>${err.message}</p>
            <button type="button" class="btn btn-primary" onclick="window.location.reload()">Retry Connection</button>
          </div>
        `;
      }
    },

    renderHeroCards(list) {
      const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
      return list.map((anime, idx) => {
        const title = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
        const banner = anime.bannerImage || anime.coverImage;
        const synopsis = anime.synopsis || '';
        const shortSynopsis = synopsis.length > 180 ? synopsis.slice(0, 180) + '...' : synopsis;
        const isFav = window.ZenithStorage.isFavorite(anime.id);

        return `
          <div class="hero-slide ${idx === 0 ? 'active' : ''}" data-slide="${idx}">
            <div class="hero-slide-backdrop" style="background-image: url('${banner}')"></div>
            <div class="hero-slide-scrim"></div>
            <div class="container hero-slide-content">
              <div class="hero-slide-meta">
                <span class="hero-badge-tag">${anime.type || 'TV'}</span>
                ${anime.rating > 0 ? `<span class="hero-badge-score">★ ${anime.ratingDisplay || anime.rating}</span>` : ''}
                ${anime.year ? `<span class="hero-badge-year">${anime.year}</span>` : ''}
                <span class="hero-badge-genre">${anime.genres.split(',').slice(0, 2).join(' • ')}</span>
              </div>
              <h1 class="hero-slide-title">${title}</h1>
              <p class="hero-slide-synopsis">${shortSynopsis}</p>
              <div class="hero-slide-actions">
                <a href="#watch/${anime.id}/1" class="btn btn-primary btn-lg">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8 5v14l11-7z"/>
                  </svg>
                  <span>Watch Ep 1</span>
                </a>
                <a href="#details/${anime.id}" class="btn btn-secondary btn-lg">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="16" x2="12" y2="12"></line>
                    <line x1="12" y1="8" x2="12.01" y2="8"></line>
                  </svg>
                  <span>Details</span>
                </a>
                <button type="button" class="btn btn-ghost btn-lg btn-hero-fav ${isFav ? 'active' : ''}" data-fav-id="${anime.id}" title="Toggle Favorite" aria-label="Toggle Favorite">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="${isFav ? '#ef4444' : 'none'}" stroke="${isFav ? '#ef4444' : 'currentColor'}" stroke-width="2">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        `;
      }).join('');
    },

    renderContinueCard(p) {
      const percent = Math.min(100, Math.round(((p.currentTimeSec || 0) / (p.durationSec || 1440)) * 100));
      const remainingMinutes = Math.max(1, Math.round(((p.durationSec || 1440) - (p.currentTimeSec || 0)) / 60));

      return `
        <div class="continue-card" data-anime-id="${p.animeId}" data-ep="${p.episodeNumber}">
          <div class="continue-thumb-wrap">
            <img src="${p.bannerImage || p.coverImage}" alt="${p.animeTitle}" loading="lazy">
            <div class="continue-scrim"></div>
            <a href="#watch/${p.animeId}/${p.episodeNumber}" class="continue-play-badge" title="Resume Playback">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </a>
            <span class="continue-ep-badge">Ep ${p.episodeNumber}</span>
            <button type="button" class="continue-remove-btn" data-remove-anime="${p.animeId}" data-remove-ep="${p.episodeNumber}" title="Remove from Continue Watching">&times;</button>
          </div>
          <div class="continue-progress-bar">
            <div class="continue-progress-fill" style="width: ${percent}%"></div>
          </div>
          <div class="continue-card-meta">
            <h4 class="continue-card-title"><a href="#details/${p.animeId}" class="continue-title-link">${p.animeTitle}</a></h4>
            <div class="continue-card-sub">
              <span>${p.episodeTitle || `Episode ${p.episodeNumber}`}</span>
              <span class="continue-remaining">${p.completed ? 'Completed ✓' : `${remainingMinutes}m left`}</span>
            </div>
          </div>
        </div>
      `;
    },

    bindHomeEvents(container) {
      // Anime card click
      this.bindCardEvents(container);

      // Hero Favorite button click
      container.querySelectorAll('.btn-hero-fav').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          e.stopPropagation();
          const favId = btn.getAttribute('data-fav-id');
          const anime = this.heroAnimeList.find(a => String(a.id) === String(favId));
          if (!anime) return;

          const isFav = window.ZenithStorage.toggleFavorite(anime);
          btn.classList.toggle('active', isFav);
          const svg = btn.querySelector('svg');
          if (svg) {
            svg.setAttribute('fill', isFav ? '#ef4444' : 'none');
            svg.setAttribute('stroke', isFav ? '#ef4444' : 'currentColor');
          }
          this.showToast(isFav ? `Added ${anime.title} to Favorites` : `Removed from Favorites`);
        });
      });

      // Hero indicators click
      container.querySelectorAll('.hero-pill').forEach(pill => {
        pill.addEventListener('click', () => {
          const idx = parseInt(pill.getAttribute('data-index'), 10);
          this.switchHeroSlide(idx);
        });
      });

      // Genre chips click
      container.querySelectorAll('.genre-chip').forEach(chip => {
        chip.addEventListener('click', () => {
          const genre = chip.getAttribute('data-genre');
          window.location.hash = `#search?genre=${encodeURIComponent(genre)}`;
        });
      });

      // Continue watching remove single
      container.querySelectorAll('.continue-remove-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const animeId = btn.getAttribute('data-remove-anime');
          const ep = btn.getAttribute('data-remove-ep');
          window.ZenithStorage.deleteProgress(animeId, ep);
          this.showToast('Removed from Continue Watching');
          const card = btn.closest('.continue-card');
          card?.remove();
          if (container.querySelectorAll('.continue-card').length === 0) {
            container.querySelector('.continue-watching-section')?.remove();
          }
        });
      });

      // Clear all continue watching
      document.getElementById('btn-clear-continue')?.addEventListener('click', () => {
        if (confirm('Clear all Continue Watching history?')) {
          window.ZenithStorage.clearAllProgress();
          this.showToast('Cleared watch history');
          document.querySelector('.continue-watching-section')?.remove();
        }
      });
    },

    startHeroAutoRotation() {
      clearInterval(this.heroInterval);
      if (this.heroAnimeList.length <= 1) return;
      this.heroInterval = setInterval(() => {
        if (document.hidden) return;
        const next = (this.heroIndex + 1) % this.heroAnimeList.length;
        this.switchHeroSlide(next);
      }, 6500);
    },

    switchHeroSlide(idx) {
      this.heroIndex = idx;
      const slides = document.querySelectorAll('.hero-slide');
      const pills = document.querySelectorAll('.hero-pill');

      slides.forEach((s, i) => s.classList.toggle('active', i === idx));
      pills.forEach((p, i) => p.classList.toggle('active', i === idx));
    },

    // ---- 2. SEARCH SCREEN ----
    async renderSearchScreen(container, queryParams = {}) {
      const q = queryParams.q || '';
      const genre = queryParams.genre || 'All';
      const sort = queryParams.sort || 'popularity';

      container.innerHTML = `
        <div class="container search-screen-container">
          <!-- Search Header Bar -->
          <div class="search-bar-wrapper">
            <div class="search-input-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input type="text" id="main-search-input" class="main-search-input" placeholder="Search anime by English, Japanese, or synonyms..." value="${q}">
              <button type="button" class="search-clear-btn" id="search-clear-btn" style="display:${q ? 'block' : 'none'}" aria-label="Clear search">&times;</button>
            </div>
            <!-- Random Anime Roulette Button -->
            <button type="button" class="btn btn-secondary btn-roulette" id="btn-roulette" title="Pick Random Anime">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="2" width="20" height="20" rx="5"></rect>
                <path d="M16 8h.01M8 8h.01M12 12h.01M16 16h.01M8 16h.01"></path>
              </svg>
              <span>Random</span>
            </button>
          </div>

          <!-- Filter Pills Row -->
          <div class="search-filters-row">
            <select class="filter-select" id="filter-format" aria-label="Filter by Format">
              <option value="All">All Formats</option>
              <option value="TV">TV Series</option>
              <option value="MOVIE">Movie</option>
              <option value="OVA">OVA</option>
              <option value="ONA">ONA</option>
              <option value="SPECIAL">Special</option>
            </select>

            <select class="filter-select" id="filter-status" aria-label="Filter by Status">
              <option value="All">All Statuses</option>
              <option value="RELEASING">Currently Airing</option>
              <option value="FINISHED">Finished Airing</option>
              <option value="NOT_YET_RELEASED">Not Yet Aired</option>
            </select>

            <select class="filter-select" id="filter-genre" aria-label="Filter by Genre">
              <option value="All">All Genres</option>
              ${['Action', 'Adventure', 'Comedy', 'Drama', 'Fantasy', 'Horror', 'Mecha', 'Mystery', 'Romance', 'Sci-Fi', 'Slice of Life', 'Sports', 'Supernatural'].map(g => `
                <option value="${g}" ${g === genre ? 'selected' : ''}>${g}</option>
              `).join('')}
            </select>

            <select class="filter-select" id="filter-sort" aria-label="Sort Results">
              <option value="popularity" ${sort === 'popularity' ? 'selected' : ''}>Popularity</option>
              <option value="score" ${sort === 'score' ? 'selected' : ''}>Top Score</option>
              <option value="trending" ${sort === 'trending' ? 'selected' : ''}>Trending</option>
              <option value="newest" ${sort === 'newest' ? 'selected' : ''}>Release Date</option>
              <option value="title a-z" ${sort === 'title a-z' ? 'selected' : ''}>Title A-Z</option>
            </select>
          </div>

          <!-- Results Header & Grid -->
          <div class="search-results-header">
            <h3 id="search-results-heading">Showing Search Results</h3>
            <span id="search-count-badge" class="badge-count">...</span>
          </div>

          <div class="anime-cards-grid" id="search-results-grid">
            <div class="loading-spinner-box">
              <div class="spinner"></div>
              <span>Searching catalog...</span>
            </div>
          </div>
        </div>
      `;

      const input = document.getElementById('main-search-input');
      const clearBtn = document.getElementById('search-clear-btn');
      const resultsGrid = document.getElementById('search-results-grid');
      const countBadge = document.getElementById('search-count-badge');
      let searchDebounce = null;

      const performSearch = async () => {
        resultsGrid.innerHTML = `
          <div class="loading-spinner-box">
            <div class="spinner"></div>
            <span>Searching anime...</span>
          </div>
        `;

        const queryStr = input.value.trim();
        const format = document.getElementById('filter-format').value;
        const status = document.getElementById('filter-status').value;
        const selGenre = document.getElementById('filter-genre').value;
        const selSort = document.getElementById('filter-sort').value;

        try {
          const results = await window.ZenithAPI.searchAnime(queryStr, {
            format,
            status,
            genre: selGenre,
            sort: selSort
          });

          countBadge.textContent = `${results.length} found`;

          if (results.length === 0) {
            resultsGrid.innerHTML = `
              <div class="empty-state-box">
                <p>No anime found matching your criteria.</p>
                <small>Try checking your spelling or adjusting filters.</small>
              </div>
            `;
          } else {
            resultsGrid.innerHTML = results.map(a => this.renderAnimeCard(a)).join('');
            this.bindCardEvents(resultsGrid);
          }
        } catch (e) {
          resultsGrid.innerHTML = `<div class="error-state"><p>Search error: ${e.message}</p></div>`;
        }
      };

      input.addEventListener('input', () => {
        clearBtn.style.display = input.value ? 'block' : 'none';
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(performSearch, 450);
      });

      clearBtn.addEventListener('click', () => {
        input.value = '';
        clearBtn.style.display = 'none';
        performSearch();
      });

      ['filter-format', 'filter-status', 'filter-genre', 'filter-sort'].forEach(id => {
        document.getElementById(id)?.addEventListener('change', performSearch);
      });

      // Roulette button
      document.getElementById('btn-roulette')?.addEventListener('click', async () => {
        this.showToast('Rolling random anime roulette...');
        const random = await window.ZenithAPI.getRandomAnime();
        if (random) {
          window.location.hash = `#details/${random.id}`;
        }
      });

      // Initial execution
      performSearch();

      if (!q && document.activeElement === document.body && window.innerWidth > 768) {
        setTimeout(() => input?.focus(), 100);
      }
    },

    bindCardEvents(container) {
      container.querySelectorAll('.anime-card').forEach(card => {
        card.addEventListener('click', (e) => {
          if (e.target.closest('.btn-card-play')) return;
          const id = card.getAttribute('data-id');
          if (id) window.location.hash = `#details/${id}`;
        });
        card.addEventListener('keydown', (e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            const id = card.getAttribute('data-id');
            if (id) window.location.hash = `#details/${id}`;
          }
        });
      });

      container.querySelectorAll('.btn-card-play').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const id = btn.getAttribute('data-play-id');
          if (id) window.location.hash = `#watch/${id}/1`;
        });
      });
    },

    // ---- 3. ANIME DETAILS SCREEN ----
    async renderDetailsScreen(container, animeId) {
      container.innerHTML = `
        <div class="loading-screen-wrap">
          <div class="spinner"></div>
          <span>Loading anime details...</span>
        </div>
      `;

      try {
        const anime = await window.ZenithAPI.getAnimeDetails(animeId);
        if (!anime) throw new Error('Anime not found');

        const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
        const displayTitle = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
        const banner = anime.bannerImage || anime.coverImage;
        const isFav = window.ZenithStorage.isFavorite(anime.id);
        const currentStatus = window.ZenithStorage.getWatchStatus(anime.id);
        const latestProgress = window.ZenithStorage.getLatestProgressForAnime(anime.id);
        const resumeEp = latestProgress ? latestProgress.episodeNumber : 1;
        const totalEp = anime.totalEpisodes || 24;
        const maxCount = totalEp > 0 ? Math.min(totalEp, 500) : 24;
        const chunkSize = 50;
        const initialEndEp = maxCount > chunkSize ? chunkSize : maxCount;

        container.innerHTML = `
          <div class="details-page-wrap">
            <!-- Hero Header Backdrop Scrim -->
            <div class="details-backdrop-hero">
              <div class="details-backdrop-bg" style="background-image: url('${banner}')"></div>
              <div class="details-backdrop-gradient"></div>
            </div>

            <!-- Details Hero Content Row -->
            <div class="container details-header-content">
              <div class="details-poster-box">
                <img src="${anime.coverImage}" alt="${displayTitle}" class="details-poster-img">
              </div>

              <div class="details-title-box">
                <div class="details-tags-row">
                  <span class="badge-pill badge-primary">${anime.type || 'TV'}</span>
                  ${anime.rating > 0 ? `<span class="badge-pill badge-score">★ ${anime.ratingDisplay}</span>` : ''}
                  ${anime.year ? `<span class="badge-pill">${anime.year}</span>` : ''}
                  <span class="badge-pill">${anime.status === 'RELEASING' ? 'Currently Airing' : 'Completed'}</span>
                </div>

                <h1 class="details-title">${displayTitle}</h1>
                ${anime.englishTitle && anime.romajiTitle && anime.englishTitle !== anime.romajiTitle ? `
                  <h2 class="details-subtitle">${titleLang === 'romaji' ? anime.englishTitle : anime.romajiTitle}</h2>
                ` : ''}

                <div class="details-meta-line">
                  <span><strong>Studio:</strong> ${anime.studio}</span>
                  <span>•</span>
                  <span><strong>Episodes:</strong> ${totalEp > 0 ? totalEp : 'Airing'}</span>
                  <span>•</span>
                  <span><strong>Genres:</strong> ${anime.genres}</span>
                </div>

                <!-- Primary Action Buttons -->
                <div class="details-action-buttons">
                  <a href="#watch/${anime.id}/${resumeEp}" class="btn btn-primary btn-lg">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M8 5v14l11-7z"/>
                    </svg>
                    <span>${latestProgress ? `Resume Ep ${resumeEp}` : 'Watch Episode 1'}</span>
                  </a>

                  <!-- Watch Status Dropdown -->
                  <div class="status-dropdown-wrap">
                    <button type="button" class="btn btn-secondary btn-lg" id="btn-status-dropdown">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                      </svg>
                      <span id="btn-status-label">${this.formatStatusLabel(currentStatus)}</span>
                    </button>
                    <div class="status-dropdown-menu" id="status-dropdown-menu">
                      <button type="button" class="status-menu-item" data-status="WATCHING">Watching</button>
                      <button type="button" class="status-menu-item" data-status="COMPLETED">Completed</button>
                      <button type="button" class="status-menu-item" data-status="PLAN_TO_WATCH">Plan to Watch</button>
                      <button type="button" class="status-menu-item" data-status="ON_HOLD">On Hold</button>
                      <button type="button" class="status-menu-item" data-status="DROPPED">Dropped</button>
                      <hr class="status-menu-divider">
                      <button type="button" class="status-menu-item remove" data-status="REMOVE">Remove from Library</button>
                    </div>
                  </div>

                  <!-- Open in Zenith Android App Button -->
                  <button type="button" class="btn btn-secondary btn-lg btn-open-app" id="btn-open-in-app" title="Open in Zenith Anime Android App">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                      <line x1="12" y1="18" x2="12.01" y2="18"></line>
                    </svg>
                    <span>Open in App</span>
                  </button>

                  <!-- Favorite Toggle -->
                  <button type="button" class="btn btn-secondary btn-icon btn-lg" id="btn-toggle-fav" title="Add to Favorites" aria-label="Add to Favorites">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="${isFav ? '#ef4444' : 'none'}" stroke="${isFav ? '#ef4444' : 'currentColor'}" stroke-width="2">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                  </button>

                  <!-- Share Link -->
                  <button type="button" class="btn btn-secondary btn-icon btn-lg" id="btn-share" title="Share Anime Link" aria-label="Share Anime Link">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="18" cy="5" r="3"></circle>
                      <circle cx="6" cy="12" r="3"></circle>
                      <circle cx="18" cy="19" r="3"></circle>
                      <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                      <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Segmented Navigation Tabs -->
            <div class="details-tabs-bar">
              <div class="container tabs-scroll-container">
                <button type="button" class="details-tab-btn active" data-tab="episodes">Episodes</button>
                <button type="button" class="details-tab-btn" data-tab="overview">Overview</button>
                <button type="button" class="details-tab-btn" data-tab="characters">Characters & VAs</button>
                <button type="button" class="details-tab-btn" data-tab="reviews">Reviews</button>
                <button type="button" class="details-tab-btn" data-tab="related">Related</button>
                <button type="button" class="details-tab-btn" data-tab="themes">Themes</button>
              </div>
            </div>

            <!-- Tab Panels Content Container -->
            <div class="container details-tab-body">
              <!-- 1. EPISODES PANEL -->
              <div class="tab-panel active" id="tab-panel-episodes">
                <div class="episodes-toolbar">
                  <div class="episodes-range-selector" id="episodes-range-selector">
                    ${this.renderEpisodeRangePills(maxCount, chunkSize)}
                  </div>
                  <div class="episodes-view-mode">
                    <button type="button" class="view-mode-btn active" id="btn-mode-grid" title="Compact Grid" aria-label="Compact Grid View">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M4 11h5V5H4v6zm0 7h5v-6H4v6zm6 0h5v-6h-5v6zm6 0h5v-6h-5v6zm-6-7h5V5h-5v6zm6-6v6h5V5h-5z"/>
                      </svg>
                    </button>
                    <button type="button" class="view-mode-btn" id="btn-mode-list" title="Detailed List" aria-label="Detailed List View">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M4 14h4v-4H4v4zm0 5h4v-4H4v4zM4 9h4V5H4v4zm5 5h12v-4H9v4zm0 5h12v-4H9v4zM9 5v4h12V5H9z"/>
                      </svg>
                    </button>
                  </div>
                </div>

                <div class="episodes-container mode-grid" id="episodes-container">
                  ${this.renderEpisodesGrid(anime, totalEp, 1, initialEndEp)}
                </div>
              </div>

              <!-- 2. OVERVIEW PANEL -->
              <div class="tab-panel" id="tab-panel-overview">
                <div class="overview-grid">
                  <div class="overview-synopsis-col">
                    <h3>Synopsis</h3>
                    <p class="synopsis-full-text">${anime.synopsis}</p>
                  </div>
                  <div class="overview-meta-card">
                    <h3>Anime Information</h3>
                    <ul class="meta-details-list">
                      <li><span>Format</span><strong>${anime.type}</strong></li>
                      <li><span>Episodes</span><strong>${totalEp > 0 ? totalEp : 'Unknown'}</strong></li>
                      <li><span>Status</span><strong>${anime.status}</strong></li>
                      <li><span>Season</span><strong>${anime.season} ${anime.year}</strong></li>
                      <li><span>Studio</span><strong>${anime.studio}</strong></li>
                      <li><span>Average Score</span><strong>${anime.rating > 0 ? anime.ratingDisplay : 'N/A'}</strong></li>
                      <li><span>Genres</span><strong>${anime.genres}</strong></li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 3. CHARACTERS & VAS PANEL -->
              <div class="tab-panel" id="tab-panel-characters">
                <div class="characters-grid">
                  ${anime.characters?.length > 0 ? anime.characters.map(c => `
                    <div class="character-card">
                      <div class="character-side">
                        <img src="${c.characterImage}" alt="${c.characterName}" class="char-thumb" loading="lazy">
                        <div class="char-names">
                          <strong>${c.characterName}</strong>
                          <small>${c.role}</small>
                        </div>
                      </div>
                      ${c.vaName ? `
                        <div class="va-side">
                          <div class="va-names">
                            <strong>${c.vaName}</strong>
                            <small>${c.vaLanguage}</small>
                          </div>
                          <img src="${c.vaImage}" alt="${c.vaName}" class="va-thumb" loading="lazy">
                        </div>
                      ` : ''}
                    </div>
                  `).join('') : '<p class="empty-text">No character information available.</p>'}
                </div>
              </div>

              <!-- 4. REVIEWS PANEL (ANILIST + MAL JIKAN) -->
              <div class="tab-panel" id="tab-panel-reviews">
                <div class="reviews-container" id="reviews-container">
                  <div class="spinner-box">
                    <div class="spinner"></div>
                    <span>Loading community reviews from AniList &amp; MyAnimeList...</span>
                  </div>
                </div>
              </div>

              <!-- 5. RELATED PANEL -->
              <div class="tab-panel" id="tab-panel-related">
                <div class="anime-cards-grid">
                  ${anime.relations?.length > 0 ? anime.relations.map(r => `
                    <div class="anime-card" onclick="window.location.hash='#details/${r.id}'">
                      <div class="card-poster-wrapper">
                        <img class="card-poster" src="${r.coverImage}" alt="${r.title}" loading="lazy">
                        <div class="card-badges-top">
                          <span class="badge-score">${r.relationType}</span>
                        </div>
                      </div>
                      <div class="card-info">
                        <h4 class="card-title">${r.title}</h4>
                        <div class="card-meta">
                          <span>${r.format}</span>
                        </div>
                      </div>
                    </div>
                  `).join('') : '<p class="empty-text">No related entries found.</p>'}
                </div>
              </div>

              <!-- 6. THEMES PANEL (ANIMETHEMES) -->
              <div class="tab-panel" id="tab-panel-themes">
                <div class="themes-container" id="themes-container">
                  <div class="spinner-box">
                    <div class="spinner"></div>
                    <span>Loading clean themes from AnimeThemes.moe...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        `;

        this.bindDetailsEvents(container, anime, maxCount, chunkSize);
      } catch (err) {
        container.innerHTML = `
          <div class="container error-state">
            <h3>Unable to load anime details</h3>
            <p>${err.message}</p>
            <a href="#home" class="btn btn-primary">Return Home</a>
          </div>
        `;
      }
    },

    formatStatusLabel(status) {
      switch (status) {
        case 'WATCHING': return 'Watching';
        case 'COMPLETED': return 'Completed';
        case 'PLAN_TO_WATCH': return 'Plan to Watch';
        case 'ON_HOLD': return 'On Hold';
        case 'DROPPED': return 'Dropped';
        default: return 'Add to Library';
      }
    },

    renderEpisodeRangePills(maxCount, chunkSize = 50) {
      if (maxCount <= 25) return `<span class="episodes-total-label">Showing all ${maxCount} episodes</span>`;
      const ranges = [];
      for (let start = 1; start <= maxCount; start += chunkSize) {
        const end = Math.min(maxCount, start + chunkSize - 1);
        ranges.push({ start, end });
      }
      return ranges.map((r, idx) => `
        <button type="button" class="ep-range-pill ${idx === 0 ? 'active' : ''}" data-start="${r.start}" data-end="${r.end}">
          Ep ${r.start}–${r.end}
        </button>
      `).join('');
    },

    renderEpisodesGrid(anime, total, startEp = 1, endEp = null) {
      const count = total > 0 ? Math.min(total, 500) : 24;
      const start = Math.max(1, startEp);
      const end = endEp ? Math.min(count, endEp) : count;
      const progressMap = window.ZenithStorage?.getAllProgress() || {};
      const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
      const displayTitle = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
      let html = '';

      for (let i = start; i <= end; i++) {
        const key = `${anime.id}_ep_${i}`;
        const p = progressMap[key];
        const isWatched = p?.completed;
        const percent = p ? Math.min(100, Math.round((p.currentTimeSec / (p.durationSec || 1440)) * 100)) : 0;

        html += `
          <a href="#watch/${anime.id}/${i}" class="episode-box ${isWatched ? 'watched' : ''}" data-ep="${i}">
            <div class="ep-thumb-preview">
              <img src="${anime.bannerImage || anime.coverImage}" alt="Episode ${i}" loading="lazy">
              <span class="ep-play-icon">▶</span>
              <span class="ep-duration-badge">24m</span>
              ${percent > 0 ? `<div class="ep-bar"><div class="ep-bar-fill" style="width:${isWatched ? 100 : percent}%"></div></div>` : ''}
            </div>
            <div class="ep-box-info">
              <div class="ep-info-main">
                <span class="ep-num">Episode ${i}</span>
                <span class="ep-title-sub">${displayTitle} — Episode ${i}</span>
                <span class="ep-meta-sub">HD • Sub &amp; Dub${percent > 0 && !isWatched ? ` • ${percent}% watched` : ''}</span>
              </div>
              <div class="ep-info-right">
                ${isWatched ? '<span class="ep-check-badge">Watched ✓</span>' : '<span class="ep-stream-cta">Watch ▶</span>'}
              </div>
            </div>
          </a>
        `;
      }
      return html;
    },

    bindDetailsEvents(container, anime, maxCount, chunkSize) {
      // Tab switcher
      const tabButtons = container.querySelectorAll('.details-tab-btn');
      const panels = container.querySelectorAll('.tab-panel');

      tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
          const tab = btn.getAttribute('data-tab');
          tabButtons.forEach(b => b.classList.toggle('active', b === btn));
          panels.forEach(p => p.classList.toggle('active', p.id === `tab-panel-${tab}`));

          // Lazy load reviews
          if (tab === 'reviews') {
            this.loadMalReviews(anime);
          }
          // Lazy load themes
          if (tab === 'themes') {
            this.loadThemes(anime);
          }
        });
      });

      // Episode range selector pagination
      const rangeSelector = document.getElementById('episodes-range-selector');
      const epContainer = document.getElementById('episodes-container');
      rangeSelector?.querySelectorAll('.ep-range-pill').forEach(pill => {
        pill.addEventListener('click', () => {
          rangeSelector.querySelectorAll('.ep-range-pill').forEach(p => p.classList.remove('active'));
          pill.classList.add('active');
          const start = parseInt(pill.getAttribute('data-start'), 10) || 1;
          const end = parseInt(pill.getAttribute('data-end'), 10) || (start + (chunkSize || 50) - 1);
          if (epContainer) {
            epContainer.innerHTML = this.renderEpisodesGrid(anime, maxCount, start, end);
          }
        });
      });

      // Status dropdown toggle
      const statusBtn = document.getElementById('btn-status-dropdown');
      const statusMenu = document.getElementById('status-dropdown-menu');
      statusBtn?.addEventListener('click', (e) => {
        e.stopPropagation();
        statusMenu?.classList.toggle('show');
      });

      statusMenu?.querySelectorAll('.status-menu-item').forEach(item => {
        item.addEventListener('click', () => {
          const status = item.getAttribute('data-status');
          window.ZenithStorage.setWatchStatus(anime, status);
          document.getElementById('btn-status-label').textContent = this.formatStatusLabel(status === 'REMOVE' ? null : status);
          statusMenu.classList.remove('show');
          this.showToast(status === 'REMOVE' ? 'Removed from Library' : `Added to ${this.formatStatusLabel(status)}`);
        });
      });

      // Open in Zenith App button
      document.getElementById('btn-open-in-app')?.addEventListener('click', () => {
        const latest = window.ZenithStorage.getLatestProgressForAnime(anime.id);
        window.ZenithApp?.openInAndroidApp(anime.id, latest ? latest.episodeNumber : null);
      });

      // Favorite toggle
      const favBtn = document.getElementById('btn-toggle-fav');
      favBtn?.addEventListener('click', () => {
        const isFav = window.ZenithStorage.toggleFavorite(anime);
        const svg = favBtn.querySelector('svg');
        if (svg) {
          svg.setAttribute('fill', isFav ? '#ef4444' : 'none');
          svg.setAttribute('stroke', isFav ? '#ef4444' : 'currentColor');
        }
        this.showToast(isFav ? 'Added to Favorites' : 'Removed from Favorites');
      });

      // Share button (canonical universal share link)
      document.getElementById('btn-share')?.addEventListener('click', () => {
        this.shareAnime(anime, null);
      });

      // View mode (Grid vs Detailed List)
      document.getElementById('btn-mode-grid')?.addEventListener('click', (e) => {
        e.currentTarget.classList.add('active');
        document.getElementById('btn-mode-list')?.classList.remove('active');
        epContainer?.classList.remove('mode-list');
        epContainer?.classList.add('mode-grid');
      });

      document.getElementById('btn-mode-list')?.addEventListener('click', (e) => {
        e.currentTarget.classList.add('active');
        document.getElementById('btn-mode-grid')?.classList.remove('active');
        epContainer?.classList.remove('mode-grid');
        epContainer?.classList.add('mode-list');
      });

      // Close status dropdown on outside click
      document.addEventListener('click', (e) => {
        if (!e.target.closest('.status-dropdown-wrap')) {
          statusMenu?.classList.remove('show');
        }
      });
    },

    async loadMalReviews(anime) {
      const container = document.getElementById('reviews-container');
      if (!container || container.getAttribute('data-loaded') === 'true') return;

      try {
        const reviews = await window.ZenithAPI.getMalReviews(anime.malId, anime.id);
        container.setAttribute('data-loaded', 'true');

        if (reviews.length === 0) {
          container.innerHTML = '<p class="empty-text">No user reviews submitted yet for this title.</p>';
          return;
        }

        container.innerHTML = reviews.map((r, idx) => {
          const isLong = (r.body || '').length > 420 || /\bimg\d*%?\(|\!\[|\[img|<img/i.test(r.body || '');
          const formattedBody = this.formatReviewBody(r.body);
          const sourceLabel = r.source === 'MAL' ? 'MyAnimeList' : 'AniList';
          const sourceClass = r.source === 'MAL' ? 'source-mal' : 'source-anilist';
          const defaultAvatar = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40' fill='%231a2232'><rect width='40' height='40'/><circle cx='20' cy='15' r='8' fill='%2364748b'/><path d='M6 36c0-6 7-9 14-9s14 3 14 9' fill='%2364748b'/></svg>";

          return `
            <article class="mal-review-card ${r.isSpoiler ? 'card-has-spoiler' : ''}" data-review-idx="${idx}">
              <div class="review-header">
                <div class="reviewer-profile">
                  <img src="${r.avatarUrl || defaultAvatar}" alt="${r.author}" class="reviewer-avatar" loading="lazy" onerror="this.src='${defaultAvatar}'">
                  <div class="reviewer-meta">
                    <div class="reviewer-name-row">
                      <strong>${r.author}</strong>
                      <span class="review-source-badge ${sourceClass}">${sourceLabel}</span>
                      ${r.isSpoiler ? '<span class="review-spoiler-badge">Contains Spoilers</span>' : ''}
                    </div>
                    <small>${r.date ? `${r.date} • ` : ''}${r.episodesWatched > 0 ? `${r.episodesWatched} episodes watched` : 'Verified Reviewer'}</small>
                  </div>
                </div>
                <div class="review-score-badge">★ ${r.score}/10</div>
              </div>

              ${r.isSpoiler ? `
                <div class="review-card-spoiler-gate">
                  <span>⚠️ This review is flagged as containing plot spoilers.</span>
                  <button type="button" class="btn btn-secondary btn-sm btn-reveal-card-spoiler">Reveal Review</button>
                </div>
              ` : ''}

              <div class="review-body-wrapper ${r.isSpoiler ? 'spoiler-hidden' : ''}">
                <div class="review-body-container ${isLong ? 'collapsed' : ''}">
                  <div class="review-rich-content">${formattedBody}</div>
                </div>
                ${isLong ? `
                  <button type="button" class="btn-review-expand" data-expanded="false">
                    <span>Read Full Review</span>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                      <polyline points="6 9 12 15 18 9"></polyline>
                    </svg>
                  </button>
                ` : ''}
              </div>

              <div class="review-footer">
                <span>👍 ${r.helpfulCount} people found this review helpful</span>
              </div>
            </article>
          `;
        }).join('');

        // Bind expand / collapse buttons
        container.querySelectorAll('.btn-review-expand').forEach(btn => {
          btn.addEventListener('click', () => {
            const wrapper = btn.closest('.review-body-wrapper');
            const bodyContainer = wrapper?.querySelector('.review-body-container');
            if (!bodyContainer) return;

            const isExpanded = btn.getAttribute('data-expanded') === 'true';
            bodyContainer.classList.toggle('collapsed', isExpanded);
            btn.setAttribute('data-expanded', isExpanded ? 'false' : 'true');
            btn.classList.toggle('expanded', !isExpanded);
            const span = btn.querySelector('span');
            if (span) span.textContent = isExpanded ? 'Read Full Review' : 'Show Less';
          });
        });

        // Bind full-card spoiler gate reveal buttons
        container.querySelectorAll('.btn-reveal-card-spoiler').forEach(btn => {
          btn.addEventListener('click', () => {
            const card = btn.closest('.mal-review-card');
            card?.querySelector('.review-card-spoiler-gate')?.remove();
            card?.querySelector('.review-body-wrapper')?.classList.remove('spoiler-hidden');
          });
        });
      } catch (err) {
        container.innerHTML = `<p class="error-text">Failed to load reviews: ${err.message}</p>`;
      }
    },

    async loadThemes(anime) {
      const container = document.getElementById('themes-container');
      if (!container || container.getAttribute('data-loaded')) return;

      try {
        const themes = await window.ZenithAPI.getAnimeThemes(anime.id, anime.title);
        container.setAttribute('data-loaded', 'true');

        if (themes.length === 0) {
          container.innerHTML = '<p class="empty-text">No clean opening or ending video themes available.</p>';
          return;
        }

        container.innerHTML = `
          <div class="themes-list">
            ${themes.map(t => `
              <div class="theme-card">
                <div class="theme-info">
                  <span class="theme-tag ${t.type.toLowerCase()}">${t.type} ${t.sequence}</span>
                  <div class="theme-titles">
                    <strong>${t.title}</strong>
                    <small>${t.artist ? `by ${t.artist}` : 'Clean Version'} • ${t.resolution}</small>
                  </div>
                </div>
                <button type="button" class="btn btn-secondary btn-sm btn-play-theme" data-video="${t.videoUrl}" data-title="${t.title}">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8 5v14l11-7z"/>
                  </svg>
                  <span>Play Theme</span>
                </button>
              </div>
            `).join('')}
          </div>
        `;

        container.querySelectorAll('.btn-play-theme').forEach(btn => {
          btn.addEventListener('click', () => {
            const videoUrl = btn.getAttribute('data-video');
            const themeTitle = btn.getAttribute('data-title');
            UI.openModal(themeTitle, `
              <div class="theme-video-wrap">
                <video src="${videoUrl}" controls autoplay playsinline style="width: 100%; border-radius: 8px;"></video>
              </div>
            `);
          });
        });
      } catch (err) {
        container.innerHTML = `<p class="error-text">Failed to load themes: ${err.message}</p>`;
      }
    },

    // ---- 4. WATCH SCREEN ----
    async renderWatchScreen(container, animeId, episodeNumber = 1) {
      container.innerHTML = `
        <div class="watch-screen-layout">
          <!-- Video Player Viewport Container -->
          <div id="player-viewport-container" class="player-viewport-container">
            <div class="spinner-box">
              <div class="spinner"></div>
              <span>Connecting to Zenith video servers...</span>
            </div>
          </div>

          <!-- Bottom Info / Episodes Guide Container -->
          <div class="container watch-bottom-info" id="watch-bottom-info">
            <div class="spinner-box">
              <div class="spinner"></div>
              <span>Loading episode details...</span>
            </div>
          </div>
        </div>
      `;

      try {
        const anime = await window.ZenithAPI.getAnimeDetails(animeId);
        if (!anime) throw new Error('Anime not found');

        const epNum = parseInt(episodeNumber, 10) || 1;

        // Mount video player
        window.ZenithPlayer.load(anime, epNum);

        // Render bottom info strip
        const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
        const displayTitle = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
        const bottomContainer = document.getElementById('watch-bottom-info');

        if (bottomContainer) {
          bottomContainer.innerHTML = `
            <div class="watch-meta-strip">
              <div class="watch-meta-left">
                <h2>${displayTitle}</h2>
                <div class="watch-badges-row">
                  <span class="badge-pill badge-primary">Episode ${epNum}</span>
                  <span class="badge-pill">${anime.type || 'TV'}</span>
                  ${anime.rating > 0 ? `<span class="badge-pill badge-score">★ ${anime.ratingDisplay}</span>` : ''}
                </div>
              </div>
              <div class="watch-meta-right">
                <button type="button" class="btn btn-secondary btn-sm" id="watch-btn-open-app" title="Open Episode in Zenith Android App">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                    <line x1="12" y1="18" x2="12.01" y2="18"></line>
                  </svg>
                  <span>Open in App</span>
                </button>
                <button type="button" class="btn btn-secondary btn-sm" id="watch-btn-share" title="Share Episode Link">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="18" cy="5" r="3"></circle>
                    <circle cx="6" cy="12" r="3"></circle>
                    <circle cx="18" cy="19" r="3"></circle>
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
                  </svg>
                  <span>Share</span>
                </button>
                <a href="#details/${anime.id}" class="btn btn-secondary btn-sm">Anime Details</a>
              </div>
            </div>

            <!-- Quick Episode Picker Strip -->
            <div class="watch-episodes-strip">
              <h3>Episodes Guide (${anime.totalEpisodes > 0 ? anime.totalEpisodes : 'All'})</h3>
              <div class="episodes-horizontal-scroll" id="watch-episodes-scroll">
                ${this.renderWatchEpisodesScroll(anime, epNum)}
              </div>
            </div>
          `;

          document.getElementById('watch-btn-share')?.addEventListener('click', () => {
            this.shareAnime(anime, epNum);
          });

          document.getElementById('watch-btn-open-app')?.addEventListener('click', () => {
            window.ZenithApp?.openInAndroidApp(anime.id, epNum);
          });

          // Auto-scroll active episode pill into horizontal center view
          const scrollStrip = document.getElementById('watch-episodes-scroll');
          const activePill = scrollStrip?.querySelector('.watch-ep-pill.active');
          if (scrollStrip && activePill) {
            requestAnimationFrame(() => {
              const targetLeft = activePill.offsetLeft - (scrollStrip.clientWidth / 2) + (activePill.clientWidth / 2);
              scrollStrip.scrollTo({ left: Math.max(0, targetLeft), behavior: 'smooth' });
            });
          }
        }
      } catch (err) {
        container.innerHTML = `<div class="container error-state"><p>Player error: ${err.message}</p></div>`;
      }
    },

    renderWatchEpisodesScroll(anime, currentEp) {
      const total = anime.totalEpisodes > 0 ? Math.min(anime.totalEpisodes, 500) : 24;
      const progressMap = window.ZenithStorage?.getAllProgress() || {};
      let html = '';

      for (let i = 1; i <= total; i++) {
        const key = `${anime.id}_ep_${i}`;
        const isWatched = progressMap[key]?.completed;
        const isCurrent = i === currentEp;

        html += `
          <a href="#watch/${anime.id}/${i}" class="watch-ep-pill ${isCurrent ? 'active' : ''} ${isWatched ? 'watched' : ''}">
            <span>Ep ${i}</span>
            ${isWatched ? '<span class="ep-check">✓</span>' : ''}
          </a>
        `;
      }
      return html;
    },

    // ---- 5. SCHEDULE SCREEN ----
    async renderScheduleScreen(container) {
      const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
      const todayIdx = new Date().getDay();

      container.innerHTML = `
        <div class="container schedule-screen-container">
          <div class="schedule-header">
            <div class="schedule-title-left">
              <span class="section-dot"></span>
              <h1 class="section-heading">Weekly Airing Schedule</h1>
            </div>
            <p class="schedule-sub">Live countdowns for weekly broadcast television releases in Japan.</p>
          </div>

          <!-- Day Tabs Selector -->
          <div class="schedule-days-bar">
            ${days.map((d, i) => `
              <button type="button" class="schedule-day-btn ${i === todayIdx ? 'active' : ''}" data-day="${i}">
                <span class="day-name">${d.slice(0, 3)}</span>
                ${i === todayIdx ? '<span class="today-dot"></span>' : ''}
              </button>
            `).join('')}
          </div>

          <div class="schedule-grid" id="schedule-grid">
            <div class="spinner-box">
              <div class="spinner"></div>
              <span>Loading airing schedule...</span>
            </div>
          </div>
        </div>
      `;

      const loadDaySchedule = async (dayIndex) => {
        const grid = document.getElementById('schedule-grid');
        grid.innerHTML = `
          <div class="spinner-box">
            <div class="spinner"></div>
            <span>Fetching broadcasts...</span>
          </div>
        `;

        // Calculate start & end unix seconds for that day of this week
        const now = new Date();
        const currentDay = now.getDay();
        const diff = dayIndex - currentDay;

        const targetDate = new Date(now);
        targetDate.setDate(now.getDate() + diff);
        targetDate.setHours(0, 0, 0, 0);

        const startSec = Math.floor(targetDate.getTime() / 1000);
        const endSec = startSec + 86400;

        try {
          const list = await window.ZenithAPI.getSchedule(startSec, endSec);
          if (list.length === 0) {
            grid.innerHTML = '<p class="empty-text">No broadcasts scheduled for this day.</p>';
            return;
          }

          grid.innerHTML = list.map(item => {
            const anime = item.anime;
            const timeUntil = item.timeUntilAiring;
            let countdownStr = 'Aired Today';
            if (timeUntil > 0) {
              const h = Math.floor(timeUntil / 3600);
              const m = Math.floor((timeUntil % 3600) / 60);
              countdownStr = `Airs in ${h}h ${m}m`;
            }

            return `
              <div class="schedule-card" onclick="window.location.hash='#details/${anime.id}'">
                <img src="${anime.coverImage}" alt="${anime.title}" class="schedule-thumb" loading="lazy">
                <div class="schedule-info">
                  <span class="schedule-countdown-badge ${timeUntil <= 0 ? 'aired' : ''}">${countdownStr}</span>
                  <h4 class="schedule-anime-title">${anime.title}</h4>
                  <div class="schedule-meta">
                    <span>Episode ${item.episode}</span>
                    <span>•</span>
                    <span>${anime.type || 'TV'}</span>
                  </div>
                </div>
              </div>
            `;
          }).join('');
        } catch (err) {
          grid.innerHTML = `<p class="error-text">Failed to load schedule: ${err.message}</p>`;
        }
      };

      container.querySelectorAll('.schedule-day-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          container.querySelectorAll('.schedule-day-btn').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          const dayIdx = parseInt(btn.getAttribute('data-day'), 10);
          loadDaySchedule(dayIdx);
        });
      });

      // Load today's schedule
      loadDaySchedule(todayIdx);
    },

    // ---- 6. MY LIST / LIBRARY SCREEN ----
    renderMyListScreen(container) {
      container.innerHTML = `
        <div class="container mylist-screen-container">
          <div class="mylist-header">
            <div class="mylist-title-left">
              <span class="section-dot"></span>
              <h1 class="section-heading">My Anime Library</h1>
            </div>
            <div class="mylist-actions-right">
              <select class="filter-select" id="mylist-sort-select" aria-label="Sort Library">
                <option value="updated">Recently Updated</option>
                <option value="title">Title A-Z</option>
                <option value="score">Highest Score</option>
              </select>
            </div>
          </div>

          <!-- Status Filter Tabs -->
          <div class="mylist-tabs-bar">
            ${['ALL', 'WATCHING', 'COMPLETED', 'PLAN_TO_WATCH', 'ON_HOLD', 'DROPPED', 'FAVORITES'].map((tab, i) => `
              <button type="button" class="mylist-tab-btn ${i === 0 ? 'active' : ''}" data-status="${tab}">
                ${this.formatTabName(tab)}
              </button>
            `).join('')}
          </div>

          <!-- Items Grid -->
          <div class="anime-cards-grid" id="mylist-cards-grid"></div>
        </div>
      `;

      let currentTab = 'ALL';
      let currentSort = 'updated';

      const updateList = () => {
        const grid = document.getElementById('mylist-cards-grid');
        const items = window.ZenithStorage.getFilteredLibrary(currentTab, currentSort);

        if (items.length === 0) {
          grid.innerHTML = `
            <div class="empty-state-box">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
              </svg>
              <h3>No anime in this list</h3>
              <p>Add anime to your watchlist from the catalog or anime details page.</p>
              <a href="#home" class="btn btn-primary btn-sm">Explore Catalog</a>
            </div>
          `;
          return;
        }

        grid.innerHTML = items.map(a => this.renderAnimeCard(a)).join('');
        this.bindCardEvents(grid);
      };

      container.querySelectorAll('.mylist-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          container.querySelectorAll('.mylist-tab-btn').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          currentTab = btn.getAttribute('data-status');
          updateList();
        });
      });

      document.getElementById('mylist-sort-select')?.addEventListener('change', (e) => {
        currentSort = e.target.value;
        updateList();
      });

      updateList();
    },

    formatTabName(tab) {
      switch (tab) {
        case 'ALL': return 'All Saved';
        case 'WATCHING': return 'Watching';
        case 'COMPLETED': return 'Completed';
        case 'PLAN_TO_WATCH': return 'Plan to Watch';
        case 'ON_HOLD': return 'On Hold';
        case 'DROPPED': return 'Dropped';
        case 'FAVORITES': return 'Favorites';
        default: return tab;
      }
    },

    // ---- 7. SETTINGS & CUSTOMIZATION PLAYGROUND SCREEN ----
    renderSettingsScreen(container) {
      const settings = window.ZenithStorage.getSettings();

      container.innerHTML = `
        <div class="container settings-screen-container">
          <div class="settings-header">
            <span class="section-dot"></span>
            <h1 class="section-heading">Settings & Customization</h1>
          </div>

          <div class="settings-grid">
            <!-- Appearance & Theme Accent -->
            <section class="settings-card">
              <h3>Theme Accent Color</h3>
              <p class="settings-desc">Choose your personal luminous accent glow across the entire client.</p>
              <div class="theme-accent-chips">
                ${[
                  { id: 'emerald', name: 'Emerald', color: '#10b981' },
                  { id: 'cyan', name: 'Cyan', color: '#06b6d4' },
                  { id: 'sakura', name: 'Sakura', color: '#f43f5e' },
                  { id: 'violet', name: 'Violet', color: '#8b5cf6' },
                  { id: 'amber', name: 'Amber', color: '#f59e0b' },
                  { id: 'crimson', name: 'Crimson', color: '#ef4444' },
                  { id: 'indigo', name: 'Indigo', color: '#536dfe' },
                  { id: 'gold', name: 'Gold', color: '#ffd54f' }
                ].map(c => `
                  <button type="button" class="accent-chip ${settings.accent === c.id ? 'active' : ''}" data-accent="${c.id}" style="background-color: ${c.color}" title="${c.name}"></button>
                `).join('')}
              </div>
            </section>

            <!-- Language & Titles -->
            <section class="settings-card">
              <h3>Title Language Preference</h3>
              <p class="settings-desc">Globally format anime titles across Home, Catalog, Player, and Library.</p>
              <div class="segmented-options">
                <button type="button" class="seg-btn ${settings.titleLanguage === 'english' ? 'active' : ''}" data-title-lang="english">English (Default)</button>
                <button type="button" class="seg-btn ${settings.titleLanguage === 'romaji' ? 'active' : ''}" data-title-lang="romaji">Japanese (Romaji)</button>
              </div>
            </section>

            <!-- Streaming Preferences -->
            <section class="settings-card">
              <h3>Streaming Preferences</h3>
              <div class="settings-row">
                <div>
                  <strong>Default Video Server</strong>
                  <p class="settings-desc">Preferred streaming source when opening episodes.</p>
                </div>
                <select class="filter-select" id="setting-default-server">
                  <option value="aniembed" ${settings.defaultServer === 'aniembed' ? 'selected' : ''}>AniEmbed (Fast HLS)</option>
                  <option value="viduki" ${settings.defaultServer === 'viduki' ? 'selected' : ''}>Viduki Multi-Stream</option>
                  <option value="megaplay" ${settings.defaultServer === 'megaplay' ? 'selected' : ''}>MegaPlay Buzz</option>
                  <option value="vidsrc" ${settings.defaultServer === 'vidsrc' ? 'selected' : ''}>VidSrc Ultra</option>
                </select>
              </div>

              <div class="settings-row">
                <div>
                  <strong>Default Audio Language</strong>
                  <p class="settings-desc">Auto-select Subtitles or English Dub audio track.</p>
                </div>
                <div class="segmented-options">
                  <button type="button" class="seg-btn ${settings.defaultAudio === 'sub' ? 'active' : ''}" data-audio="sub">Subtitles</button>
                  <button type="button" class="seg-btn ${settings.defaultAudio === 'dub' ? 'active' : ''}" data-audio="dub">Dubbed</button>
                </div>
              </div>
            </section>

            <!-- Data & Backups -->
            <section class="settings-card">
              <h3>Backup & Data Management</h3>
              <p class="settings-desc">Export or restore your library, watch progress, and custom settings as JSON.</p>
              <div class="backup-actions-row">
                <button type="button" class="btn btn-secondary btn-sm" id="btn-export-backup">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="7 10 12 15 17 10"></polyline>
                    <line x1="12" y1="15" x2="12" y2="3"></line>
                  </svg>
                  <span>Export JSON Backup</span>
                </button>

                <label class="btn btn-secondary btn-sm file-label">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="17 8 12 3 7 8"></polyline>
                    <line x1="12" y1="3" x2="12" y2="15"></line>
                  </svg>
                  <span>Import JSON Backup</span>
                  <input type="file" id="file-import-backup" accept=".json" style="display:none;">
                </label>
              </div>

              <hr class="status-menu-divider" style="margin: 16px 0;">

              <div class="danger-actions-row">
                <button type="button" class="btn btn-ghost btn-sm" id="btn-clear-history">Clear Watch History</button>
                <button type="button" class="btn btn-ghost btn-sm text-danger" id="btn-reset-all">Reset All Local Data</button>
              </div>
            </section>

            <!-- Android Native Client Link -->
            <section class="settings-card native-client-card">
              <h3>Zenith Anime Native Android Client</h3>
              <p class="settings-desc">Looking for the native Android APK with Jetpack Compose & ExoPlayer?</p>
              <div class="native-actions-row">
                <a href="https://github.com/gokuthug1/zenith-anime/raw/master/APK/zenith-anime-latest.apk" class="btn btn-primary btn-sm" download>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/>
                  </svg>
                  <span>Download APK (v1.7.0)</span>
                </a>
                <a href="landing.html" class="btn btn-secondary btn-sm">View Showcase Page</a>
              </div>
            </section>
          </div>
        </div>
      `;

      this.bindSettingsEvents(container);
    },

    bindSettingsEvents(container) {
      // Accent chips
      container.querySelectorAll('.accent-chip').forEach(chip => {
        chip.addEventListener('click', () => {
          container.querySelectorAll('.accent-chip').forEach(c => c.classList.remove('active'));
          chip.classList.add('active');
          const acc = chip.getAttribute('data-accent');
          window.ZenithStorage.updateSettings({ accent: acc });
          document.documentElement.setAttribute('data-accent', acc);
          this.showToast(`Applied ${acc.charAt(0).toUpperCase() + acc.slice(1)} theme`);
        });
      });

      // Title language
      container.querySelectorAll('[data-title-lang]').forEach(btn => {
        btn.addEventListener('click', () => {
          container.querySelectorAll('[data-title-lang]').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          const lang = btn.getAttribute('data-title-lang');
          window.ZenithStorage.updateSettings({ titleLanguage: lang });
          this.showToast(`Title preference set to ${lang}`);
        });
      });

      // Server select
      document.getElementById('setting-default-server')?.addEventListener('change', (e) => {
        window.ZenithStorage.updateSettings({ defaultServer: e.target.value });
        this.showToast('Default streaming server updated');
      });

      // Audio language
      container.querySelectorAll('[data-audio]').forEach(btn => {
        btn.addEventListener('click', () => {
          container.querySelectorAll('[data-audio]').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          const aud = btn.getAttribute('data-audio');
          window.ZenithStorage.updateSettings({ defaultAudio: aud });
          this.showToast(`Default audio set to ${aud.toUpperCase()}`);
        });
      });

      // Export backup
      document.getElementById('btn-export-backup')?.addEventListener('click', () => {
        const json = window.ZenithStorage.exportBackupJson();
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `zenith_anime_backup_${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        URL.revokeObjectURL(url);
        this.showToast('Backup JSON exported successfully!');
      });

      // Import backup
      document.getElementById('file-import-backup')?.addEventListener('change', (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (event) => {
          const res = window.ZenithStorage.importBackupJson(event.target.result);
          if (res.success) {
            this.showToast(`Restored ${res.animeCount} anime & ${res.progressCount} progress records!`);
            setTimeout(() => window.location.reload(), 1200);
          } else {
            alert(`Backup import failed: ${res.error}`);
          }
        };
        reader.readAsText(file);
      });

      // Clear watch history
      document.getElementById('btn-clear-history')?.addEventListener('click', () => {
        if (confirm('Clear all episode watch history and resume positions?')) {
          window.ZenithStorage.clearAllProgress();
          this.showToast('Watch history cleared');
        }
      });

      // Reset all data
      document.getElementById('btn-reset-all')?.addEventListener('click', () => {
        if (confirm('WARNING: This will permanently delete your entire library, favorites, and watch progress. Continue?')) {
          window.ZenithStorage.clearAllData();
          this.showToast('All local data cleared');
          setTimeout(() => window.location.reload(), 800);
        }
      });
    }
  };

  window.ZenithUI = UI;
})(window);
