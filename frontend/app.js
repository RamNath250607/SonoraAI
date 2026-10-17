// Ram Beats Multi-Device Web Client (iPhone, Android, Laptop)
const API_BASE = window.location.port === "3000" 
  ? `http://${window.location.hostname}:8080` 
  : window.location.origin;

// State
let currentTrack = null;
let currentQueue = [];
let queueIndex = -1;
let isShuffle = false;
let repeatMode = 0; // 0 = off, 1 = repeat all, 2 = repeat one
let favorites = JSON.parse(localStorage.getItem("rambeats_favs") || localStorage.getItem("sonora_favs") || "[]");
let radioStations = [];
let hostedSongs = [];

// DOM Elements
const audio = document.getElementById("audio-engine");
const searchInput = document.getElementById("search-input");
const clearSearchBtn = document.getElementById("clear-search-btn");
const searchMicBtn = document.getElementById("search-mic-btn");
const sidebarMicBtn = document.getElementById("sidebar-mic-btn");
const heroListenBtn = document.getElementById("hero-listen-btn");
const heroRadioBtn = document.getElementById("hero-radio-btn");
const featuredCards = document.getElementById("featured-cards");
const tracksList = document.getElementById("tracks-list");
const searchResultsList = document.getElementById("search-results-list");
const favoritesList = document.getElementById("favorites-list");
const radioGrid = document.getElementById("radio-grid");
const hostSongsList = document.getElementById("host-songs-list");
const favBadge = document.getElementById("fav-badge");
const categoryTitle = document.getElementById("category-title");

// Player Elements
const playerBar = document.getElementById("player-bar");
const playerArt = document.getElementById("player-art");
const playerTitle = document.getElementById("player-title");
const playerArtist = document.getElementById("player-artist");
const playerFavBtn = document.getElementById("player-fav-btn");
const playerBadge = document.getElementById("player-badge");
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

// Modals
const voiceModal = document.getElementById("voice-modal");
const voiceCloseBtn = document.getElementById("voice-close-btn");
const voiceStatus = document.getElementById("voice-status-heading");
const voiceTranscript = document.getElementById("voice-transcript");
const voiceActionChip = document.getElementById("voice-action-chip");
const voiceActionText = document.getElementById("voice-action-text");
const voiceRetryBtn = document.getElementById("voice-retry-btn");
const voiceDoneBtn = document.getElementById("voice-done-btn");

const uploadModal = document.getElementById("upload-modal");
const uploadCloseBtn = document.getElementById("upload-close-btn");
const btnOpenUpload = document.getElementById("btn-open-upload");
const btnUploadTriggerTab = document.getElementById("btn-upload-trigger-tab");
const uploadForm = document.getElementById("upload-form");
const uploadSubmitBtn = document.getElementById("upload-submit-btn");

let recognition = null;
let recognizedCommand = null;

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("server-url-display").innerText = API_BASE;
  const topbarApkBtn = document.getElementById("topbar-download-apk-btn");
  const sidebarApkBtn = document.getElementById("sidebar-download-apk-btn");
  const mobileApkBtn = document.getElementById("mobile-download-apk-btn");
  if (topbarApkBtn) topbarApkBtn.href = `${API_BASE}/api/download-apk`;
  if (sidebarApkBtn) sidebarApkBtn.href = `${API_BASE}/api/download-apk`;
  if (mobileApkBtn) mobileApkBtn.href = `${API_BASE}/api/download-apk`;
  updateFavoritesBadge();
  loadTrending("Tamil Hits");
  loadRadioStations("all");
  loadHostedSongs();
  setupNavigation();
  setupAudioListeners();
  setupSearch();
  setupVoice();
  setupUpload();
  setupMediaSession();
});

// Category & Trending
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
    tracksList.innerHTML = `<div style="padding:20px; color:#ff4d6d;">Could not connect to host server at ${API_BASE}.</div>`;
  }
}

