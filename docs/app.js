/**
 * Zenith Anime — Interactive Landing & Playground Script
 * Built with vanilla JavaScript. Modular, defensive, and accessible.
 */

(function () {
  "use strict";

  const ACCENT_MAP = {
    emerald: "#10b981",
    cyan: "#06b6d4",
    sakura: "#f43f5e",
    violet: "#8b5cf6",
    amber: "#f59e0b",
    crimson: "#ef4444",
    indigo: "#536dfe",
    gold: "#ffd54f"
  };

  const PLAY_SVG_PATH = "M8 5v14l11-7z";
  const PAUSE_SVG_PATH = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";

  function formatSec(sec) {
    const s = Math.max(0, Math.floor(sec || 0));
    const m = Math.floor(s / 60);
    const r = s % 60;
    return `${String(m).padStart(2, "0")}:${String(r).padStart(2, "0")}`;
  }

  function loadInitialAccent() {
    try {
      const settingsRaw = localStorage.getItem("zenith_settings");
      if (settingsRaw) {
        const parsed = JSON.parse(settingsRaw);
        if (parsed && parsed.accent && ACCENT_MAP[parsed.accent]) {
          return parsed.accent;
        }
      }
      const standalone = localStorage.getItem("zenith_accent");
      if (standalone && ACCENT_MAP[standalone]) return standalone;
    } catch {
      // ignore
    }
    return "emerald";
  }

  // State Management
  const state = {
    accent: loadInitialAccent(),
    heroSim: {
      currentSec: 624, // 10:24
      totalSec: 1440,  // 24:00
      playing: false
    },
    playground: {
      isLegacy: false,
      transportPosition: "center", // center, bottom, split
      seekbarStyle: "modern",      // modern, minimal, thick
      lockSide: "left",            // left, right
      overlayDim: 0.65,
      hiddenControls: new Set(),
      currentSec: 252,             // 04:12
      totalSec: 1425,              // 23:45
      playing: false
    }
  };

  // DOM Elements cache
  const elements = {
    html: document.documentElement,
    themeChips: document.querySelectorAll(".color-chip"),
    segButtons: document.querySelectorAll(".seg-btn"),
    toggles: document.querySelectorAll(".playground-toggle"),
    simulatorBox: document.getElementById("sim-screen"),
    simulatorLock: document.getElementById("sim-lock-icon"),
    simulatorTransportCenter: document.getElementById("sim-transport-center"),
    simulatorTransportBottom: document.getElementById("sim-transport-bottom"),
    simulatorCenterRewind: document.getElementById("sim-center-rewind"),
    simulatorCenterFF: document.getElementById("sim-center-ff"),
    simulatorBottomPlay: document.getElementById("sim-bottom-play"),
    simulatorSeekbar: document.getElementById("sim-seekbar"),
    simulatorSeekProgress: document.getElementById("sim-seekbar-progress"),
    simulatorTimeCurrent: document.getElementById("sim-time-current"),
    simulatorTimeTotal: document.getElementById("sim-time-total"),
    simulatorTopIcons: document.querySelectorAll(".sim-top-icon"),
    heroSeekbar: document.getElementById("hero-seekbar"),
    heroSeekProgress: document.getElementById("hero-seekbar-progress"),
    heroTimeCurrent: document.getElementById("hero-time-current"),
    heroPlayBtn: document.getElementById("hero-play-btn"),
    mobileToggle: document.getElementById("mobile-menu-toggle"),
    navLinks: document.getElementById("nav-links"),
    modals: document.querySelectorAll(".modal-overlay"),
    toastContainer: document.getElementById("toast-container")
  };

  /** Initialize Application */
  function init() {
    // Redirect ?anime=ID deep links opened on landing.html to index.html
    const search = window.location.search;
    if (search) {
      const params = new URLSearchParams(search);
      const animeId = params.get("anime") || params.get("id");
      const ep = params.get("ep") || params.get("episode");
      if (animeId) {
        const targetHash = ep ? `#watch/${animeId}/${ep}` : `#details/${animeId}`;
        window.location.replace(`index.html${search}${targetHash}`);
        return;
      }
    }

    applyTheme(state.accent);
    bindThemeEvents();
    bindPlaygroundEvents();
    bindInteractiveSeekbars();
    bindModalEvents();
    bindMobileNav();
    updateSimulator();
    updateHeroMockupUI();
  }

  /** Apply Theme Accent and sync with zenith_settings */
  function applyTheme(accentName) {
    const normalized = ACCENT_MAP[accentName] ? accentName : "emerald";
    state.accent = normalized;
    elements.html.setAttribute("data-accent", normalized);

    try {
      localStorage.setItem("zenith_accent", normalized);
      const existingRaw = localStorage.getItem("zenith_settings");
      const existing = existingRaw ? JSON.parse(existingRaw) : {};
      existing.accent = normalized;
      localStorage.setItem("zenith_settings", JSON.stringify(existing));
    } catch {
      // ignore storage errors
    }

    const dot = document.getElementById("theme-current-dot");
    if (dot) dot.style.backgroundColor = ACCENT_MAP[normalized] || "#10b981";

    // Update active chip state
    elements.themeChips.forEach((chip) => {
      const isTarget = chip.getAttribute("data-color") === normalized;
      chip.classList.toggle("active", isTarget);
      chip.setAttribute("aria-selected", isTarget ? "true" : "false");
      chip.setAttribute("aria-pressed", isTarget ? "true" : "false");
    });
  }

  /** Bind Theme Switcher Events */
  function bindThemeEvents() {
    const toggleBtn = document.getElementById("theme-dropdown-toggle");
    const dropdown = document.getElementById("theme-palette-dropdown");

    toggleBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      dropdown?.classList.toggle("show");
    });

    document.addEventListener("click", (e) => {
      if (!e.target.closest("#theme-dropdown-wrap")) {
        dropdown?.classList.remove("show");
      }
    });

    elements.themeChips.forEach((chip) => {
      chip.addEventListener("click", () => {
        const color = chip.getAttribute("data-color");
        if (color) {
          applyTheme(color);
          dropdown?.classList.remove("show");
        }
      });
    });
  }

  /** Bind Interactive Playground Controls */
  function bindPlaygroundEvents() {
    elements.segButtons.forEach((btn) => {
      btn.addEventListener("click", () => {
        const group = btn.getAttribute("data-group");
        const val = btn.getAttribute("data-value");
        if (!group || !val) return;

        const siblings = btn.parentElement?.querySelectorAll(".seg-btn");
        siblings?.forEach((sib) => sib.classList.remove("active"));
        btn.classList.add("active");

        if (group === "transport") {
          state.playground.transportPosition = val;
        } else if (group === "seekbar") {
          state.playground.seekbarStyle = val;
        } else if (group === "lockside") {
          state.playground.lockSide = val;
        } else if (group === "layout") {
          state.playground.isLegacy = val === "legacy";
        }

        updateSimulator();
      });
    });

    elements.toggles.forEach((toggle) => {
      toggle.addEventListener("change", (e) => {
        const target = e.target;
        const controlId = target?.getAttribute("data-control");
        if (!controlId) return;

        if (target.checked) {
          state.playground.hiddenControls.delete(controlId);
        } else {
          state.playground.hiddenControls.add(controlId);
        }
        updateSimulator();
      });
    });
  }

  /** Bind Interactive Seekbars & Transport Buttons for Hero Mockup & Live Simulator */
  function bindInteractiveSeekbars() {
    // 1. Live Simulator Seekbar Scrubbing
    if (elements.simulatorSeekbar) {
      let draggingSim = false;
      const scrubSim = (clientX) => {
        const rect = elements.simulatorSeekbar.getBoundingClientRect();
        if (!rect.width) return;
        const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
        state.playground.currentSec = Math.round(ratio * state.playground.totalSec);
        updateSimulatorTimelineUI();
      };

      elements.simulatorSeekbar.addEventListener("pointerdown", (e) => {
        draggingSim = true;
        elements.simulatorSeekbar.setPointerCapture?.(e.pointerId);
        scrubSim(e.clientX);
      });
      elements.simulatorSeekbar.addEventListener("pointermove", (e) => {
        if (!draggingSim) return;
        scrubSim(e.clientX);
      });
      elements.simulatorSeekbar.addEventListener("pointerup", (e) => {
        if (!draggingSim) return;
        draggingSim = false;
        elements.simulatorSeekbar.releasePointerCapture?.(e.pointerId);
      });
    }

    // 2. Hero Mockup Seekbar Scrubbing
    if (elements.heroSeekbar) {
      let draggingHero = false;
      const scrubHero = (clientX) => {
        const rect = elements.heroSeekbar.getBoundingClientRect();
        if (!rect.width) return;
        const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
        state.heroSim.currentSec = Math.round(ratio * state.heroSim.totalSec);
        updateHeroMockupUI();
      };

      elements.heroSeekbar.addEventListener("pointerdown", (e) => {
        draggingHero = true;
        elements.heroSeekbar.setPointerCapture?.(e.pointerId);
        scrubHero(e.clientX);
      });
      elements.heroSeekbar.addEventListener("pointermove", (e) => {
        if (!draggingHero) return;
        scrubHero(e.clientX);
      });
      elements.heroSeekbar.addEventListener("pointerup", (e) => {
        if (!draggingHero) return;
        draggingHero = false;
        elements.heroSeekbar.releasePointerCapture?.(e.pointerId);
      });
    }

    // 3. Simulator Play/Pause & Seek Buttons
    document.querySelectorAll(".sim-play-trigger").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.playground.playing = !state.playground.playing;
        updateSimulatorTimelineUI();
      });
    });

    document.querySelectorAll(".sim-seek-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const delta = parseInt(btn.getAttribute("data-seek"), 10) || 0;
        state.playground.currentSec = Math.min(
          state.playground.totalSec,
          Math.max(0, state.playground.currentSec + delta)
        );
        updateSimulatorTimelineUI();
      });
    });

    // 4. Hero Mockup Play/Pause & Seek Buttons
    elements.heroPlayBtn?.addEventListener("click", () => {
      state.heroSim.playing = !state.heroSim.playing;
      updateHeroMockupUI();
    });

    document.getElementById("hero-seek-back")?.addEventListener("click", () => {
      state.heroSim.currentSec = Math.max(0, state.heroSim.currentSec - 10);
      updateHeroMockupUI();
    });

    document.getElementById("hero-seek-fwd")?.addEventListener("click", () => {
      state.heroSim.currentSec = Math.min(state.heroSim.totalSec, state.heroSim.currentSec + 10);
      updateHeroMockupUI();
    });

    // 5. Tick active simulated playback
    setInterval(() => {
      if (document.hidden) return;
      if (state.playground.playing) {
        state.playground.currentSec = (state.playground.currentSec + 1) % (state.playground.totalSec + 1);
        updateSimulatorTimelineUI();
      }
      if (state.heroSim.playing) {
        state.heroSim.currentSec = (state.heroSim.currentSec + 1) % (state.heroSim.totalSec + 1);
        updateHeroMockupUI();
      }
    }, 1000);
  }

  function updateHeroMockupUI() {
    const h = state.heroSim;
    const pct = Math.min(100, Math.max(0, (h.currentSec / h.totalSec) * 100));
    if (elements.heroSeekProgress) elements.heroSeekProgress.style.width = `${pct.toFixed(1)}%`;
    if (elements.heroTimeCurrent) elements.heroTimeCurrent.textContent = formatSec(h.currentSec);
    if (elements.heroSeekbar) elements.heroSeekbar.setAttribute("aria-valuenow", String(Math.round(pct)));
    const pathEl = elements.heroPlayBtn?.querySelector("path");
    if (pathEl) pathEl.setAttribute("d", h.playing ? PAUSE_SVG_PATH : PLAY_SVG_PATH);
  }

  function updateSimulatorTimelineUI() {
    const p = state.playground;
    const pct = Math.min(100, Math.max(0, (p.currentSec / p.totalSec) * 100));
    if (elements.simulatorSeekProgress) elements.simulatorSeekProgress.style.width = `${pct.toFixed(1)}%`;
    if (elements.simulatorTimeCurrent) elements.simulatorTimeCurrent.textContent = formatSec(p.currentSec);
    if (elements.simulatorTimeTotal) elements.simulatorTimeTotal.textContent = formatSec(p.totalSec);
    if (elements.simulatorSeekbar) elements.simulatorSeekbar.setAttribute("aria-valuenow", String(Math.round(pct)));

    document.querySelectorAll(".sim-play-trigger svg path").forEach((pathEl) => {
      pathEl.setAttribute("d", p.playing ? PAUSE_SVG_PATH : PLAY_SVG_PATH);
    });
  }

  /** Update Simulator Mockup DOM based on state */
  function updateSimulator() {
    const p = state.playground;

    // Lock icon visibility & side
    if (elements.simulatorLock) {
      if (p.isLegacy || p.hiddenControls.has("lock")) {
        elements.simulatorLock.style.display = "none";
      } else {
        elements.simulatorLock.style.display = "flex";
        if (p.lockSide === "right") {
          elements.simulatorLock.style.left = "auto";
          elements.simulatorLock.style.right = "10px";
        } else {
          elements.simulatorLock.style.left = "10px";
          elements.simulatorLock.style.right = "auto";
        }
      }
    }

    // Transport position (Center vs Bottom vs Split — no duplicate Play buttons in Split mode!)
    if (elements.simulatorTransportCenter && elements.simulatorTransportBottom) {
      if (p.transportPosition === "bottom") {
        elements.simulatorTransportCenter.style.display = "none";
        elements.simulatorTransportBottom.style.display = "flex";
        if (elements.simulatorBottomPlay) elements.simulatorBottomPlay.style.display = "flex";
      } else if (p.transportPosition === "split") {
        // Split mode: Center has main Play/Pause button only; Bottom has -10s/+10s seek & +85s Intro pill
        elements.simulatorTransportCenter.style.display = "flex";
        elements.simulatorTransportBottom.style.display = "flex";
        if (elements.simulatorCenterRewind) elements.simulatorCenterRewind.style.display = "none";
        if (elements.simulatorCenterFF) elements.simulatorCenterFF.style.display = "none";
        if (elements.simulatorBottomPlay) elements.simulatorBottomPlay.style.display = "none";
      } else {
        // Center mode: Center has Rewind + Play/Pause + Fast-Forward; Bottom has only seekbar
        elements.simulatorTransportCenter.style.display = "flex";
        elements.simulatorTransportBottom.style.display = "none";
        if (elements.simulatorCenterRewind) elements.simulatorCenterRewind.style.display = "inline-flex";
        if (elements.simulatorCenterFF) elements.simulatorCenterFF.style.display = "inline-flex";
      }
    }

    // Seekbar thickness
    if (elements.simulatorSeekbar) {
      let height = "5px";
      if (p.seekbarStyle === "minimal") height = "3px";
      if (p.seekbarStyle === "thick") height = "8px";
      elements.simulatorSeekbar.style.height = height;
    }

    // Top control icons visibility
    elements.simulatorTopIcons.forEach((icon) => {
      const id = icon.getAttribute("data-id");
      if (id && p.hiddenControls.has(id)) {
        icon.style.display = "none";
      } else {
        icon.style.display = "inline-flex";
      }
    });

    updateSimulatorTimelineUI();
  }

  /** Modal Dialog Management */
  function bindModalEvents() {
    document.querySelectorAll("[data-open-modal]").forEach((trigger) => {
      trigger.addEventListener("click", (e) => {
        e.preventDefault();
        const modalId = trigger.getAttribute("data-open-modal");
        if (modalId) openModal(modalId);
      });
    });

    elements.modals.forEach((modal) => {
      modal.addEventListener("click", (e) => {
        if (e.target === modal) closeModal(modal);
      });

      modal.querySelectorAll(".modal-close-btn").forEach((btn) => {
        btn.addEventListener("click", () => closeModal(modal));
      });
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        elements.modals.forEach((m) => closeModal(m));
      }
    });
  }

  function openModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    const closeBtn = modal.querySelector(".modal-close-btn");
    closeBtn?.focus();
  }

  function closeModal(modal) {
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
  }

  /** Mobile Navigation Drawer */
  function bindMobileNav() {
    if (!elements.mobileToggle || !elements.navLinks) return;

    elements.mobileToggle.addEventListener("click", (e) => {
      e.stopPropagation();
      const isExpanded = elements.mobileToggle.getAttribute("aria-expanded") === "true";
      elements.mobileToggle.setAttribute("aria-expanded", isExpanded ? "false" : "true");
      elements.navLinks.classList.toggle("mobile-open");
    });

    elements.navLinks.querySelectorAll(".nav-link").forEach((link) => {
      link.addEventListener("click", () => {
        elements.navLinks.classList.remove("mobile-open");
        elements.mobileToggle.setAttribute("aria-expanded", "false");
      });
    });

    document.addEventListener("click", (e) => {
      if (!e.target.closest(".site-header")) {
        elements.navLinks.classList.remove("mobile-open");
        elements.mobileToggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  /** Show Toast Feedback */
  window.showToast = function (message) {
    let container = document.getElementById("toast-container");
    if (!container) {
      container = document.createElement("div");
      container.id = "toast-container";
      container.className = "toast-container";
      document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = "toast-item show";
    toast.innerHTML = `<span class="toast-dot"></span><span class="toast-text">${message}</span>`;

    container.appendChild(toast);
    setTimeout(() => {
      toast.classList.remove("show");
      setTimeout(() => toast.remove(), 220);
    }, 2800);
  };

  /** Copy text helper */
  window.copyToClipboard = function (text, message) {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        window.showToast(message || "Copied to clipboard!");
      });
    }
  };

  // Run on DOM Ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
