// SONORA AI Web Player Client
const API_BASE = window.location.port === "3000" 
  ? "http://localhost:8080" 
  : window.location.origin;

// State
let currentTrack = null;
let currentQueue = [];
let queueIndex = -1;
let isShuffle = false;
let repeatMode = 0; // 0 = off, 1 = repeat all, 2 = repeat one
let favorites = JSON.parse(localStorage.getItem("sonora_favs") || "[]");

// DOM Elements
const audio = document.getElementById("audio-engine");
const searchInput = document.getElementById("search-input");
const clearSearchBtn = document.getElementById("clear-search-btn");
const searchMicBtn = document.getElementById("search-mic-btn");
const sidebarMicBtn = document.getElementById("sidebar-mic-btn");
const heroListenBtn = document.getElementById("hero-listen-btn");
const heroTrendingBtn = document.getElementById("hero-trending-btn");
const featuredCards = document.getElementById("featured-cards");
const tracksList = document.getElementById("tracks-list");
const searchResultsList = document.getElementById("search-results-list");
const favoritesList = document.getElementById("favorites-list");
const favBadge = document.getElementById("fav-badge");
const categoryTitle = document.getElementById("category-title");

// Player Elements
const playerBar = document.getElementById("player-bar");
const playerArt = document.getElementById("player-art");
const playerTitle = document.getElementById("player-title");
const playerArtist = document.getElementById("player-artist");
const playerFavBtn = document.getElementById("player-fav-btn");
const btnPlayPause = document.getElementById("btn-play-pause");
const btnPrev = document.getElementById("btn-prev");
const btnNext = document.getElementById("btn-next");
const btnShuffle = document.getElementById("btn-shuffle");
const btnRepeat = document.getElementById("btn-repeat");
const seekContainer = document.getElementById("seek-container");
const seekProgress = document.getElementById("seek-progress");
const timeCurrent = document.getElementById("time-current");
const timeTotal = document.getElementById("time-total");
const volumeSlider = document.getElementById("volume-slider");

// Voice Modal Elements
const voiceModal = document.getElementById("voice-modal");
const voiceCloseBtn = document.getElementById("voice-close-btn");
const voiceStatus = document.getElementById("voice-status-heading");
const voiceTranscript = document.getElementById("voice-transcript");
const voiceActionChip = document.getElementById("voice-action-chip");
const voiceActionText = document.getElementById("voice-action-text");
const voiceRetryBtn = document.getElementById("voice-retry-btn");
const voiceDoneBtn = document.getElementById("voice-done-btn");

let recognition = null;
let recognizedCommand = null;

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  updateFavoritesBadge();
  loadTrending("Tamil Hits");
  setupNavigation();
  setupAudioListeners();
  setupSearch();
  setupVoice();
});

// Category & Trending Fetch
async function loadTrending(category) {
  categoryTitle.innerText = category;
  tracksList.innerHTML = `<div style="padding:20px; color:#a69eb8;">Loading ${category} catalog...</div>`;
  featuredCards.innerHTML = `<div style="padding:20px; color:#a69eb8;">Loading featured tracks...</div>`;

  try {
    const res = await fetch(`${API_BASE}/api/trending?category=${encodeURIComponent(category)}`);
    const data = await res.json();

    if (data && data.length > 0) {
      renderFeatured(data.slice(0, 4));
      renderTrackList(tracksList, data, data);
      document.getElementById("featured-count").innerText = `${data.length} tracks available`;
    } else {
      tracksList.innerHTML = `<div style="padding:20px; color:#a69eb8;">No tracks found for ${category}.</div>`;
    }
  } catch (err) {
    console.error("API error:", err);
    tracksList.innerHTML = `<div style="padding:20px; color:#ff4d6d;">Could not connect to backend API at ${API_BASE}. Ensure Docker container is running!</div>`;
  }
}

function renderFeatured(tracks) {
  featuredCards.innerHTML = "";
  tracks.forEach(track => {
    const card = document.createElement("div");
    card.className = "music-card";
    card.innerHTML = `
      <div class="card-art-box">
        <img src="${track.artworkUrl || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300'}" alt="${track.title}">
        <div class="card-play-overlay">
          <div class="play-circle-small"><i class="fa-solid fa-play"></i></div>
        </div>
      </div>
      <div class="card-title">${track.title}</div>
      <div class="card-artist">${track.artist}</div>
    `;
    card.addEventListener("click", () => playTrack(track, tracks));
    featuredCards.appendChild(card);
  });
}

