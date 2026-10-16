# SONORA AI 🎵⚡

> **Smart Voice & Text Music Player for Android**
> Built with Kotlin, Jetpack Compose, Material 3, and AndroidX Media3 ExoPlayer.

![Android](https://img.shields.io/badge/Android-Native-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-purple.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)
![Media3](https://img.shields.io/badge/AndroidX-Media3%20ExoPlayer-orange.svg)

---

## 🌟 Features

- 🎙️ **Voice Commands with Tanglish & Tamil NLP**:
  - *"Play Jana Nayagan Thalapathy song"*
  - *"Play latest Anirudh songs"*
  - *"Arabic Kuthu paatu podu"* (Tamil)
  - *"Leo song play pannu"* (Tanglish)
  - *"Pause"* / *"Resume"* / *"Next song"* / *"Add to favorites"*
- 🔍 **Real-Time Music Discovery**:
  - Searches official Apple Music / iTunes public catalogs and open audio repositories.
  - Automatically fetches high-resolution 600×600 album artwork and metadata.
  - In-app audio playback via Media3 ExoPlayer with direct AAC 256kbps audio streaming.
- 🎛️ **Full-Featured Modern Player**:
  - Full-screen player with album art, neon cyan glowing controls, seek bar, queue sheet, shuffle, and repeat modes.
  - Docked mini-player with progress indicator on all screens.
  - Background playback & lock-screen media controls via `MediaSessionService`.
  - Automatic audio focus management and headphone disconnect pause (`handleAudioBecomingNoisy`).
- 💜 **Cyber-Violet Aesthetic**:
  - Dark obsidian background (`#0A0713`) with electric purple (`#9D4EDD`) and neon cyan (`#00F5D4`) accents.
- 💾 **Local Persistence**:
  - Saved Favorites and Search History.

---

## 🏗️ Architecture

- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **Audio Service**: `androidx.media3.session.MediaSessionService` + `ExoPlayer`
- **Speech Engine**: `android.speech.SpeechRecognizer`
- **Networking**: `OkHttp` + `Gson`
- **Image Loading**: `Coil Compose`

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or Android CLI
- JDK 17+
- Android SDK 34+ (compileSdk 36)

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```