// Live Radio Stations
async function loadRadioStations(tag = "all") {
  radioGrid.innerHTML = `<div style="padding:20px; color:#a69eb8;">Tuning into live radio streams...</div>`;
  try {
    const res = await fetch(`${API_BASE}/api/radio/stations?tag=${encodeURIComponent(tag)}`);
    radioStations = await res.json();
    renderRadioGrid(radioStations);
  } catch (err) {
    radioGrid.innerHTML = `<div style="padding:20px; color:#ff4d6d;">Failed to load live radio streams.</div>`;
  }
}

function renderRadioGrid(stations) {
  radioGrid.innerHTML = "";
  stations.forEach(st => {
    const card = document.createElement("div");
    card.className = "music-card radio-card";
    card.innerHTML = `
      <span class="radio-live-tag">🔴 LIVE</span>
      <div class="card-art-box">
        <img src="${st.favicon || 'logo.png'}" alt="${st.name}">
        <div class="card-play-overlay">
          <div class="play-circle-small"><i class="fa-solid fa-play"></i></div>
        </div>
      </div>
      <div class="card-title">${st.name}</div>
      <div class="card-artist">${st.tags || 'Live Broadcast'}</div>
    `;

    card.addEventListener("click", () => {
      const radioTrack = {
        id: st.id,
        title: st.name,
        artist: "Live Radio Station",
        album: st.tags || "Radio Broadcast",
        artworkUrl: st.favicon || "logo.png",
        streamUrl: st.streamUrl,
        durationMs: 0,
        isLive: true,
        sourceName: "Live Radio Station"
      };
      playTrack(radioTrack, [radioTrack]);
    });

    radioGrid.appendChild(card);
  });
}

// Host Server Vault (Full Songs)
async function loadHostedSongs() {
  hostSongsList.innerHTML = `<div style="padding:20px; color:#a69eb8;">Loading full-length songs from host server vault...</div>`;
  try {
    const res = await fetch(`${API_BASE}/api/hosted-songs`);
    hostedSongs = await res.json();
    if (hostedSongs.length > 0) {
      renderTrackList(hostSongsList, hostedSongs, hostedSongs);
    } else {
      hostSongsList.innerHTML = `<div style="padding:30px; text-align:center; color:#a69eb8;">No songs uploaded yet. Click <strong>"Add Song to Host"</strong> above to host your full MP3 tracks!</div>`;
    }
  } catch (err) {
    hostSongsList.innerHTML = `<div style="padding:20px; color:#ff4d6d;">Host server library unavailable.</div>`;
  }
}