function renderTrackList(container, tracks, queue) {
  container.innerHTML = "";
  tracks.forEach((track, idx) => {
    const isPlaying = currentTrack && currentTrack.id === track.id && !audio.paused;
    const isFav = favorites.some(f => f.id === track.id);

    const row = document.createElement("div");
    row.className = `track-row ${currentTrack && currentTrack.id === track.id ? 'active' : ''}`;
    row.innerHTML = `
      <div class="track-row-art">
        <img src="${track.artworkUrl || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120'}" alt="${track.title}">
      </div>
      <div class="track-row-info">
        <div class="track-row-title">${track.title}</div>
        <div class="track-row-artist">${track.artist} • ${track.album || 'Single'}</div>
      </div>
      <div class="track-row-badges">
        ${track.isPreview ? '<span class="badge-tag">PREVIEW</span>' : '<span class="badge-tag">FULL</span>'}
      </div>
      <div class="track-duration">${formatTime(track.durationMs || 30000)}</div>
      <button class="btn-fav ${isFav ? 'active' : ''}" title="Favorite">
        <i class="fa-${isFav ? 'solid' : 'regular'} fa-heart"></i>
      </button>
    `;

    row.addEventListener("click", (e) => {
      if (e.target.closest(".btn-fav")) return;
      playTrack(track, queue);
    });

    const favBtn = row.querySelector(".btn-fav");
    favBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      toggleFavorite(track);
      favBtn.classList.toggle("active");
      favBtn.innerHTML = `<i class="fa-${favorites.some(f => f.id === track.id) ? 'solid' : 'regular'} fa-heart"></i>`;
    });

    container.appendChild(row);
  });
}

// Audio Engine & Playback
function playTrack(track, queue = [track]) {
  currentTrack = track;
  currentQueue = queue;
  queueIndex = queue.findIndex(t => t.id === track.id);

  playerTitle.innerText = track.title;
  playerArtist.innerText = `${track.artist} • ${track.album}`;
  playerArt.src = track.artworkUrl || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120";
  
  audio.src = track.streamUrl;
  audio.play().then(() => {
    updatePlayPauseButton(true);
    highlightActiveRows();
  }).catch(err => {
    console.error("Playback failed:", err);
  });

  updateFavIcon();
}

function updatePlayPauseButton(isPlaying) {
  btnPlayPause.innerHTML = isPlaying ? `<i class="fa-solid fa-pause"></i>` : `<i class="fa-solid fa-play"></i>`;
}

function highlightActiveRows() {
  document.querySelectorAll(".track-row").forEach(row => {
    row.classList.remove("active");
  });
}

function setupAudioListeners() {
  btnPlayPause.addEventListener("click", () => {
    if (!currentTrack && currentQueue.length > 0) {
      playTrack(currentQueue[0], currentQueue);
      return;
    }
    if (audio.paused) {
      audio.play();
      updatePlayPauseButton(true);
    } else {
      audio.pause();
      updatePlayPauseButton(false);
    }
  });

  btnNext.addEventListener("click", skipNext);
  btnPrev.addEventListener("click", skipPrev);

  btnShuffle.addEventListener("click", () => {
    isShuffle = !isShuffle;
    btnShuffle.classList.toggle("active", isShuffle);
  });

  btnRepeat.addEventListener("click", () => {
    repeatMode = (repeatMode + 1) % 3;
    btnRepeat.classList.toggle("active", repeatMode !== 0);
  });

  audio.addEventListener("timeupdate", () => {
    if (audio.duration) {
      const progress = (audio.currentTime / audio.duration) * 100;
      seekProgress.style.width = `${progress}%`;
      timeCurrent.innerText = formatSeconds(audio.currentTime);
      timeTotal.innerText = formatSeconds(audio.duration);
    }
  });

  audio.addEventListener("ended", () => {
    if (repeatMode === 2) {
      audio.currentTime = 0;
      audio.play();
    } else {
      skipNext();
    }
  });

  seekContainer.addEventListener("click", (e) => {
    const rect = seekContainer.getBoundingClientRect();
    const pos = (e.clientX - rect.left) / rect.width;
    if (audio.duration) {
      audio.currentTime = pos * audio.duration;
    }
  });

  volumeSlider.addEventListener("input", (e) => {
    audio.volume = parseFloat(e.target.value);
  });

  playerFavBtn.addEventListener("click", () => {
    if (currentTrack) {
      toggleFavorite(currentTrack);
      updateFavIcon();
    }
  });
}

