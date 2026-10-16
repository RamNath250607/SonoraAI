package com.sonora.ai.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonora.ai.theme.SonoraBackground
import com.sonora.ai.theme.SonoraCard
import com.sonora.ai.theme.SonoraCyan
import com.sonora.ai.theme.SonoraPurple
import com.sonora.ai.theme.SonoraTextPrimary
import com.sonora.ai.theme.SonoraTextSecondary
import com.sonora.ai.ui.components.MiniPlayerBar
import com.sonora.ai.ui.components.VoiceListeningModal
import com.sonora.ai.ui.screens.FavoritesScreen
import com.sonora.ai.ui.screens.HomeScreen
import com.sonora.ai.ui.screens.NowPlayingScreen
import com.sonora.ai.ui.screens.SearchScreen
import com.sonora.ai.ui.screens.SettingsScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentScreen by viewModel.currentScreen.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isVoiceModalOpen by viewModel.isVoiceModalOpen.collectAsState()

    val playerState by viewModel.playerState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    val featuredTracks by viewModel.featuredTracks.collectAsState()
    val recommendedTracks by viewModel.recommendedTracks.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchErrorMessage by viewModel.searchErrorMessage.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    // Permission launcher for Voice input
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openVoiceSearch()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice music commands.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val triggerVoiceSearch: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.openVoiceSearch()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Intercept back button if Now Playing is expanded
    BackHandler(enabled = isNowPlayingExpanded) {
        viewModel.setNowPlayingExpanded(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main Screen View + Bottom Navigation
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    SonoraScreen.HOME -> {
                        HomeScreen(
                            featuredTracks = featuredTracks,
                            recommendedTracks = recommendedTracks,
                            selectedCategory = selectedCategory,
                            isLoading = isHomeLoading,
                            playerState = playerState,
                            isFavorite = { viewModel.isTrackFavorite(it) },
                            onCategorySelect = { viewModel.selectCategory(it) },
                            onSearchClick = { viewModel.setScreen(SonoraScreen.SEARCH) },
                            onMicClick = triggerVoiceSearch,
                            onTrackPlay = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                    SonoraScreen.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            searchHistory = searchHistory,
                            isLoading = isSearching,
                            errorMessage = searchErrorMessage,
                            playerState = playerState,
                            isFavorite = { viewModel.isTrackFavorite(it) },
                            onQueryChange = { viewModel.searchQuery.value = it },
                            onSearchSubmit = { viewModel.search(it) },
                            onClearQuery = { viewModel.searchQuery.value = "" },
                            onDeleteHistoryItem = { viewModel.storageManager.removeSearchQuery(it) },
                            onBack = { viewModel.setScreen(SonoraScreen.HOME) },
                            onMicClick = triggerVoiceSearch,
                            onTrackPlay = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                    SonoraScreen.FAVORITES -> {
                        FavoritesScreen(
                            favorites = favorites,
                            playerState = playerState,
                            onTrackPlay = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                    SonoraScreen.SETTINGS -> {
                        SettingsScreen(
                            voiceLanguage = viewModel.storageManager.voiceLanguage,
                            autoPlayNext = viewModel.storageManager.autoPlayNext,
                            onVoiceLanguageChange = { viewModel.storageManager.voiceLanguage = it },
                            onAutoPlayNextChange = { viewModel.storageManager.autoPlayNext = it }
                        )
                    }
                }
            }

            // Docked Mini-Player Bar
            if (playerState.currentTrack != null && !isNowPlayingExpanded) {
                MiniPlayerBar(
                    playerState = playerState,
                    onBarClick = { viewModel.setNowPlayingExpanded(true) },
                    onPlayPauseClick = { viewModel.playerController.togglePlayPause() },
                    onNextClick = { viewModel.playerController.skipNext() }
                )
            }

            // Bottom Navigation Bar
            NavigationBar(
                containerColor = SonoraCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == SonoraScreen.HOME,
                    onClick = { viewModel.setScreen(SonoraScreen.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SonoraCyan,
                        selectedTextColor = SonoraCyan,
                        indicatorColor = SonoraPurple.copy(alpha = 0.3f),
                        unselectedIconColor = SonoraTextSecondary,
                        unselectedTextColor = SonoraTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == SonoraScreen.SEARCH,
                    onClick = { viewModel.setScreen(SonoraScreen.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SonoraCyan,
                        selectedTextColor = SonoraCyan,
                        indicatorColor = SonoraPurple.copy(alpha = 0.3f),
                        unselectedIconColor = SonoraTextSecondary,
                        unselectedTextColor = SonoraTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == SonoraScreen.FAVORITES,
                    onClick = { viewModel.setScreen(SonoraScreen.FAVORITES) },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SonoraCyan,
                        selectedTextColor = SonoraCyan,
                        indicatorColor = SonoraPurple.copy(alpha = 0.3f),
                        unselectedIconColor = SonoraTextSecondary,
                        unselectedTextColor = SonoraTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == SonoraScreen.SETTINGS,
                    onClick = { viewModel.setScreen(SonoraScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SonoraCyan,
                        selectedTextColor = SonoraCyan,
                        indicatorColor = SonoraPurple.copy(alpha = 0.3f),
                        unselectedIconColor = SonoraTextSecondary,
                        unselectedTextColor = SonoraTextSecondary
                    )
                )
            }
        }

        // Fullscreen Animated Now Playing Screen Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && playerState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val current = playerState.currentTrack
            if (current != null) {
                NowPlayingScreen(
                    playerState = playerState,
                    isFavorite = viewModel.isTrackFavorite(current.id),
                    onCollapse = { viewModel.setNowPlayingExpanded(false) },
                    onPlayPause = { viewModel.playerController.togglePlayPause() },
                    onSeek = { viewModel.playerController.seekTo(it) },
                    onNext = { viewModel.playerController.skipNext() },
                    onPrevious = { viewModel.playerController.skipPrevious() },
                    onToggleShuffle = { viewModel.playerController.toggleShuffle() },
                    onToggleRepeat = { viewModel.playerController.toggleRepeat() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onTrackSelect = { track -> viewModel.playTrack(track, playerState.queue) }
                )
            }
        }

        // Animated Voice Recognition Modal Overlay
        if (isVoiceModalOpen) {
            VoiceListeningModal(
                voiceState = voiceState,
                onDismiss = { viewModel.closeVoiceSearch() },
                onRetry = { viewModel.retryVoiceSearch() },
                onCommandDetected = { command -> viewModel.handleCommand(command) }
            )
        }
    }
}