function renderFeatured(tracks) {
  featuredCards.innerHTML = "";
  tracks.forEach(track => {
    const card = document.createElement("div");
    card.className = "music-card";
    card.innerHTML = `
      <div class="card-art-box">
        <img src="${track.artworkUrl || 'logo.png'}" alt="${track.title}">
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
  tracks.forEach((track) => {
    const isPlaying = currentTrack && currentTrack.id === track.id && !audio.paused;
    const isFav = favorites.some(f => f.id === track.id);

    const row = document.createElement("div");
    row.className = `track-row ${currentTrack && currentTrack.id === track.id ? 'active' : ''}`;
    
    let badgeHtml = '<span class="badge-tag">PREVIEW</span>';
    if (track.isLive) {
      badgeHtml = '<span class="badge-tag live">🔴 LIVE</span>';
    } else if (!track.isPreview) {
      badgeHtml = '<span class="badge-tag full">FULL SONG</span>';
    }

    row.innerHTML = `
      <div class="track-row-art">
        <img src="${track.artworkUrl || 'logo.png'}" alt="${track.title}">
      </div>
      <div class="track-row-info">
        <div class="track-row-title">${track.title}</div>
        <div class="track-row-artist">${track.artist} • ${track.album || 'Single'}</div>
      </div>
      <div class="track-row-badges">
        ${badgeHtml}
      </div>
      <div class="track-duration">${track.isLive ? 'LIVE' : formatTime(track.durationMs || 30000)}</div>
      ${track.isLive ? '' : `
      <button class="btn-track-download" title="Download Song">
        <i class="fa-solid fa-download"></i>
      </button>`}
      <button class="btn-fav ${isFav ? 'active' : ''}" title="Favorite">
        <i class="fa-${isFav ? 'solid' : 'regular'} fa-heart"></i>
      </button>
    `;

    row.addEventListener("click", (e) => {
      if (e.target.closest(".btn-fav") || e.target.closest(".btn-track-download")) return;
      playTrack(track, queue);
    });

    const downloadBtn = row.querySelector(".btn-track-download");
    if (downloadBtn) {
      downloadBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        downloadTrack(track);
      });
    }

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
  playerArtist.innerText = `${track.artist} • ${track.album || ''}`;
  playerArt.src = track.artworkUrl || "logo.png";
  
  if (track.isLive) {
    playerBadge.innerText = "🔴 LIVE";
    playerBadge.className = "preview-badge live";
    timeTotal.innerText = "LIVE";
  } else if (!track.isPreview) {
    playerBadge.innerText = "FULL SONG";
    playerBadge.className = "preview-badge full";
    timeTotal.innerText = formatTime(track.durationMs);
  } else {
    playerBadge.innerText = "PREVIEW";
    playerBadge.className = "preview-badge";
    timeTotal.innerText = formatTime(track.durationMs || 30000);
  }

  audio.src = track.streamUrl;
  audio.play().then(() => {
    updatePlayPauseButton(true);
    highlightActiveRows();
    updateMediaSession();
  }).catch(err => {
    console.error("Playback error:", err);
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

// MediaSession for iPhone Lockscreen, Mac & Android Controls
function setupMediaSession() {
  if ('mediaSession' in navigator) {
    navigator.mediaSession.setActionHandler('play', () => {
      audio.play();
      updatePlayPauseButton(true);
    });
    navigator.mediaSession.setActionHandler('pause', () => {
      audio.pause();
      updatePlayPauseButton(false);
    });
    navigator.mediaSession.setActionHandler('previoustrack', skipPrev);
    navigator.mediaSession.setActionHandler('nexttrack', skipNext);
    navigator.mediaSession.setActionHandler('seekto', (details) => {
      if (details.seekTime && audio.duration) {
        audio.currentTime = details.seekTime;
      }
    });
  }
}

function updateMediaSession() {
  if ('mediaSession' in navigator && currentTrack) {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: currentTrack.title,
      artist: currentTrack.artist,
      album: currentTrack.album || "Ram Beats",
      artwork: [
        { src: currentTrack.artworkUrl || 'logo.png', sizes: '512x512', type: 'image/png' }
      ]
    });
  }
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
    if (currentTrack && currentTrack.isLive) {
      seekProgress.style.width = "100%";
      timeCurrent.innerText = "LIVE";
      return;
    }
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
    if (currentTrack && currentTrack.isLive) return;
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

  const playerDownloadBtn = document.getElementById("player-download-btn");
  if (playerDownloadBtn) {
    playerDownloadBtn.addEventListener("click", () => {
      if (currentTrack) {
        downloadTrack(currentTrack);
      }
    });
  }
}

function downloadTrack(track) {
  if (!track) return;
  if (track.isLive) {
    alert("Live radio broadcasts cannot be downloaded.");
    return;
  }
  const cleanId = track.id.replace("full_", "").replace("yt_", "");
  const safeTitle = encodeURIComponent(track.title);
  const downloadUrl = `${API_BASE}/api/download/${cleanId}?title=${safeTitle}`;
  
  const a = document.createElement("a");
  a.href = downloadUrl;
  a.download = `${track.title}.m4a`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
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
  localStorage.setItem("rambeats_favs", JSON.stringify(favorites));
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
  searchResultsList.innerHTML = `<div style="padding:20px; color:#a69eb8;">Searching catalogs & host server...</div>`;

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

// Upload Full Song to Host
function setupUpload() {
  const openModal = () => uploadModal.style.display = "flex";
  const closeModal = () => uploadModal.style.display = "none";

  btnOpenUpload.addEventListener("click", openModal);
  btnUploadTriggerTab.addEventListener("click", openModal);
  uploadCloseBtn.addEventListener("click", closeModal);

  uploadForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fileInput = document.getElementById("upload-file-input");
    const titleInput = document.getElementById("upload-title");
    const artistInput = document.getElementById("upload-artist");
    const albumInput = document.getElementById("upload-album");

    if (!fileInput.files[0]) return;

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);
    formData.append("title", titleInput.value);
    formData.append("artist", artistInput.value);
    formData.append("album", albumInput.value || "Host Vault");

    uploadSubmitBtn.disabled = true;
    uploadSubmitBtn.innerText = "Hosting Full Song...";

    try {
      const res = await fetch(`${API_BASE}/api/upload-song`, {
        method: "POST",
        body: formData
      });
      const newSong = await res.json();
      closeModal();
      uploadForm.reset();
      uploadSubmitBtn.disabled = false;
      uploadSubmitBtn.innerHTML = `<i class="fa-solid fa-upload"></i> Host & Save Full Song`;

      // Reload vault and play immediately
      await loadHostedSongs();
      switchTab("host-library");
      playTrack(newSong, [newSong]);
    } catch (err) {
      alert("Failed to upload song to host server: " + err.message);
      uploadSubmitBtn.disabled = false;
      uploadSubmitBtn.innerText = "Try Again";
    }
  });
}

// Navigation Tabs
function setupNavigation() {
  const allNavBtns = document.querySelectorAll(".nav-item, .mob-nav-item");
  allNavBtns.forEach(btn => {
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

  document.querySelectorAll(".radio-filter-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      document.querySelectorAll(".radio-filter-chip").forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      const tag = chip.getAttribute("data-tag");
      loadRadioStations(tag);
    });
  });

  heroRadioBtn.addEventListener("click", () => {
    switchTab("radio");
  });
}

function switchTab(tabId) {
  document.querySelectorAll(".nav-item, .mob-nav-item").forEach(b => {
    b.classList.toggle("active", b.getAttribute("data-tab") === tabId);
  });
  document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));
  const activePane = document.getElementById(`tab-${tabId}`);
  if (activePane) activePane.classList.add("active");

  if (tabId === "favorites") {
    renderFavorites();
  } else if (tabId === "host-library") {
    loadHostedSongs();
  } else if (tabId === "radio") {
    loadRadioStations("all");
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
  voiceTranscript.innerText = "Speak now: \"Play Jana Nayagan\", \"Play Radio\", or \"Pause\"";
  voiceActionChip.style.display = "none";
  voiceRetryBtn.style.display = "none";
  voiceDoneBtn.style.display = "none";

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    voiceStatus.innerText = "Not Supported";
    voiceTranscript.innerText = "Speech recognition is not enabled in this browser. Try Chrome, Edge, or Safari on iOS.";
    return;
  }

  recognition = new SpeechRecognition();
  recognition.lang = "en-IN";
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

      setTimeout(() => {
        executeRecognizedCommand();
      }, 1000);
    } catch (err) {
      voiceStatus.innerText = "Error parsing command";
    }
  };

  recognition.onerror = () => {
    voiceStatus.innerText = "Couldn't catch that";
    voiceTranscript.innerText = "No speech detected or mic permission denied.";
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
      const firstRow = searchResultsList.querySelector(".track-row");
      if (firstRow) firstRow.click();
    });
  } else if (action === "RADIO") {
    switchTab("radio");
    if (radioStations.length > 0) {
      const st = radioStations[0];
      playTrack({
        id: st.id,
        title: st.name,
        artist: "Live Radio Station",
        album: st.tags,
        artworkUrl: st.favicon,
        streamUrl: st.streamUrl,
        isLive: true
      });
    }
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
