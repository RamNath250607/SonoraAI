package com.sonora.ai.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.ai.data.local.StorageManager
import com.sonora.ai.data.model.Track
import com.sonora.ai.data.repository.MusicRepository
import com.sonora.ai.player.MusicPlayerController
import com.sonora.ai.player.PlayerState
import com.sonora.ai.voice.SonoraCommand
import com.sonora.ai.voice.VoiceRecognitionManager
import com.sonora.ai.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SonoraScreen {
    HOME,
    SEARCH,
    FAVORITES,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val playerController = MusicPlayerController.getInstance(application)
    val storageManager = StorageManager(application)
    val voiceManager = VoiceRecognitionManager(application)
    private val repository = MusicRepository()

    // Navigation and Modals
    private val _currentScreen = MutableStateFlow(SonoraScreen.HOME)
    val currentScreen: StateFlow<SonoraScreen> = _currentScreen.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _isVoiceModalOpen = MutableStateFlow(false)
    val isVoiceModalOpen: StateFlow<Boolean> = _isVoiceModalOpen.asStateFlow()

    // Home Screen Data
    private val _selectedCategory = MutableStateFlow("Tamil Hits")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _featuredTracks = MutableStateFlow<List<Track>>(emptyList())
    val featuredTracks: StateFlow<List<Track>> = _featuredTracks.asStateFlow()

    private val _recommendedTracks = MutableStateFlow<List<Track>>(emptyList())
    val recommendedTracks: StateFlow<List<Track>> = _recommendedTracks.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    // Search Screen Data
    val searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchErrorMessage = MutableStateFlow<String?>(null)
    val searchErrorMessage: StateFlow<String?> = _searchErrorMessage.asStateFlow()

    // Flows from dependencies
    val playerState: StateFlow<PlayerState> = playerController.playerState
    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val favorites: StateFlow<List<Track>> = storageManager.favoritesFlow
    val searchHistory: StateFlow<List<String>> = storageManager.historyFlow

    init {
        loadHomeContent()
    }

    fun setScreen(screen: SonoraScreen) {
        _currentScreen.value = screen
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun openVoiceSearch() {
        _isVoiceModalOpen.value = true
        voiceManager.startListening(storageManager.voiceLanguage)
    }

    fun closeVoiceSearch() {
        voiceManager.stopListening()
        voiceManager.resetState()
        _isVoiceModalOpen.value = false
    }

    fun retryVoiceSearch() {
        voiceManager.startListening(storageManager.voiceLanguage)
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            val featured = repository.getCuratedSection("Tamil Hits")
            _featuredTracks.value = featured
            _recommendedTracks.value = featured
            _isHomeLoading.value = false
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _isHomeLoading.value = true
            val tracks = repository.getCuratedSection(category)
            _recommendedTracks.value = tracks
            _isHomeLoading.value = false
        }
    }

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        searchQuery.value = trimmed
        storageManager.addSearchQuery(trimmed)

        viewModelScope.launch {
            _isSearching.value = true
            _searchErrorMessage.value = null
            val result = repository.search(trimmed)
            if (result.isSuccess) {
                val list = result.getOrNull().orEmpty()
                _searchResults.value = list
                if (list.isEmpty()) {
                    _searchErrorMessage.value = "No playable songs found for \"$trimmed\". Try another song or artist."
                }
            } else {
                _searchErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Network error during search."
            }
            _isSearching.value = false
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playerController.playTrack(track, queue)
    }

    fun toggleFavorite(track: Track) {
        storageManager.toggleFavorite(track)
    }

    fun isTrackFavorite(trackId: String): Boolean {
        return storageManager.isFavorite(trackId)
    }

    fun handleCommand(command: SonoraCommand) {
        closeVoiceSearch()

        when (command) {
            is SonoraCommand.Play -> {
                storageManager.addSearchQuery(command.query)
                viewModelScope.launch {
                    _isSearching.value = true
                    _searchErrorMessage.value = null
                    val result = repository.search(command.query)
                    _isSearching.value = false

                    if (result.isSuccess) {
                        val tracks = result.getOrNull().orEmpty()
                        if (tracks.isNotEmpty()) {
                            _searchResults.value = tracks
                            playTrack(tracks.first(), tracks)
                            _isNowPlayingExpanded.value = true
                        } else {
                            _searchErrorMessage.value = "Could not find \"${command.query}\"."
                            _currentScreen.value = SonoraScreen.SEARCH
                        }
                    } else {
                        _searchErrorMessage.value = "Failed to search for \"${command.query}\"."
                        _currentScreen.value = SonoraScreen.SEARCH
                    }
                }
            }
            is SonoraCommand.Pause -> {
                playerController.pause()
            }
            is SonoraCommand.Resume -> {
                playerController.resume()
            }
            is SonoraCommand.Next -> {
                playerController.skipNext()
            }
            is SonoraCommand.Previous -> {
                playerController.skipPrevious()
            }
            is SonoraCommand.ToggleShuffle -> {
                playerController.toggleShuffle()
            }
            is SonoraCommand.ToggleRepeat -> {
                playerController.toggleRepeat()
            }
            is SonoraCommand.FavoriteCurrent -> {
                playerState.value.currentTrack?.let { toggleFavorite(it) }
            }
            is SonoraCommand.SearchOnly -> {
                _currentScreen.value = SonoraScreen.SEARCH
                search(command.query)
            }
            is SonoraCommand.ShowHistory -> {
                _currentScreen.value = SonoraScreen.SEARCH
            }
            is SonoraCommand.ShowFavorites -> {
                _currentScreen.value = SonoraScreen.FAVORITES
            }
            is SonoraCommand.Unknown -> {
                _currentScreen.value = SonoraScreen.SEARCH
                search(command.rawText)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.stopListening()
    }
}