function skipNext() {
  if (currentQueue.length === 0) return;
  if (isShuffle) {
    queueIndex = Math.floor(Math.random() * currentQueue.length);
  } else {
    queueIndex = (queueIndex + 1) % currentQueue.length;
  }
  playTrack(currentQueue[queueIndex], currentQueue);
}

function skipPrev() {
  if (currentQueue.length === 0) return;
  if (audio.currentTime > 3) {
    audio.currentTime = 0;
    return;
  }
  queueIndex = queueIndex - 1 < 0 ? currentQueue.length - 1 : queueIndex - 1;
  playTrack(currentQueue[queueIndex], currentQueue);
}

// Favorites
function toggleFavorite(track) {
  const exists = favorites.some(f => f.id === track.id);
  if (exists) {
    favorites = favorites.filter(f => f.id !== track.id);
  } else {
    favorites.unshift(track);
  }
  localStorage.setItem("sonora_favs", JSON.stringify(favorites));
  updateFavoritesBadge();
}

function updateFavoritesBadge() {
  favBadge.innerText = favorites.length;
}

function updateFavIcon() {
  if (!currentTrack) return;
  const isFav = favorites.some(f => f.id === currentTrack.id);
  playerFavBtn.innerHTML = `<i class="fa-${isFav ? 'solid' : 'regular'} fa-heart"></i>`;
  playerFavBtn.style.color = isFav ? "var(--error)" : "var(--text-muted)";
}

// Search
function setupSearch() {
  searchInput.addEventListener("input", () => {
    clearSearchBtn.style.display = searchInput.value ? "block" : "none";
  });

  clearSearchBtn.addEventListener("click", () => {
    searchInput.value = "";
    clearSearchBtn.style.display = "none";
    switchTab("home");
  });

  searchInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      performSearch(searchInput.value);
    }
  });
}

async function performSearch(query) {
  const clean = query.trim();
  if (!clean) return;

  switchTab("search");
  document.getElementById("search-title").innerText = `Results for "${clean}"`;
  searchResultsList.innerHTML = `<div style="padding:20px; color:#a69eb8;">Searching catalogs...</div>`;

  try {
    const res = await fetch(`${API_BASE}/api/search?q=${encodeURIComponent(clean)}&limit=25`);
    const data = await res.json();
    if (data && data.length > 0) {
      document.getElementById("search-count").innerText = `${data.length} tracks`;
      renderTrackList(searchResultsList, data, data);
    } else {
      searchResultsList.innerHTML = `<div style="padding:20px; color:#a69eb8;">No playable tracks found for "${clean}". Try another title or artist!</div>`;
    }
  } catch (err) {
    searchResultsList.innerHTML = `<div style="padding:20px; color:#ff4d6d;">Error searching music backend.</div>`;
  }
}

// Navigation Tabs
function setupNavigation() {
  document.querySelectorAll(".nav-item").forEach(btn => {
    btn.addEventListener("click", () => {
      const tab = btn.getAttribute("data-tab");
      switchTab(tab);
    });
  });

  document.querySelectorAll(".category-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      document.querySelectorAll(".category-chip").forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      const cat = chip.getAttribute("data-cat");
      switchTab("home");
      loadTrending(cat);
    });
  });

  heroTrendingBtn.addEventListener("click", () => {
    loadTrending("Tamil Hits");
  });
}

function switchTab(tabId) {
  document.querySelectorAll(".nav-item").forEach(b => {
    b.classList.toggle("active", b.getAttribute("data-tab") === tabId);
  });
  document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));
  const activePane = document.getElementById(`tab-${tabId}`);
  if (activePane) activePane.classList.add("active");

  if (tabId === "favorites") {
    renderFavorites();
  }
}

function renderFavorites() {
  if (favorites.length === 0) {
    favoritesList.innerHTML = `<div style="padding:30px; text-align:center; color:#a69eb8;">No favorites saved yet. Tap the heart icon on any song to save!</div>`;
    document.getElementById("play-all-favs-btn").style.display = "none";
  } else {
    document.getElementById("play-all-favs-btn").style.display = "inline-flex";
    document.getElementById("play-all-favs-btn").onclick = () => playTrack(favorites[0], favorites);
    renderTrackList(favoritesList, favorites, favorites);
  }
}

// Voice Recognition & Command Handler
function setupVoice() {
  const triggerButtons = [searchMicBtn, sidebarMicBtn, heroListenBtn];
  triggerButtons.forEach(btn => {
    btn.addEventListener("click", startVoiceRecognition);
  });

  voiceCloseBtn.addEventListener("click", stopVoiceRecognition);
  voiceRetryBtn.addEventListener("click", startVoiceRecognition);
  voiceDoneBtn.addEventListener("click", executeRecognizedCommand);
}

function startVoiceRecognition() {
  voiceModal.style.display = "flex";
  voiceStatus.innerText = "Listening...";
  voiceTranscript.innerText = "Speak now: \"Play Jana Nayagan\", \"Anirudh songs\", or \"Pause\"";
  voiceActionChip.style.display = "none";
  voiceRetryBtn.style.display = "none";
  voiceDoneBtn.style.display = "none";

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    voiceStatus.innerText = "Not Supported";
    voiceTranscript.innerText = "Speech recognition is not supported in this browser. Try Chrome or Edge.";
    return;
  }

  recognition = new SpeechRecognition();
  recognition.lang = "en-IN"; // Supports Tamil/Indian English accents well
  recognition.continuous = false;
  recognition.interimResults = false;

  recognition.onresult = async (event) => {
    const text = event.results[0][0].transcript;
    voiceTranscript.innerText = `"${text}"`;
    voiceStatus.innerText = "Processing Command...";

    try {
      const res = await fetch(`${API_BASE}/api/parse-command`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text })
      });
      const cmd = await res.json();
      recognizedCommand = cmd;

      voiceActionChip.style.display = "inline-block";
      voiceActionText.innerText = cmd.action + (cmd.query ? `: "${cmd.query}"` : "");
      voiceDoneBtn.style.display = "inline-flex";

      // Auto-execute after 1s
      setTimeout(() => {
        executeRecognizedCommand();
      }, 1000);
    } catch (err) {
      voiceStatus.innerText = "Error parsing command";
    }
  };

  recognition.onerror = (e) => {
    voiceStatus.innerText = "Couldn't catch that";
    voiceTranscript.innerText = "No speech detected or mic access denied.";
    voiceRetryBtn.style.display = "inline-flex";
  };

  recognition.start();
}

function stopVoiceRecognition() {
  if (recognition) {
    recognition.stop();
  }
  voiceModal.style.display = "none";
}

function executeRecognizedCommand() {
  stopVoiceRecognition();
  if (!recognizedCommand) return;

  const { action, query } = recognizedCommand;
  if (action === "PLAY" && query) {
    searchInput.value = query;
    performSearch(query).then(() => {
      // Auto play first result
      const firstRow = searchResultsList.querySelector(".track-row");
      if (firstRow) firstRow.click();
    });
  } else if (action === "PAUSE") {
    audio.pause();
    updatePlayPauseButton(false);
  } else if (action === "RESUME") {
    audio.play();
    updatePlayPauseButton(true);
  } else if (action === "NEXT") {
    skipNext();
  } else if (action === "PREVIOUS") {
    skipPrev();
  } else if (action === "FAVORITE") {
    if (currentTrack) {
      toggleFavorite(currentTrack);
      updateFavIcon();
    }
  } else if (action === "SEARCH" && query) {
    searchInput.value = query;
    performSearch(query);
  }
}

// Helpers
function formatTime(ms) {
  const totalSeconds = Math.floor(ms / 1000);
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

function formatSeconds(sec) {
  const totalSeconds = Math.floor(sec);
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}
